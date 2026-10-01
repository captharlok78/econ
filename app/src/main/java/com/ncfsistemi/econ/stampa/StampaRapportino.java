package com.ncfsistemi.econ.stampa;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.util.Base64;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import android.app.AlertDialog;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ncfsistemi.econ.api.MercuryApiService;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.ModelliStampa;
import com.ncfsistemi.econ.db.table.Preventivi;
import com.ncfsistemi.econ.db.table.RapportiniDettaglio;
import com.ncfsistemi.econ.db.table.RapportiniDettaglioTipi;
import com.ncfsistemi.econ.db.table.Rapportini;
import com.ncfsistemi.econ.db.table.StatiDocumento;
import com.ncfsistemi.econ.utils.DittaLocale;
import com.ncfsistemi.econ.utils.RegoleRapportino;
import com.ncfsistemi.econ.utils.RigheRapportino;
import com.ncfsistemi.econ.utils.Sessione;
import com.ncfsistemi.econ.utils.Utility;

/**
 * Stampa del rapportino dal tablet (GESTIONE_RAPPORTINI.md §15): stesse variabili e stessi modelli del server
 * (App\Stampa\StampaRapportino), dal database del tablet e dai dati della ditta (DittaLocale). L'HTML va alla stampa di
 * Android: si sceglie una stampante o "Salva come PDF". Niente prezzi e costi.
 */
public final class StampaRapportino {

    /** WebView della stampa in corso: deve restare viva finche' la stampa non ha letto il documento. */
    private static WebView inStampa;

    private StampaRapportino() {
    }

    /** Stampa il rapportino salvato: con un solo modello subito, con piu' modelli della ditta si sceglie. */
    public static void stampa(Activity activity, int idRapportino) {
        DbInterno db = new DbInterno(activity);
        List<ContentValues> modelli = modelli(db);
        db.close();
        if (modelli.size() <= 1) {
            stampa(activity, idRapportino, modelli.isEmpty() ? null : modelli.get(0));
            return;
        }
        String[] nomi = new String[modelli.size()];
        for (int i = 0; i < nomi.length; i++) {
            nomi[i] = RigheRapportino.testo(modelli.get(i), ModelliStampa.NOME) + (i == 0 ? " (proposto)" : "");
        }
        new AlertDialog.Builder(activity).setTitle("Modello di stampa")
                .setItems(nomi, (d, i) -> stampa(activity, idRapportino, modelli.get(i)))
                .setNegativeButton("Annulla", null).show();
    }

