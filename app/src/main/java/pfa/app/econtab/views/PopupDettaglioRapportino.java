package pfa.app.econtab.views;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import pfa.app.econtab.R;
import pfa.app.econtab.RapportinoDettaglioModActivity;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.RapportiniDettaglioOperatori;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.utils.RegoleRapportino;
import pfa.app.econtab.utils.Utility;

/**
 * Riga di rapportino: tipo (viaggio andata, lavoro, viaggio ritorno), descrizione, orari facoltativi, durata,
 * riga MD dell'ordine a cui imputare le ore, "a costo" (solo viaggio), operatori e nota. La durata (calcolata dagli
 * orari se ci sono entrambi) vale per ciascun operatore scelto, modificabile per singolo operatore: le ore-uomo sono
 * la somma delle ore degli operatori, e solo quelle si salvano. Gli operatori sceglibili dipendono dall'utente, dal
 * cantiere e dalla settimana del rapportino (RegoleRapportino).
 */
public class PopupDettaglioRapportino implements OnClickListener, TextWatcher {

	private View vista = null;
	private Context ctx = null;
	private AlertDialog di = null;

	private int idRapportino = 0;
    private int idRapportinoDettaglio = 0;
	private RadioGroup radioTipo = null;
	private EditText editDescrizione = null;
	private EditText editOraInizio = null;
	private EditText editOraFine = null;
	private EditText editOre = null;
	private EConTabSpinner spinnerRigaOrdine = null;
	private CheckBox checkACosto = null;
    private EditText editNota = null;

    /** Operatori mostrati, per id_utente_ditta, nell'ordine della lista. */
    private final Map<Integer, RigaOperatore> operatori = new LinkedHashMap<Integer, RigaOperatore>();
    /** true mentre si aggiornano le ore da codice: i TextWatcher non devono marcarle come modificate a mano. */
    private boolean aggiornamentoInCorso = false;

    /** Un operatore nel popup: casella di scelta e ore, piu' l'eventuale record gia' salvato. */
    private final class RigaOperatore {
        int idUtenteDitta;
        CheckBox scelto;
        EditText ore;
        /** id in rapportini_dettaglio_operatori, 0 se non ancora salvato */
        int idSalvato = 0;
        double oreSalvate = 0;
        /** ore cambiate a mano: non seguono piu' le ore della riga */
        boolean oreProprie = false;
    }

	public PopupDettaglioRapportino(Context ctx,  int idRapportino,int idRapportinoDettaglio) {
		vista = View.inflate(ctx, R.layout.dialog_rapportino_dett, null);
		this.ctx = ctx;
		this.idRapportino = idRapportino;
        this.idRapportinoDettaglio = idRapportinoDettaglio;
		impostaCampi();
	}

