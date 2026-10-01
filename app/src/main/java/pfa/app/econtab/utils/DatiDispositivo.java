package pfa.app.econtab.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.DisplayMetrics;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import pfa.app.econtab.BuildConfig;
import pfa.app.econtab.api.MercuryApiClient;
import pfa.app.econtab.api.MercuryApiService;
import pfa.app.econtab.db.DbInterno;

/**
 * Dati del dispositivo (STATO_SISTEMA_APP.md): li mostra lo Stato sistema e si mandano al server (Sonata › Terminali)
 * all'apertura dell'app e dallo Stato sistema. Le chiavi sono quelle che il server salva nei metadati del terminale;
 * marca, modello e sistema hanno anche una colonna propria.
 */
public final class DatiDispositivo {

    private DatiDispositivo() {
    }

    /** Seriale con cui il server riconosce il dispositivo (lo stesso del login). */
    public static String seriale(Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /** "Samsung" invece di "samsung". */
    public static String marca() {
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.trim();
        return m.isEmpty() ? "" : m.substring(0, 1).toUpperCase(Locale.ITALY) + m.substring(1);
    }

    public static String sistema() {
        return "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
    }

    /** Dimensione del database dell'app, con i file di appoggio (journal/wal). */
    public static long dimensioneDatabase(Context ctx) {
        File db = ctx.getDatabasePath(DbInterno.DATABASE_NAME);
        long tot = db.length();
        for (String suffisso : new String[]{"-journal", "-wal", "-shm"}) {
            tot += new File(db.getPath() + suffisso).length();
        }
        return tot;
    }

    /** [libero, totale] in byte della memoria interna (dove stanno app e database). */
    public static long[] spazio() {
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        return new long[]{fs.getAvailableBytes(), fs.getTotalBytes()};
    }

    /** [disponibile, totale] in byte della RAM. */
    public static long[] ram(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return new long[]{mi.availMem, mi.totalMem};
    }

    /** Percentuale della batteria, -1 se non nota; "in carica" a parte. */
    public static int batteria(Context ctx) {
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        return bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
    }

    public static boolean inCarica(Context ctx) {
        Intent stato = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int s = stato != null ? stato.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
        return s == BatteryManager.BATTERY_STATUS_CHARGING || s == BatteryManager.BATTERY_STATUS_FULL;
    }

    /** "Wi-Fi", "Rete mobile", "Ethernet", "Nessuna". */
    public static String rete(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities nc = cm != null ? cm.getNetworkCapabilities(cm.getActiveNetwork()) : null;
        if (nc == null) return "Nessuna";
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Rete mobile";
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
        return "Altra";
    }

    public static String schermo(Context ctx) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        return Math.max(dm.widthPixels, dm.heightPixels) + " × " + Math.min(dm.widthPixels, dm.heightPixels) + " px · "
                + dm.densityDpi + " dpi";
    }

    /** "1,2 GB", "350 MB", "12 KB". */
    public static String byte_(long b) {
        if (b >= 1L << 30) return String.format(Locale.ITALY, "%.1f GB", b / (double) (1L << 30));
        if (b >= 1L << 20) return String.format(Locale.ITALY, "%.1f MB", b / (double) (1L << 20));
        return String.format(Locale.ITALY, "%d KB", Math.max(1, b / 1024));
    }

    /** Tutti i dati da mandare al server (chiavi stabili, valori leggibili e grezzi). */
    public static Map<String, Object> raccogli(Context ctx) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("device_serial", seriale(ctx));
        d.put("appCommit", BuildConfig.GIT_COMMIT);
        String v = VersioneApp.salvata(ctx);
        if (v != null) d.put("versioneApp", v);
        d.put("marca", marca());
        d.put("modello", Build.MODEL);
        d.put("sistema", sistema());
        d.put("dispositivo", Build.DEVICE);
        d.put("prodotto", Build.PRODUCT);
        d.put("patchSicurezza", Build.VERSION.SECURITY_PATCH);
        d.put("schermo", schermo(ctx));
        d.put("lingua", Locale.getDefault().toLanguageTag());
        d.put("fusoOrario", TimeZone.getDefault().getID());
        d.put("rete", rete(ctx));
        try {
            long[] sp = spazio();
            d.put("spazioLibero", byte_(sp[0]));
            d.put("spazioTotale", byte_(sp[1]));
            long[] r = ram(ctx);
            d.put("ramDisponibile", byte_(r[0]));
            d.put("ramTotale", byte_(r[1]));
            d.put("database", byte_(dimensioneDatabase(ctx)));
            d.put("schemaDatabase", DbInterno.SCHEMA_VERSION);
            int b = batteria(ctx);
            if (b >= 0) d.put("batteria", b + "%" + (inCarica(ctx) ? " (in carica)" : ""));
        } catch (Exception ignored) {
            // dati facoltativi
        }
        return d;
    }

    /** Invio sincrono (da un thread di lavoro): stato del terminale, null se non riuscito. */
    public static MercuryApiService.TerminaleResponse invia(Context ctx) {
        try {
            retrofit2.Response<MercuryApiService.TerminaleResponse> r = MercuryApiClient.getInstance(ctx).getService()
                    .inviaTerminale(raccogli(ctx)).execute();
            return r.isSuccessful() ? r.body() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
