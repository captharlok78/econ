package com.ncfsistemi.econ.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;

import okhttp3.ResponseBody;
import retrofit2.Response;

import com.ncfsistemi.econ.api.MercuryApiClient;

/**
 * Foto dell'articolo per la scheda dell'app: non viaggia nel catalogo (sarebbero troppi dati), si scarica dal server
 * (GET api/mobile/catalogo/{id}/foto) quando si apre la scheda e resta nella cache del dispositivo per i giorni
 * successivi, così senza rete si vede l'ultima scaricata. Articoli nati sul dispositivo (id negativo): nessuna foto.
 */
public final class FotoArticolo {

    /** Una foto in cache più vecchia di così si riscarica (la foto può cambiare sul server). */
    private static final long DURATA_CACHE_MS = 7L * 24 * 60 * 60 * 1000;

    public interface Pronta {
        /** null = l'articolo non ha foto (o non si è potuta scaricare e non c'è in cache). */
        void foto(Bitmap foto);
    }

    private FotoArticolo() {
    }

    public static void carica(Context ctx, int idArticolo, Pronta pronta) {
        Handler ui = new Handler(Looper.getMainLooper());
        if (idArticolo <= 0) {
            pronta.foto(null);
            return;
        }
        Context app = ctx.getApplicationContext();
        new Thread(() -> {
            File file = file(app, idArticolo);
            Bitmap foto = null;
            boolean fresca = file.isFile() && System.currentTimeMillis() - file.lastModified() < DURATA_CACHE_MS;
            if (!fresca) {
                try {
                    Response<ResponseBody> r = MercuryApiClient.getInstance(app).getService().getFotoArticolo(idArticolo).execute();
                    if (r.isSuccessful() && r.body() != null) {
                        byte[] byte_ = r.body().bytes();
                        Bitmap letta = BitmapFactory.decodeByteArray(byte_, 0, byte_.length);
                        if (letta != null) {
                            try (FileOutputStream out = new FileOutputStream(file)) {
                                out.write(byte_);
                            }
                            foto = letta;
                        }
                    } else if (r.code() == 404) {
                        file.delete();
                    }
                } catch (Exception e) {
                    android.util.Log.w("FotoArticolo", "download foto " + idArticolo + ": " + e.getMessage());
                }
            }
            if (foto == null && file.isFile()) {
                foto = BitmapFactory.decodeFile(file.getAbsolutePath());
            }
            Bitmap finale = foto;
            ui.post(() -> pronta.foto(finale));
        }).start();
    }

    private static File file(Context ctx, int idArticolo) {
        File cartella = new File(ctx.getCacheDir(), "foto_articoli");
        //noinspection ResultOfMethodCallIgnored
        cartella.mkdirs();
        return new File(cartella, idArticolo + ".img");
    }
}