	private void impostaCampi() {
        radioTipo = (RadioGroup) vista.findViewById(R.id.radio_tipo);
        editDescrizione = (EditText) vista.findViewById(R.id.editText_descrizione);
        editOraInizio = (EditText) vista.findViewById(R.id.editText_ora_inizio);
        editOraFine = (EditText) vista.findViewById(R.id.editText_ora_fine);
		editOre  = (EditText) vista.findViewById(R.id.editText_ore);
        spinnerRigaOrdine = (EConTabSpinner) vista.findViewById(R.id.econtabSpinner_riga_ordine);
        checkACosto = (CheckBox) vista.findViewById(R.id.checkBox_a_costo);
        editNota  = (EditText) vista.findViewById(R.id.editText_nota_dett);

        String tipo = RapportiniDettaglio.TIPO_LAVORO;
        String rigaOrdine = "0";
        if (idRapportinoDettaglio != 0) {
            DbInterno db = new DbInterno(ctx);
            ContentValues where = new ContentValues();
            where.put(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO,idRapportinoDettaglio);
            ContentValues valDett = db.getRecord(new RapportiniDettaglio(),where);
            ArrayList<Object> salvati = RapportiniDettaglioOperatori.operatoriRiga(db, idRapportinoDettaglio);
            db.close();
            if (valDett!=null){
                if (valDett.getAsString(RapportiniDettaglio.TIPO) != null) tipo = valDett.getAsString(RapportiniDettaglio.TIPO);
                editDescrizione.setText(valDett.getAsString(RapportiniDettaglio.DESCRIZIONE));
                editOraInizio.setText(valDett.getAsString(RapportiniDettaglio.ORA_INIZIO));
                editOraFine.setText(valDett.getAsString(RapportiniDettaglio.ORA_FINE));
                editNota.setText(valDett.getAsString(RapportiniDettaglio.NOTE));
                Integer aCosto = valDett.getAsInteger(RapportiniDettaglio.A_COSTO);
                checkACosto.setChecked(aCosto == null || aCosto != 0);
                Integer riga = valDett.getAsInteger(RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO);
                rigaOrdine = "" + (riga != null ? riga : 0);
            }
            // durata proposta: le ore del primo operatore (le ore della riga non si salvano)
            if (!salvati.isEmpty()) {
                editOre.setText(Utility.formatNumero(((ContentValues) salvati.get(0)).getAsDouble(RapportiniDettaglioOperatori.ORE)));
            }
        }
        radioTipo.check(RapportiniDettaglio.TIPO_VIAGGIO_ANDATA.equals(tipo) ? R.id.radio_viaggio_andata
                : RapportiniDettaglio.TIPO_VIAGGIO_RITORNO.equals(tipo) ? R.id.radio_viaggio_ritorno : R.id.radio_lavoro);
        radioTipo.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                aggiornaPerTipo();
            }
        });
        aggiornaPerTipo();

        impostaRigheOrdine(rigaOrdine);
        impostaOrario(editOraInizio);
        impostaOrario(editOraFine);
        editDescrizione.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                controllaAbilitazioneTastoConferma();
            }
        });
        impostaOperatori();
        // dopo i valori iniziali: da qui le ore della riga si propagano agli operatori
        editOre.addTextChangedListener(this);
	}

    private String tipoScelto() {
        int id = radioTipo.getCheckedRadioButtonId();
        if (id == R.id.radio_viaggio_andata) return RapportiniDettaglio.TIPO_VIAGGIO_ANDATA;
        if (id == R.id.radio_viaggio_ritorno) return RapportiniDettaglio.TIPO_VIAGGIO_RITORNO;
        return RapportiniDettaglio.TIPO_LAVORO;
    }

    /** "A costo" solo per il viaggio (il lavoro e' sempre a costo); la descrizione e' obbligatoria solo per il lavoro. */
    private void aggiornaPerTipo() {
        boolean viaggio = RapportiniDettaglio.isViaggio(tipoScelto());
        checkACosto.setVisibility(viaggio ? View.VISIBLE : View.GONE);
        editDescrizione.setHint(viaggio ? "" : ctx.getString(R.string.descrizione_lavoro_obbligatoria));
        controllaAbilitazioneTastoConferma();
    }

    /** Righe MD dell'ordine del rapportino, piu' "extra ordine"; nascosto se il rapportino non ha un ordine. */
    private void impostaRigheOrdine(String valore) {
        DbInterno db = new DbInterno(ctx);
        ContentValues whereRapp = new ContentValues();
        whereRapp.put(Rapportini.ID_RAPPORTINO, idRapportino);
        ContentValues rapp = db.getRecord(new Rapportini(), whereRapp);
        int idOrdine = rapp != null && rapp.getAsInteger(Rapportini.ID_ORDINE) != null ? rapp.getAsInteger(Rapportini.ID_ORDINE) : 0;
        ArrayList<Object> righe = idOrdine == 0 ? new ArrayList<Object>() : db.eseguiSelect("Select "
                + PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO + ", " + PreventiviDettaglio.DESCRIZIONE + " from " + PreventiviDettaglio.NOME_TABELLA
                + " where " + PreventiviDettaglio.ID_PREVENTIVO + "=" + idOrdine + " and " + PreventiviDettaglio.TIPO + "='" + PreventiviDettaglio.MANOPERA + "'"
                + " order by " + PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO, null);
        db.close();

        ArrayList<Object> valori = new ArrayList<Object>();
        ContentValues extra = new ContentValues();
        extra.put(EConTabSpinner.VALORE, "0");
        extra.put(EConTabSpinner.DESCRIZIONE, ctx.getString(R.string.extra_ordine));
        valori.add(extra);
        boolean presente = "0".equals(valore);
        for (Object o : righe) {
            ContentValues r = (ContentValues) o;
            ContentValues v = new ContentValues();
            v.put(EConTabSpinner.VALORE, "" + r.getAsInteger(PreventiviDettaglio.ID_PREVENTIVO_DETTAGLIO));
            v.put(EConTabSpinner.DESCRIZIONE, r.getAsString(PreventiviDettaglio.DESCRIZIONE));
            valori.add(v);
            if (v.getAsString(EConTabSpinner.VALORE).equals(valore)) presente = true;
        }
        spinnerRigaOrdine.setValue(presente ? valore : "0");
        spinnerRigaOrdine.setValoriSpinnerLibero(valori);
        vista.findViewById(R.id.linearRigaOrdine).setVisibility(idOrdine == 0 ? View.GONE : View.VISIBLE);
    }

    /** Campo orario: tocco = scelta dell'ora (con "Pulisci" per toglierla); cambiando gli orari si ricalcola la durata. */
    private void impostaOrario(final EditText campo) {
        campo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] hm = oraMinuti(campo.getText().toString());
                if (hm == null) {
                    java.util.Calendar ora = java.util.Calendar.getInstance();
                    hm = new int[] { ora.get(java.util.Calendar.HOUR_OF_DAY), ora.get(java.util.Calendar.MINUTE) };
                }
                TimePickerDialog dlg = new TimePickerDialog(ctx, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int ore, int minuti) {
                        campo.setText(String.format(java.util.Locale.ROOT, "%02d:%02d", ore, minuti));
                        calcolaDurata();
                    }
                }, hm[0], hm[1], true);
                dlg.setButton(DialogInterface.BUTTON_NEUTRAL, ctx.getString(R.string.pulisci), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        campo.setText("");
                        controllaAbilitazioneTastoConferma();
                    }
                });
                dlg.show();
            }
        });
    }

    /** "HH:MM" → {ore, minuti}, null se vuoto o non valido. */
    private static int[] oraMinuti(String s) {
        if (s == null) return null;
        String[] p = s.trim().split(":");
        if (p.length < 2) return null;
        try {
            return new int[] { Integer.parseInt(p[0]), Integer.parseInt(p[1]) };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Con entrambi gli orari la durata (in ore, a cavallo della mezzanotte se la fine e' prima dell'inizio) va nelle ore. */
    private void calcolaDurata() {
        int[] inizio = oraMinuti(editOraInizio.getText().toString());
        int[] fine = oraMinuti(editOraFine.getText().toString());
        if (inizio != null && fine != null) {
            int minuti = (fine[0] * 60 + fine[1]) - (inizio[0] * 60 + inizio[1]);
            if (minuti < 0) minuti += 24 * 60;
            editOre.setText(Utility.formatNumero(Utility.arrotonda(minuti / 60.0, 2)));
        }
        controllaAbilitazioneTastoConferma();
    }

    /** Elenco operatori sceglibili, con spuntati quelli gia' sulla riga (anche se nel frattempo non piu' sceglibili). */
    private void impostaOperatori() {
        DbInterno db = new DbInterno(ctx);
        RegoleRapportino.Utente utente = RegoleRapportino.utenteCorrente(db, ctx);

        ContentValues whereRapp = new ContentValues();
        whereRapp.put(Rapportini.ID_RAPPORTINO, idRapportino);
        ContentValues rapp = db.getRecord(new Rapportini(), whereRapp);
        int idCantiere = rapp != null && rapp.getAsInteger(Rapportini.ID_CANTIERE) != null ? rapp.getAsInteger(Rapportini.ID_CANTIERE) : 0;
        long data = rapp != null && rapp.getAsLong(Rapportini.DATA_RAPPORTINO) != null ? rapp.getAsLong(Rapportini.DATA_RAPPORTINO) : 0;

        ArrayList<ContentValues> sceglibili = RegoleRapportino.operatoriSelezionabili(db, utente, idCantiere, data);
        ArrayList<Object> salvati = idRapportinoDettaglio != 0
                ? RapportiniDettaglioOperatori.operatoriRiga(db, idRapportinoDettaglio) : new ArrayList<Object>();
        db.close();

        LinearLayout lista = (LinearLayout) vista.findViewById(R.id.lista_operatori);
        for (ContentValues ut : sceglibili) {
            String nome = RegoleRapportino.nome(ut);
            if (ut.getAsInteger(RegoleRapportino.PIANIFICATO) == 1 && sceglibili.size() > 1) {
                nome += " (" + ctx.getString(R.string.pianificato) + ")";
            }
            aggiungiOperatore(lista, ut.getAsInteger(Utenti.ID_UTENTE_DITTA), nome);
        }
        for (Object o : salvati) {
            ContentValues op = (ContentValues) o;
            int id = op.getAsInteger(RapportiniDettaglioOperatori.ID_UTENTE_DITTA);
            RigaOperatore r = operatori.get(id);
            if (r == null) {
                // gia' sulla riga ma non piu' sceglibile (es. pianificazione cambiata o riga di un coordinatore): resta
                String nome = ((op.getAsString(RapportiniDettaglioOperatori.NOME_OPERATORE) != null ? op.getAsString(RapportiniDettaglioOperatori.NOME_OPERATORE) : "")
                        + " " + (op.getAsString(RapportiniDettaglioOperatori.COGNOME_OPERATORE) != null ? op.getAsString(RapportiniDettaglioOperatori.COGNOME_OPERATORE) : "")).trim();
                r = aggiungiOperatore(lista, id, nome.isEmpty() ? "#" + id : nome);
            }
            r.idSalvato = op.getAsInteger(RapportiniDettaglioOperatori.ID_RAPPORTINO_DETTAGLIO_OPERATORE);
            r.oreSalvate = op.getAsDouble(RapportiniDettaglioOperatori.ORE);
            aggiornamentoInCorso = true;
            r.scelto.setChecked(true);
            r.ore.setText(Utility.formatNumero(r.oreSalvate));
            aggiornamentoInCorso = false;
            r.oreProprie = !Utility.formatNumero(r.oreSalvate).equals(editOre.getText().toString());
        }

        TextView info = (TextView) vista.findViewById(R.id.textView_operatori_info);
        boolean soloSeStesso = !utente.amministratore && sceglibili.size() == 1
                && sceglibili.get(0).getAsInteger(Utenti.ID_UTENTE_DITTA) == utente.idUtenteDitta;
        if (sceglibili.isEmpty()) {
            info.setText(utente.idUtenteDitta == 0 && !utente.amministratore ? R.string.operatori_non_scaricati : R.string.nessun_cantiere_rapportino);
            info.setVisibility(View.VISIBLE);
        } else if (soloSeStesso) {
            info.setText(R.string.solo_ore_proprie);
            info.setVisibility(View.VISIBLE);
        }

        // riga nuova: l'utente stesso gia' spuntato (se puo' sceglierlo); se puo' mettere ore solo su se stesso non si toglie
        if (idRapportinoDettaglio == 0) {
            RigaOperatore io = operatori.get(utente.idUtenteDitta);
            if (io != null) {
                io.scelto.setChecked(true);
            }
        }
        if (soloSeStesso) {
            RigaOperatore io = operatori.get(utente.idUtenteDitta);
            if (io != null && io.scelto.isChecked() && operatori.size() == 1) {
                io.scelto.setEnabled(false);
            }
        }
    }

    private RigaOperatore aggiungiOperatore(LinearLayout lista, int idUtenteDitta, String nome) {
        final RigaOperatore r = new RigaOperatore();
        r.idUtenteDitta = idUtenteDitta;

        LinearLayout riga = new LinearLayout(ctx);
        riga.setOrientation(LinearLayout.HORIZONTAL);
        riga.setGravity(Gravity.CENTER_VERTICAL);

        r.scelto = new CheckBox(ctx);
        r.scelto.setText(nome);
        riga.addView(r.scelto, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        r.ore = new EditText(ctx);
        r.ore.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        r.ore.setEms(3);
        r.ore.setMaxLines(1);
        r.ore.setHint(R.string.ore);
        r.ore.setEnabled(false);
        riga.addView(r.ore, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        r.scelto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean scelto) {
                r.ore.setEnabled(scelto);
                if (scelto && !r.oreProprie) {
                    aggiornamentoInCorso = true;
                    r.ore.setText(editOre.getText().toString());
                    aggiornamentoInCorso = false;
                }
                controllaAbilitazioneTastoConferma();
            }
        });
        r.ore.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!aggiornamentoInCorso) {
                    r.oreProprie = true;
                }
                controllaAbilitazioneTastoConferma();
            }
        });

        lista.addView(riga);
        operatori.put(idUtenteDitta, r);
        return r;
    }

	public void apriPopup() {

		di = Utility.mostraDialogPersonalizzato(ctx.getString(R.string.dettaglio_rapportino), ctx, vista, ctx.getString(R.string.salva),
				ctx.getString(R.string.annulla), this);

		controllaAbilitazioneTastoConferma();
	}

	@Override
	public void onClick(DialogInterface arg0, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			DbInterno db = new DbInterno(ctx);
            RapportiniDettaglio tabRappDett = new RapportiniDettaglio();
            ContentValues val = idRapportinoDettaglio == 0 ? tabRappDett.getValoriLogInserimento(db) : tabRappDett.getValoriLogModifica(db);
            String tipo = tipoScelto();
            val.put(RapportiniDettaglio.TIPO, tipo);
            val.put(RapportiniDettaglio.DESCRIZIONE, editDescrizione.getText().toString().trim());
            val.put(RapportiniDettaglio.ORA_INIZIO, editOraInizio.getText().toString());
            val.put(RapportiniDettaglio.ORA_FINE, editOraFine.getText().toString());
            val.put(RapportiniDettaglio.A_COSTO, !RapportiniDettaglio.isViaggio(tipo) || checkACosto.isChecked() ? 1 : 0);
            val.put(RapportiniDettaglio.ID_PREVENTIVO_DETTAGLIO, spinnerRigaOrdine.getValue().equals("") ? 0 : Integer.parseInt(spinnerRigaOrdine.getValue()));
            val.put(RapportiniDettaglio.NOTE, editNota.getText().toString());
            if (idRapportinoDettaglio==0){
                val.put(RapportiniDettaglio.ID_RAPPORTINO,idRapportino);
                tabRappDett.inserisciRecord(db,val);
                idRapportinoDettaglio = val.getAsInteger(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO);
            }
            else{
                ContentValues where = new ContentValues();
                where.put(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO,idRapportinoDettaglio);
                tabRappDett.aggiornaRecord(db,val,where);
            }
            salvaOperatori(db);
            // il rapportino non scrive piu' nell'ordine: il legame e' la riga d'ordine scelta (§9.5 dello studio)

			db.close();
            ((RapportinoDettaglioModActivity)ctx).caricaDettaglio();
		}
	}

    /** Inserisce, aggiorna o toglie gli operatori della riga secondo le caselle spuntate. */
    private void salvaOperatori(DbInterno db) {
        RapportiniDettaglioOperatori tab = new RapportiniDettaglioOperatori();
        for (RigaOperatore r : operatori.values()) {
            double ore = oreOperatore(r);
            if (r.scelto.isChecked()) {
                if (r.idSalvato == 0) {
                    ContentValues val = tab.getValoriLogInserimento(db);
                    val.put(RapportiniDettaglioOperatori.ID_RAPPORTINO_DETTAGLIO, idRapportinoDettaglio);
                    val.put(RapportiniDettaglioOperatori.ID_UTENTE_DITTA, r.idUtenteDitta);
                    val.put(RapportiniDettaglioOperatori.ORE, ore);
                    tab.inserisciRecord(db, val);
                } else if (ore != r.oreSalvate) {
                    ContentValues val = tab.getValoriLogModifica(db);
                    val.put(RapportiniDettaglioOperatori.ORE, ore);
                    ContentValues where = new ContentValues();
                    where.put(RapportiniDettaglioOperatori.ID_RAPPORTINO_DETTAGLIO_OPERATORE, r.idSalvato);
                    tab.aggiornaRecord(db, val, where);
                }
            } else if (r.idSalvato != 0) {
                ContentValues where = new ContentValues();
                where.put(RapportiniDettaglioOperatori.ID_RAPPORTINO_DETTAGLIO_OPERATORE, r.idSalvato);
                tab.cancellaRecord(db, where);
            }
        }
    }

    /** Ore dell'operatore; se lasciate vuote valgono le ore della riga. */
    private double oreOperatore(RigaOperatore r) {
        String testo = r.ore.getText().toString().trim();
        return Utility.formatNumeroDB(testo.isEmpty() ? editOre.getText().toString() : testo);
    }

	private void controllaAbilitazioneTastoConferma() {
        if (di == null) return;
        boolean almenoUnOperatore = false;
        for (RigaOperatore r : operatori.values()) {
            if (r.scelto.isChecked()) almenoUnOperatore = true;
        }
        boolean descrizioneOk = RapportiniDettaglio.isViaggio(tipoScelto()) || !editDescrizione.getText().toString().trim().isEmpty();
        boolean ok = descrizioneOk && !editOre.getText().toString().equals("") && almenoUnOperatore;
        di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(ok);
	}

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    /** Durata della riga cambiata: si ricopia sugli operatori non modificati a mano. */
    @Override
    public void afterTextChanged(Editable editable) {
        aggiornamentoInCorso = true;
        for (RigaOperatore r : operatori.values()) {
            if (r.scelto.isChecked() && !r.oreProprie) {
                r.ore.setText(editOre.getText().toString());
            }
        }
        aggiornamentoInCorso = false;
        controllaAbilitazioneTastoConferma();
    }
}
