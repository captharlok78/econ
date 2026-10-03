package com.ncfsistemi.econ.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Profilo per lavorare senza server (ACCESSO_OFFLINE.md §2.2): un account in una ditta, su questo tablet.
 *
 * Si abilita dopo un login online riuscito e un allineamento di avvio completato. Per controllare la password senza
 * server si tiene un verificatore PBKDF2 con sale (non la password). Vale per i giorni decisi dalla ditta
 * (ditte.giorni_offline_app, "offlineGiorni" nelle risposte del server) dall'ultimo contatto con il server; dopo
 * TENTATIVI_MAX password sbagliate offline serve di nuovo il server. Sta nello storage cifrato di TokenManager.
 */
public final class ProfiloOffline {

    public static final int TENTATIVI_MAX = 5;
    private static final int GIORNI_DEFAULT = 7;
    private static final int ITERAZIONI = 120_000;
    private static final long GIORNO_MS = 24L * 60 * 60 * 1000;

    private static final String K_EMAIL = "offline_email";
    private static final String K_SALE = "offline_sale";
    private static final String K_VERIFICATORE = "offline_verificatore";
    private static final String K_ACCOUNT = "offline_account";
    private static final String K_ULTIMO_ONLINE = "offline_ultimo_online";
    private static final String K_ULTIMO_VISTO = "offline_ultimo_visto";
    private static final String K_GIORNI = "offline_giorni";
    private static final String K_TENTATIVI = "offline_tentativi";
    private static final String K_BLOCCATO = "offline_bloccato";

