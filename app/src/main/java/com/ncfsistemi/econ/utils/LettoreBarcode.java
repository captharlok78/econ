package com.ncfsistemi.econ.utils;

import android.app.Activity;
import android.widget.Toast;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

/**
 * Lettura di un barcode con la fotocamera (STUDIO_BARCODE_ARTICOLI.md): usa lo scanner di Google Play Services, che
 * mostra la sua finestra e restituisce il codice letto; l'app non chiede il permesso della fotocamera. Codici dei
 * prodotti (EAN-13/8, UPC) e quelli di magazzino (Code 128/39, QR, DataMatrix). La prima volta Play Services scarica il
 * modulo dello scanner: senza rete in quel momento la lettura non parte e si avvisa.
 */
public final class LettoreBarcode {

    public interface Letto {
        void codice(String testo);
    }

    private LettoreBarcode() {
    }

    public static void leggi(Activity activity, Letto letto) {
        GmsBarcodeScannerOptions opzioni = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_ITF)
                .enableAutoZoom()
                .build();
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(activity, opzioni);
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String testo = barcode.getRawValue();
                    if (testo != null && !testo.trim().isEmpty()) {
                        letto.codice(testo.trim());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(activity,
                        "Lettura del barcode non disponibile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
