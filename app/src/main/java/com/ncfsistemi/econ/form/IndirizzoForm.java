package com.ncfsistemi.econ.form;

import android.content.ContentValues;
import android.widget.Toast;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.ClientiIndirizzi;
import com.ncfsistemi.econ.utils.CantiereDaIndirizzo;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/**
 * Indirizzo del cliente (ClienteIndirizzoModActivity, GESTIONE_CLIENTI.md §6). Extra: "ID" (modifica) oppure
 * ClientiIndirizzi.ID_ANAGRAFICA (nuovo). Se diventa principale, gli altri indirizzi del cliente non lo sono piu' (il
 * server fa lo stesso e copia la sede sul cliente). Nel nuovo, con CANTIERI_CREA, si puo' creare subito il cantiere.
 */
public class IndirizzoForm extends DefinizioneForm {

	private static final String CREA_CANTIERE = "crea_cantiere";

	@Override
	public AbstractTable tabella() {
		return new ClientiIndirizzi();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuovo indirizzo" : "Modifica indirizzo";
	}

	@Override
	public String moduloFunzionalita() {
		return FunzionalitaApp.CLIENTI;
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.INDIRIZZO, "Indirizzo")
				.scelta(ClientiIndirizzi.TIPO, "Tipo", ClientiIndirizzi.TIPI).obbligatorio().predefinito(ClientiIndirizzi.TIPI[0][0]).metaRiga()
				.testo(ClientiIndirizzi.DESCRIZIONE, "Descrizione").max(100).aiuto("Es. Magazzino").metaRiga()
				.testo(ClientiIndirizzi.INDIRIZZO, "Via / piazza").max(150).metaRiga(4).nuovaRiga()
				.testo(ClientiIndirizzi.CIVICO, "Civico").max(15).maiuscolo().metaRiga()
				.intero(ClientiIndirizzi.CAP, "CAP").max(10).metaRiga().nuovaRiga()
				.testo(ClientiIndirizzi.CITTA, "Città").max(70).metaRiga(3)
				.testo(ClientiIndirizzi.PROVINCIA, "Prov.").max(2).maiuscolo().metaRiga()
				.testo(ClientiIndirizzi.NAZIONE, "Nazione").max(2).maiuscolo().aiuto("IT, CH, FR…").predefinito("IT")
				.note(ClientiIndirizzi.NOTE, "Note").max(255)
				.siNo(ClientiIndirizzi.PRINCIPALE, "Sede principale")
				.siNo(AbstractTable.ATTIVO, "Attivo").predefinito("1")
				.siNo(CREA_CANTIERE, "Crea anche il cantiere a questo indirizzo").appoggio().soloInserimento();
	}

	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		if (nuovo && !FunzionalitaApp.ha(ospite, FunzionalitaApp.CANTIERI_CREA)) f.nascondi(CREA_CANTIERE);
	}

	/** Italia: CAP di 5 cifre. */
	@Override
	public void controlla(Form f, boolean nuovo) {
		String nazione = f.valore(ClientiIndirizzi.NAZIONE);
		String cap = f.valore(ClientiIndirizzi.CAP);
		if ((nazione.isEmpty() || nazione.equals("IT")) && !cap.isEmpty() && !cap.matches("\\d{5}")) {
			f.errore(ClientiIndirizzi.CAP, "Il CAP italiano ha 5 cifre");
		}
	}

	private int cliente() {
		ContentValues record = ospite.getRecord();
		if (record != null) return record.getAsInteger(ClientiIndirizzi.ID_ANAGRAFICA);
		return ospite.getIntent().getIntExtra(ClientiIndirizzi.ID_ANAGRAFICA, 0);
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		if (val.getAsString(ClientiIndirizzi.NAZIONE).isEmpty()) val.put(ClientiIndirizzi.NAZIONE, "IT");
		if (nuovo) val.put(ClientiIndirizzi.ID_ANAGRAFICA, cliente());
	}

	@Override
	public void dopoSalvataggio(DbInterno db, ContentValues val, boolean nuovo) {
		long questo = nuovo ? val.getAsLong(ClientiIndirizzi.ID) : ospite.getIDModifica();
		// un solo principale per cliente: gli altri indirizzi smettono di esserlo (da inviare)
		Integer principale = val.getAsInteger(ClientiIndirizzi.PRINCIPALE);
		if (principale != null && principale == 1) {
			db.getWritableDatabase().execSQL("UPDATE " + ClientiIndirizzi.NOME_TABELLA + " SET " + ClientiIndirizzi.PRINCIPALE + " = 0, "
					+ AbstractTable.IN_SERVER + " = 0 WHERE " + ClientiIndirizzi.ID_ANAGRAFICA + " = ? AND " + ClientiIndirizzi.ID + " <> ? AND "
					+ ClientiIndirizzi.PRINCIPALE + " = 1", new Object[] { cliente(), questo });
		}
		Campo crea = ospite.getForm().campo(CREA_CANTIERE);
		if (nuovo && crea.isVisibile() && "1".equals(crea.valore())) {
			String esito = CantiereDaIndirizzo.crea(db, val.getAsInteger(ClientiIndirizzi.ID));
			Toast.makeText(ospite, esito, Toast.LENGTH_LONG).show();
		}
	}
}
