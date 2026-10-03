package com.ncfsistemi.econ.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;

import com.ncfsistemi.econ.MenuActivity;
import com.ncfsistemi.econ.api.ProfiloOffline;
import com.ncfsistemi.econ.api.TokenManager;

/**
 * Ingresso nell'app senza server (ACCESSO_OFFLINE.md §2.3): con il profilo offline abilitato (ProfiloOffline) oppure,
 * come prima, con il token ancora valido. Si entra nel menu con l'avviso che i dati partono solo alla riconnessione.
 */
public final class AccessoOffline {

    /** Extra del menu: mostrare l'avviso di ingresso offline. */
    public static final String EXTRA_AVVISO = "avviso_offline";

    private AccessoOffline() {
    }

    public static boolean possibile(Context ctx) {
        TokenManager tm = TokenManager.getInstance(ctx);
        if (!tm.hasToken() || tm.getIdDitta() <= 0) return false;
        return ProfiloOffline.disponibile(ctx) || tm.isTokenValido();
    }

    /** Messaggio per chi non puo' entrare offline. */
    public static String motivo(Context ctx) {
        String m = ProfiloOffline.motivoNonDisponibile(ctx);
        return "Server non raggiungibile. " + (m != null ? m : "Serve il collegamento al server.");
    }

    /** Entra nel menu lavorando offline. */
    public static void entra(Context ctx) {
        Sessione.ripristinaDaToken(ctx);
        Sessione.setOffline(true);
        ProfiloOffline.segnaIngresso(ctx);
        Riconnessione.avvia(ctx);
        Intent menu = new Intent(ctx, MenuActivity.class);
        menu.putExtra(EXTRA_AVVISO, true);
        menu.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(menu);
    }

    /** Avviso all'ingresso offline (una volta per apertura). */
    public static void mostraAvviso(Activity a) {
        StringBuilder testo = new StringBuilder()
                .append("Il server non è raggiungibile: lavori con i dati del tablet.\n\n")
                .append("Quello che inserisci o modifichi resta sul tablet e viene inviato solo quando l'app si ricollega al server. ")
                .append("Fino ad allora non ricevi gli aggiornamenti dall'ufficio (nuovi cantieri, pianificazione, modifiche).");
        int nonInviate = DatiLocali.modificheNonInviate(a);
        if (nonInviate > 0) {
            testo.append("\n\nModifiche ancora da inviare: ").append(nonInviate).append(".");
        }
        if (ProfiloOffline.disponibile(a)) {
            int giorni = ProfiloOffline.giorniRimanenti(a);
            testo.append("\n\n").append(giorni == 0
                    ? "Oggi è l'ultimo giorno in cui puoi lavorare senza collegarti."
                    : "Puoi lavorare senza collegarti ancora per " + giorni + (giorni == 1 ? " giorno." : " giorni."));
        }
        KeyguardManager km = (KeyguardManager) a.getSystemService(Context.KEYGUARD_SERVICE);
        if (km != null && !km.isDeviceSecure()) {
            testo.append("\n\nIl tablet non ha un blocco schermo: impostane uno (PIN, sequenza o impronta), "
                    + "perché i dati della ditta sono sul dispositivo.");
        }
        new AlertDialog.Builder(a)
                .setTitle("Modalità offline")
                .setMessage(testo)
                .setPositiveButton("Ho capito", null)
                .show();
    }

    /**
     * Per le funzioni che richiedono il server: offline mostra l'avviso e ritorna true (il chiamante si ferma).
     */
    public static boolean richiedeServer(Activity a, String funzione) {
        if (!Sessione.isOffline()) return false;
        Utility.mostraDialog(funzione, "Disponibile quando il tablet è collegato al server.", a, "OK");
        return true;
    }
}