    private static void stampa(Activity activity, int idRapportino, ContentValues modello) {
        String html;
        try {
            if (modello == null) {
                Toast.makeText(activity, "Nessun modello di stampa: sincronizza il tablet.", Toast.LENGTH_LONG).show();
                return;
            }
            DbInterno db = new DbInterno(activity);
            Map<String, Object> contesto = contesto(activity, db, idRapportino);
            db.close();
            html = MiniMustache.render(RigheRapportino.testo(modello, ModelliStampa.CONTENUTO), contesto);
        } catch (Exception e) {
            Toast.makeText(activity, "Stampa non riuscita: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        String titolo = "Rapportino " + idRapportino;
        WebView web = new WebView(activity);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager pm = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                pm.print(titolo, view.createPrintDocumentAdapter(titolo),
                        new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
            }
        });
        inStampa = web;
        web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    /**
     * Modelli attivi del rapportino nell'ordine in cui proporli, come il server (GESTIONE_RAPPORTINI.md §16): quello scelto
     * dalla ditta (ditte_modelli_stampa), poi il predefinito, poi gli altri.
     */
    public static List<ContentValues> modelli(DbInterno db) {
        String d = com.ncfsistemi.econ.db.table.DitteModelliStampa.NOME_TABELLA;
        List<ContentValues> ris = new ArrayList<>();
        for (Object o : db.eseguiSelect("select m.* from " + ModelliStampa.NOME_TABELLA + " m where m." + ModelliStampa.DOCUMENTO + "='"
                + ModelliStampa.DOC_RAPPORTINO + "' and coalesce(m." + ModelliStampa.ATTIVO + ",1)=1 order by case when exists (select 1 from " + d
                + " x where x.id_modello = m.id and x.documento = m.documento and x.id_ditta = " + Sessione.getDittaSelezionata()
                + ") then 0 else 1 end, m." + ModelliStampa.PREDEFINITO + " desc, m." + ModelliStampa.ORDINE + ", m." + ModelliStampa.NOME, null)) {
            ris.add((ContentValues) o);
        }
        return ris;
    }

    /** Variabili del modello (§15.3), come sul server. */
    static Map<String, Object> contesto(Context ctx, DbInterno db, int id) throws Exception {
        ContentValues r = db.getRecord("select * from " + Rapportini.NOME_TABELLA + " where " + Rapportini.ID + "=" + id);
        if (r == null) throw new IllegalStateException("rapportino non trovato");
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("ditta", ditta(ctx));

        Map<String, Object> rap = new LinkedHashMap<>();
        rap.put("numero", Rapportini.numeroDocumento(r)); // vuoto finche' il server non l'ha dato: "(numero da assegnare)" 
        Long data = r.getAsLong(Rapportini.DATA_RAPPORTINO);
        rap.put("data", data != null ? Utility.numberToData(data) : "");
        ContentValues stato = RegoleRapportino.stato(db, RigheRapportino.intero(r, Rapportini.ID_STATO));
        rap.put("stato", stato != null ? RigheRapportino.testo(stato, StatiDocumento.NOME) : "");
        rap.put("operatore", RegoleRapportino.nomeOperatore(db, RigheRapportino.intero(r, Rapportini.ID_UTENTE_DITTA)));
        rap.put("note", RigheRapportino.testo(r, Rapportini.NOTE));
        String firma = RigheRapportino.testo(r, Rapportini.FIRMA);
        rap.put("firma", firma.isEmpty() ? "" : "data:image/png;base64," + firma);
        rap.put("firma_nome", RigheRapportino.testo(r, Rapportini.FIRMA_NOME));
        rap.put("firma_data", dataOra(r.getAsLong(Rapportini.FIRMA_DATA)));
        rap.put("firmato", !firma.isEmpty());
        c.put("rapportino", rap);

        // chi ha fatto il rapportino: nome e recapiti (operatori scaricati sul tablet)
        Map<String, Object> op = new LinkedHashMap<>();
        ContentValues ut = db.getRecord("select * from " + com.ncfsistemi.econ.db.table.Utenti.NOME_TABELLA + " where "
                + com.ncfsistemi.econ.db.table.Utenti.ID_UTENTE_DITTA + "=" + RigheRapportino.intero(r, Rapportini.ID_UTENTE_DITTA));
        op.put("nome", ut != null ? RegoleRapportino.nome(ut) : RegoleRapportino.nomeOperatore(db, RigheRapportino.intero(r, Rapportini.ID_UTENTE_DITTA)));
        op.put("email", ut != null ? RigheRapportino.testo(ut, com.ncfsistemi.econ.db.table.Utenti.E_MAIL) : "");
        op.put("telefono", ut != null ? RigheRapportino.testo(ut, com.ncfsistemi.econ.db.table.Utenti.TELEFONO) : "");
        c.put("operatore", op);

        ContentValues cl = db.getRecord("select * from " + Anagrafica.NOME_TABELLA + " where " + Anagrafica.ID_ANAGRAFICA + "="
                + RigheRapportino.intero(r, Rapportini.ID_CLIENTE));
        if (cl != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ragione_sociale", RigheRapportino.testo(cl, Anagrafica.RAGIONE_SOCIALE));
            m.put("indirizzo", indirizzo(RigheRapportino.testo(cl, Anagrafica.INDIRIZZO), "", RigheRapportino.testo(cl, Anagrafica.CAP),
                    RigheRapportino.testo(cl, Anagrafica.CITTA), RigheRapportino.testo(cl, Anagrafica.PROVINCIA)));
            m.put("partita_iva", RigheRapportino.testo(cl, Anagrafica.PARTITA_IVA));
            m.put("codice_fiscale", RigheRapportino.testo(cl, Anagrafica.CODICE_FISCALE));
            c.put("cliente", m);
        }
        ContentValues ca = db.getRecord("select * from " + Cantieri.NOME_TABELLA + " where " + Cantieri.ID_CANTIERE + "="
                + RigheRapportino.intero(r, Rapportini.ID_CANTIERE));
        if (ca != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nome", RigheRapportino.testo(ca, Cantieri.NOME));
            m.put("indirizzo", indirizzo(RigheRapportino.testo(ca, Cantieri.INDIRIZZO), RigheRapportino.testo(ca, Cantieri.CIVICO),
                    RigheRapportino.testo(ca, Cantieri.CAP), RigheRapportino.testo(ca, Cantieri.CITTA), RigheRapportino.testo(ca, Cantieri.PROVINCIA)));
            c.put("cantiere", m);
        }
        ContentValues or = db.getRecord("select * from " + Preventivi.NOME_TABELLA + " where " + Preventivi.ID_PREVENTIVO + "="
                + RigheRapportino.intero(r, Rapportini.ID_ORDINE));
        if (or != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            String anno = RigheRapportino.testo(or, Preventivi.ANNO);
            m.put("numero", RigheRapportino.testo(or, Preventivi.NUMERO) + (anno.isEmpty() || "0".equals(anno) ? "" : "/" + anno));
            m.put("titolo", RigheRapportino.testo(or, Preventivi.TITOLO));
            c.put("ordine", m);
        }

        List<Object> manodopera = new ArrayList<>(), viaggi = new ArrayList<>(), materiali = new ArrayList<>();
        double minutiLavoro = 0, minutiViaggio = 0;
        for (ContentValues d : RigheRapportino.righe(db, id)) {
            String categoria = RigheRapportino.testo(d, RigheRapportino.CATEGORIA);
            double q = RigheRapportino.decimale(d, RapportiniDettaglio.QUANTITA);
            double minuti = q * RigheRapportino.intero(d, RigheRapportino.MINUTI_PER_UNITA);
            boolean materiale = RapportiniDettaglioTipi.CATEGORIA_MATERIALE.equals(categoria);
            Map<String, Object> riga = new LinkedHashMap<>();
            riga.put("tipo", RigheRapportino.testo(d, RigheRapportino.CODICE_TIPO));
            riga.put("descrizione", materiale ? RigheRapportino.testo(d, RigheRapportino.DESCRIZIONE_ARTICOLO) : RigheRapportino.testo(d, RigheRapportino.DESCRIZIONE_TIPO));
            riga.put("codice", RigheRapportino.testo(d, RigheRapportino.CODICE_ARTICOLO));
            riga.put("utente", RigheRapportino.testo(d, RigheRapportino.NOME_UTENTE));
            riga.put("quantita", numero(q));
            // descrizione dell'unita' di misura (es. "Ore", "Minuti"), come il server; il codice resta in um_codice
            String descrUm = RigheRapportino.testo(d, RigheRapportino.DESCRIZIONE_UNITA);
            riga.put("um", descrUm.isEmpty() ? RigheRapportino.testo(d, RigheRapportino.CODICE_UNITA) : descrUm);
            riga.put("um_codice", RigheRapportino.testo(d, RigheRapportino.CODICE_UNITA));
            riga.put("ore", materiale ? "" : oreMinuti(minuti));
            String inizio = RigheRapportino.testo(d, RapportiniDettaglio.ORA_INIZIO), fine = RigheRapportino.testo(d, RapportiniDettaglio.ORA_FINE);
            riga.put("orario", inizio.isEmpty() && fine.isEmpty() ? "" : (inizio.isEmpty() ? "?" : inizio) + "–" + (fine.isEmpty() ? "?" : fine));
            riga.put("note", RigheRapportino.testo(d, RapportiniDettaglio.NOTE));
            if (materiale) {
                materiali.add(riga);
            } else if (RapportiniDettaglioTipi.CATEGORIA_VIAGGIO.equals(categoria)) {
                viaggi.add(riga);
                minutiViaggio += minuti;
            } else {
                manodopera.add(riga);
                minutiLavoro += minuti;
            }
        }
        c.put("manodopera", manodopera);
        c.put("viaggi", viaggi);
        c.put("materiali", materiali);
        Map<String, Object> totali = new LinkedHashMap<>();
        // totali in ore (anche le righe in minuti), con la descrizione dell'unita' OR
        totali.put("ore_lavoro", numero(minutiLavoro / 60));
        totali.put("ore_viaggio", numero(minutiViaggio / 60));
        totali.put("totale_ore", numero((minutiLavoro + minutiViaggio) / 60));
        ContentValues unitaOre = db.getRecord("select " + com.ncfsistemi.econ.db.table.UnitaMisura.NOME + " from " + com.ncfsistemi.econ.db.table.UnitaMisura.NOME_TABELLA
                + " where " + com.ncfsistemi.econ.db.table.UnitaMisura.UNITA_MISURA + "='" + com.ncfsistemi.econ.db.table.UnitaMisura.CODICE_ORE + "'");
        String umOre = unitaOre != null ? RigheRapportino.testo(unitaOre, com.ncfsistemi.econ.db.table.UnitaMisura.NOME) : "";
        totali.put("um_ore", umOre.isEmpty() ? "Ore" : umOre);
        totali.put("materiali", materiali.size());
        totali.put("ha_viaggi", !viaggi.isEmpty());
        totali.put("ha_materiali", !materiali.isEmpty());
        c.put("totali", totali);
        Map<String, Object> st = new LinkedHashMap<>();
        st.put("data_ora", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(new Date()));
        // "1.0.48 / 1.0.44": versione del server (ultima vista) / versione di questa app
        String server = com.ncfsistemi.econ.utils.VersioneApp.server(ctx), app = com.ncfsistemi.econ.utils.VersioneApp.salvata(ctx);
        st.put("versione", (server != null ? server : "?") + " / " + (app != null ? app : com.ncfsistemi.econ.BuildConfig.VERSION_NAME));
        c.put("stampa", st);
        return c;
    }

    private static Map<String, Object> ditta(Context ctx) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        MercuryApiService.DittaDati d = DittaLocale.getDati(ctx);
        m.put("ragione_sociale", d != null && d.ragioneSociale != null ? d.ragioneSociale : "");
        m.put("indirizzo", d != null && d.indirizzo != null ? d.indirizzo : "");
        m.put("cap", d != null && d.cap != null ? d.cap : "");
        m.put("citta", d != null && d.citta != null ? d.citta : "");
        m.put("provincia", d != null && d.provincia != null ? d.provincia : "");
        m.put("partita_iva", d != null && d.partitaIva != null ? d.partitaIva : "");
        m.put("codice_fiscale", d != null && d.codiceFiscale != null ? d.codiceFiscale : "");
        m.put("telefono", d != null && d.telefono != null ? d.telefono : "");
        m.put("cellulare", d != null && d.cellulare != null ? d.cellulare : "");
        m.put("email", d != null && d.email != null ? d.email : "");
        m.put("note_rapportini", d != null && d.noteRapportini != null ? d.noteRapportini : "");
        File logo = DittaLocale.getFileLogo(ctx);
        String dataUri = "";
        if (logo != null && logo.isFile()) {
            byte[] b = Files.readAllBytes(logo.toPath());
            String tipo = b.length > 3 && (b[0] & 0xFF) == 0xFF ? "image/jpeg" : b.length > 11 && b[8] == 'W' && b[9] == 'E' ? "image/webp" : "image/png";
            dataUri = "data:" + tipo + ";base64," + Base64.encodeToString(b, Base64.NO_WRAP);
        }
        m.put("logo", dataUri);
        return m;
    }

    private static String indirizzo(String via, String civico, String cap, String citta, String prov) {
        String v = (via + " " + civico).trim();
        String luogo = (cap + " " + citta + (prov.isEmpty() ? "" : " (" + prov + ")")).trim();
        return (v + (!v.isEmpty() && !luogo.isEmpty() ? ", " : "") + luogo).trim();
    }

    private static String dataOra(Long n) {
        String s = n != null ? String.valueOf(n) : "";
        return s.length() == 14 ? Utility.numberToData(n) + " " + s.substring(8, 10) + ":" + s.substring(10, 12) : "";
    }

    /** 7,5 → "7,5"; 3 → "3" (come il server) */
    static String numero(double n) {
        boolean intero = Math.abs(n - Math.rint(n)) < 0.0001;
        java.text.DecimalFormat f = new java.text.DecimalFormat(intero ? "#,##0" : "#,##0.00", java.text.DecimalFormatSymbols.getInstance(Locale.ITALY));
        return f.format(n);
    }

    /** minuti → "7:10" */
    static String oreMinuti(double minuti) {
        long m = Math.round(minuti);
        return String.format(Locale.ITALY, "%d:%02d", m / 60, m % 60);
    }
}