    private ProfiloOffline() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return TokenManager.getInstance(ctx).prefs();
    }

    /** Account (utente@ditta) del token salvato, null se non noto. */
    private static String accountToken(TokenManager tm) {
        if (tm.getUserId() <= 0 || tm.getIdDitta() <= 0) return null;
        return tm.getUserId() + "@" + tm.getIdDitta();
    }

    /**
     * Login online riuscito con questa password: aggiorna il verificatore. Se l'email e' cambiata l'abilitazione
     * precedente non vale piu' (serve un nuovo allineamento completo).
     */
    public static void registraPassword(Context ctx, String email, String password) {
        if (email == null || password == null || password.isEmpty()) return;
        SharedPreferences p = prefs(ctx);
        if (!email.equalsIgnoreCase(p.getString(K_EMAIL, ""))) {
            p.edit().remove(K_ACCOUNT).remove(K_VERIFICATORE).putString(K_EMAIL, email).apply();
        }
        segnaOnline(ctx, null);
        // PBKDF2 costa qualche centinaio di millisecondi: fuori dal thread principale
        new Thread(() -> {
            byte[] sale = new byte[16];
            new SecureRandom().nextBytes(sale);
            p.edit().putString(K_EMAIL, email)
                    .putString(K_SALE, Base64.encodeToString(sale, Base64.NO_WRAP))
                    .putString(K_VERIFICATORE, Base64.encodeToString(deriva(password, sale), Base64.NO_WRAP))
                    .putInt(K_TENTATIVI, 0)
                    .apply();
        }, "profilo-offline").start();
    }

    /** Password cambiata dall'app (server raggiungibile): il verificatore segue la nuova. */
    public static void aggiornaPassword(Context ctx, String nuova) {
        String email = prefs(ctx).getString(K_EMAIL, "");
        if (!email.isEmpty()) registraPassword(ctx, email, nuova);
    }

    /** Il server ha risposto con un accesso valido: riparte il conteggio dei giorni offline. giorni null = invariato. */
    static void segnaOnline(TokenManager tm, Integer giorni) {
        long ora = System.currentTimeMillis();
        SharedPreferences.Editor ed = tm.prefs().edit()
                .putLong(K_ULTIMO_ONLINE, ora)
                .putLong(K_ULTIMO_VISTO, ora)
                .remove(K_BLOCCATO);
        if (giorni != null) ed.putInt(K_GIORNI, Math.max(0, giorni));
        ed.apply();
    }

    public static void segnaOnline(Context ctx, Integer giorni) {
        segnaOnline(TokenManager.getInstance(ctx), giorni);
    }

    /** Allineamento di avvio completato senza errori: l'account del token puo' lavorare offline. */
    public static void abilita(Context ctx) {
        TokenManager tm = TokenManager.getInstance(ctx);
        String account = accountToken(tm);
        if (account == null) return;
        prefs(ctx).edit().putString(K_ACCOUNT, account).apply();
    }

    /** Il server ha rifiutato l'accesso (dispositivo disattivato, licenza, account): niente offline fino al prossimo login. */
    public static void blocca(Context ctx) {
        prefs(ctx).edit().putBoolean(K_BLOCCATO, true).apply();
    }

    /** Logout: si dimentica tutto. */
    static void cancella(SharedPreferences.Editor ed) {
        ed.remove(K_EMAIL).remove(K_SALE).remove(K_VERIFICATORE).remove(K_ACCOUNT).remove(K_ULTIMO_ONLINE)
                .remove(K_ULTIMO_VISTO).remove(K_GIORNI).remove(K_TENTATIVI).remove(K_BLOCCATO);
    }

    public static String email(Context ctx) {
        return prefs(ctx).getString(K_EMAIL, "");
    }

    public static boolean disponibile(Context ctx) {
        return motivoNonDisponibile(ctx) == null;
    }

    /** null se si puo' entrare offline, altrimenti il motivo da mostrare. */
    public static String motivoNonDisponibile(Context ctx) {
        SharedPreferences p = prefs(ctx);
        TokenManager tm = TokenManager.getInstance(ctx);
        String account = p.getString(K_ACCOUNT, null);
        if (account == null || !account.equals(accountToken(tm)) || !p.contains(K_VERIFICATORE)) {
            return "Il primo accesso su questo tablet richiede il collegamento al server.";
        }
        if (p.getBoolean(K_BLOCCATO, false)) {
            return "Il server ha negato l'accesso: serve il collegamento per entrare.";
        }
        int giorni = p.getInt(K_GIORNI, GIORNI_DEFAULT);
        if (giorni <= 0) {
            return "La ditta non consente di lavorare offline: serve il collegamento al server.";
        }
        if (p.getInt(K_TENTATIVI, 0) >= TENTATIVI_MAX) {
            return "Troppe password sbagliate: serve il collegamento al server per entrare.";
        }
        long ora = System.currentTimeMillis();
        // orologio portato indietro: non deve allungare il periodo offline
        if (ora < p.getLong(K_ULTIMO_VISTO, 0) - GIORNO_MS) {
            return "La data del tablet non e' corretta: serve il collegamento al server.";
        }
        if (ora - p.getLong(K_ULTIMO_ONLINE, 0) > giorni * GIORNO_MS) {
            return "Sono passati piu' di " + giorni + " giorni dall'ultimo collegamento: serve il server per entrare.";
        }
        return null;
    }

    /** Giorni che restano per lavorare offline (arrotondati per difetto, minimo 0). */
    public static int giorniRimanenti(Context ctx) {
        SharedPreferences p = prefs(ctx);
        long fine = p.getLong(K_ULTIMO_ONLINE, 0) + p.getInt(K_GIORNI, GIORNI_DEFAULT) * GIORNO_MS;
        return (int) Math.max(0, (fine - System.currentTimeMillis()) / GIORNO_MS);
    }

    /** Ingresso offline: tiene nota dell'ora (per accorgersi di un orologio portato indietro). */
    public static void segnaIngresso(Context ctx) {
        SharedPreferences p = prefs(ctx);
        p.edit().putLong(K_ULTIMO_VISTO, Math.max(p.getLong(K_ULTIMO_VISTO, 0), System.currentTimeMillis())).apply();
    }

    /** Controlla la password senza server (da un thread di lavoro); una sbagliata conta un tentativo. */
    public static boolean verificaPassword(Context ctx, String email, String password) {
        SharedPreferences p = prefs(ctx);
        boolean ok = false;
        try {
            if (email != null && email.equalsIgnoreCase(p.getString(K_EMAIL, ""))) {
                byte[] sale = Base64.decode(p.getString(K_SALE, ""), Base64.NO_WRAP);
                byte[] atteso = Base64.decode(p.getString(K_VERIFICATORE, ""), Base64.NO_WRAP);
                ok = atteso.length > 0 && MessageDigest.isEqual(atteso, deriva(password, sale));
            }
        } catch (Exception e) {
            ok = false;
        }
        p.edit().putInt(K_TENTATIVI, ok ? 0 : p.getInt(K_TENTATIVI, 0) + 1).apply();
        return ok;
    }

    public static int tentativiRimasti(Context ctx) {
        return Math.max(0, TENTATIVI_MAX - prefs(ctx).getInt(K_TENTATIVI, 0));
    }

    private static byte[] deriva(String password, byte[] sale) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sale, ITERAZIONI, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 non disponibile", e);
        }
    }
}
