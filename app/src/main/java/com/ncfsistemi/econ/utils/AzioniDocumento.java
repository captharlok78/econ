package com.ncfsistemi.econ.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.ElementiCantiere;
import com.ncfsistemi.econ.db.table.Preventivi;

/** Azioni sul preventivo comuni all'elenco e alla scheda del documento (DocumentoActivity). */
public final class AzioniDocumento {

    private AzioniDocumento() {
    }

    public static void trasformaInOrdine(final Activity a, final ContentValues val, final Runnable dopo) {
        Utility.mostraConfermaDialog(a.getString(R.string.attenzione), a.getString(R.string.messaggio_trasformazione_preventivi),
                a, a.getString(R.string.conferma), a.getString(R.string.annulla), (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        ArrayList<Object> elementiModificatiCantiere = _getElementiModificatiCantiere(a, val.getAsInteger(Preventivi.ID_PREVENTIVO));
                        if (elementiModificatiCantiere.size() > 0) {
                            StringBuilder messaggio = new StringBuilder("I seguenti elementi del cantiere verranno sostituiti se confermi la "
                                    + "trasformazione del preventivo in ordine. Se gli elementi appartenevano ad un ordine aperto, saranno "
                                    + "tolti dall'ordine e conteggiati nel nuovo ordine derivante dal preventivo trasformato:\n");
                            for (Object o : elementiModificatiCantiere) {
                                ContentValues valMod = (ContentValues) o;
                                messaggio.append(valMod.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO)).append(" - ")
                                        .append(valMod.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT)).append("\n");
                            }
                            Utility.mostraConfermaDialog(a.getString(R.string.attenzione), messaggio.toString(), a, "Ok",
                                    a.getString(R.string.annulla), (dialogInterface, i) -> {
                                        if (i == DialogInterface.BUTTON_POSITIVE) {
                                            _eseguiTrasformazione(a, val, dopo);
                                        }
                                    });
                        } else {
                            _eseguiTrasformazione(a, val, dopo);
                        }
                    }
                });
    }

    private static void _eseguiTrasformazione(Activity a, ContentValues val, Runnable dopo) {
        // l'ordine nasce dal preventivo: serve ORDINI.CREA del pacchetto (il server lo rifiuterebbe)
        if (!com.ncfsistemi.econ.utils.FunzionalitaApp.ha(a, com.ncfsistemi.econ.utils.FunzionalitaApp.codice(
                com.ncfsistemi.econ.utils.FunzionalitaApp.ORDINI, com.ncfsistemi.econ.utils.FunzionalitaApp.CREA))) {
            com.ncfsistemi.econ.utils.Utility.mostraDialog("Non consentito",
                    "La tua licenza non consente questo inserimento per il tuo ruolo. Chiedi all'amministratore della ditta.",
                    a, "OK");
            return;
        }
        DbInterno db = new DbInterno(a);
        try {
            db.getReadableDatabase().beginTransaction();
            new Preventivi().trasformaPreventivoInOrdine(db, val.getAsInteger(Preventivi.ID_PREVENTIVO));
            db.getReadableDatabase().setTransactionSuccessful();
        } catch (Exception e) {
            Toast.makeText(a, Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
        } finally {
            db.getReadableDatabase().endTransaction();
            db.close();
            if (dopo != null) dopo.run();
        }
    }

    private static ArrayList<Object> _getElementiModificatiCantiere(Activity a, int idPreventivo) {
        DbInterno db = new DbInterno(a);
        ArrayList<Object> modificati = db.eseguiSelect(
                "Select * from elementi_cantiere where id_preventivo=" + idPreventivo
                        + " and id_elemento_cant_origine is not null and id_elemento_cant_origine<>0 order by numero_identificativo", null);
        db.close();
        return modificati;
    }

    /** Menu delle azioni sul documento (elenco): modifica dei dati, cambio di stato, conversione in ordine. */
    public static void menu(final com.ncfsistemi.econ.EconActivity a, final ContentValues val, final Runnable dopo) {
        String tipo = val.getAsString(Preventivi.TIPO);
        String stato = val.getAsString(Preventivi.STATO);
        boolean converti = Preventivi.TIPO_PREVENTIVO.equals(tipo)
                && (Preventivi.STATO_APERTO.equals(stato) || Preventivi.STATO_ACCETTATO.equals(stato));
        String[] opzioni = converti ? new String[] { a.getString(R.string.modifica), a.getString(R.string.cambia_stato), a.getString(R.string.trasforma_in_ordine) }
                : new String[] { a.getString(R.string.modifica), a.getString(R.string.cambia_stato) };
        Utility.mostraSelezioneDialog("", opzioni, a, (d, i) -> {
            if (i == 0) {
                android.content.Intent intent = new android.content.Intent(a, com.ncfsistemi.econ.PreventiviDettaglioModActivity.class);
                intent.putExtra("ID", val.getAsInteger(Preventivi.ID_PREVENTIVO));
                a.apriFinestraModifica(intent, 2);
            }
            if (i == 1) cambiaStato(a, val, dopo);
            if (i == 2) trasformaInOrdine(a, val, dopo);
        });
    }

    /** Cambio di stato (preventivo: aperto/accettato/rifiutato/chiuso; ordine: aperto/chiuso, la chiusura chiude l'ordine). */
    public static void cambiaStato(final Activity a, final ContentValues valPrev, final Runnable dopo) {
        final String tipo = valPrev.getAsString(Preventivi.TIPO);
        final ArrayList<Object> stati;
        if (Preventivi.TIPO_PREVENTIVO.equals(tipo)) {
            stati = new Preventivi().getStatiPreventivo(a);
        } else {
            if (Preventivi.STATO_CHIUSO.equals(valPrev.getAsString(Preventivi.STATO))) {
                Utility.mostraDialog(a.getString(R.string.attenzione), a.getString(R.string.messaggio_ordine_chiuso), a, "OK");
                return;
            }
            stati = new Preventivi().getStatiOrdine(a);
        }
        String[] opzioni = new String[stati.size()];
        for (int i = 0; i < stati.size(); i++) opzioni[i] = ((ContentValues) stati.get(i)).getAsString("DESC");
        Utility.mostraSelezioneDialog(a.getString(R.string.cambia_stato), opzioni, a, (d, i) -> {
            final ContentValues val = (ContentValues) stati.get(i);
            if (Preventivi.TIPO_ORDINE.equals(tipo) && Preventivi.STATO_CHIUSO.equals(val.getAsString("VAL"))) {
                Utility.mostraConfermaDialog(a.getString(R.string.attenzione), a.getString(R.string.messaggio_chiusura_ordine), a, "OK",
                        a.getString(R.string.annulla), (d2, j) -> {
                            if (j == DialogInterface.BUTTON_POSITIVE) {
                                scriviStato(a, val.getAsString("VAL"), valPrev);
                                DbInterno db = new DbInterno(a);
                                new Preventivi().chiudiOrdine(db, valPrev.getAsInteger(Preventivi.ID_PREVENTIVO));
                                db.close();
                                if (dopo != null) dopo.run();
                            }
                        });
            } else {
                scriviStato(a, val.getAsString("VAL"), valPrev);
                if (dopo != null) dopo.run();
            }
        });
    }

    private static void scriviStato(Activity a, String stato, ContentValues valPrev) {
        DbInterno db = new DbInterno(a);
        ContentValues upd = new ContentValues();
        upd.put(Preventivi.STATO, stato);
        ContentValues where = new ContentValues();
        where.put(Preventivi.ID_PREVENTIVO, valPrev.getAsInteger(Preventivi.ID_PREVENTIVO));
        new Preventivi().aggiornaRecord(db, upd, where);
        db.close();
    }
}
