package com.ncfsistemi.econ.form;

import android.content.ContentValues;

import com.ncfsistemi.econ.R;
import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Anagrafica;
import com.ncfsistemi.econ.db.table.ClientiIndirizzi;
import com.ncfsistemi.econ.db.table.Iva;
import com.ncfsistemi.econ.utils.FaIcone;
import com.ncfsistemi.econ.utils.FunzionalitaApp;

/**
 * Nuovo cliente / modifica del cliente (ClientiDettaglioModActivity, GESTIONE_CLIENTI.md §6): Dati principali (azienda o
 * persona fisica, attivo), Sede principale (solo nel nuovo cliente: il server ne fa il suo primo indirizzo), Dati
 * fatturazione. Gli altri indirizzi e i referenti si gestiscono dalla scheda del cliente.
 */
public class ClienteForm extends DefinizioneForm {

	private static final String VIA = "sede_via";
	private static final String CIVICO = "sede_civico";

	@Override
	public AbstractTable tabella() {
		return new Anagrafica();
	}

	@Override
	public String titolo(boolean nuovo) {
		return nuovo ? "Nuovo cliente" : "Modifica cliente";
	}

	@Override
	public String sottotitolo(ContentValues record) {
		String codice = record.getAsString(Anagrafica.CODICE);
		String nome = record.getAsString(Anagrafica.RAGIONE_SOCIALE);
		return codice == null || codice.isEmpty() ? nome : nome + " · " + codice;
	}

	@Override
	public String moduloFunzionalita() {
		return FunzionalitaApp.CLIENTI;
	}

	@Override
	public void sezioni(Form f) {
		f.sezione(FaIcone.CLIENTE, "Dati principali")
				.opzioni(Anagrafica.TIPO_CF, "Tipo", new String[][] {
						{ Anagrafica.TIPO_AZIENDA, "Azienda / ente" }, { Anagrafica.TIPO_PERSONA_FISICA, "Persona fisica" } })
				.predefinito(Anagrafica.TIPO_AZIENDA)
				.testo(Anagrafica.RAGIONE_SOCIALE, "Ragione sociale / nome e cognome").obbligatorio().max(100).metaRiga(3)
				// il codice del cliente arriva dal server: non si modifica dal dispositivo
				.testo(Anagrafica.CODICE, contesto.getString(R.string.codice)).max(20).soloLettura().metaRiga()
				.avviso("Il codice del cliente lo assegna il server alla prima sincronizzazione (o lo si scrive dal portale): dall'app non si modifica.")
				.telefono(Anagrafica.TELEFONO, "Telefono").max(20).metaRiga().nuovaRiga()
				.telefono(Anagrafica.CELLULARE, "Cellulare").max(20).icona(FaIcone.CELLULARE).metaRiga()
				.email(Anagrafica.MAIL, "Email").max(150)
				.note(Anagrafica.NOTE, "Note").max(255)
				// codice del cliente nel gestionale della ditta: si imposta dal portale o con l'importazione
				.testo(Anagrafica.CODICE_ESTERNO, "Codice esterno").soloLettura().soloModifica()
				.avviso("È il codice del cliente nel tuo gestionale, usato per la sincronizzazione con il gestionale. Si imposta dal portale (Dati interfacciamento) o con l'importazione clienti: dall'app non si modifica.")
				.siNo(AbstractTable.ATTIVO, "Attivo").aiuto("Disattivato: esce dal tablet alla prossima sincronizzazione").predefinito("1");
		// sede con via e civico separati: diventa il primo indirizzo del cliente (dopoSalvataggio)
		f.sezione(FaIcone.INDIRIZZO, "Sede principale")
				.testo(VIA, "Via / piazza").max(150).appoggio().soloInserimento().metaRiga(4)
				.aiuto("Gli altri indirizzi si aggiungono dalla scheda del cliente, linguetta Indirizzi.")
				.testo(CIVICO, "Civico").max(15).maiuscolo().appoggio().soloInserimento().metaRiga()
				.intero(Anagrafica.CAP, "CAP").max(10).soloInserimento().metaRiga().nuovaRiga()
				.testo(Anagrafica.CITTA, "Città").max(70).soloInserimento().metaRiga(3)
				.testo(Anagrafica.PROVINCIA, "Prov.").max(2).maiuscolo().soloInserimento().metaRiga();
		f.sezione(FaIcone.FATTURAZIONE, "Dati fatturazione")
				// persona fisica: niente partita IVA
				.testo(Anagrafica.PARTITA_IVA, "Partita IVA").max(50).visibileSe(Anagrafica.TIPO_CF, Anagrafica.TIPO_AZIENDA).metaRiga()
				.testo(Anagrafica.CODICE_FISCALE, "Codice fiscale").max(16).maiuscolo().metaRiga()
				.scelta(Anagrafica.CODICE_IVA, contesto.getString(R.string.iva), new Iva()).obbligatorio()
				.email(Anagrafica.PEC, "PEC").max(150).metaRiga().nuovaRiga()
				.testo(Anagrafica.CODICE_SDI, "Codice SDI").max(7).maiuscolo().metaRiga();
	}

	/**
	 * Nuovo cliente: IVA proposta = quella predefinita della ditta (portale, Amministrazione › Azienda); senza, la piu'
	 * usata dai clienti della ditta, altrimenti il 22%.
	 */
	@Override
	public void dopoCaricamento(Form f, ContentValues record, boolean nuovo) {
		if (!nuovo) return;
		String dittaIva = com.ncfsistemi.econ.api.TokenManager.getInstance(contesto).getIvaPredefinita();
		// solo se il codice e' gia' tra le IVA del tablet, altrimenti la regola sotto
		if (dittaIva != null && f.campo(Anagrafica.CODICE_IVA).codici.contains(dittaIva)) {
			f.imposta(Anagrafica.CODICE_IVA, dittaIva);
			return;
		}
		DbInterno db = new DbInterno(contesto);
		java.util.ArrayList<Object> r = db.eseguiSelect("select " + Anagrafica.CODICE_IVA + " from " + Anagrafica.NOME_TABELLA + " where coalesce("
				+ Anagrafica.CODICE_IVA + ", '') <> '' group by " + Anagrafica.CODICE_IVA + " order by count(*) desc limit 1", null);
		db.close();
		f.imposta(Anagrafica.CODICE_IVA, r.isEmpty() ? "022" : ((ContentValues) r.get(0)).getAsString(Anagrafica.CODICE_IVA));
	}

	/**
	 * Nuovo cliente: prima di salvare si cercano clienti gia' presenti con lo stesso nome/ragione sociale, partita IVA,
	 * codice fiscale o indirizzo (sul cliente o tra i suoi indirizzi); se ce ne sono si avvisa e l'operatore sceglie se
	 * crearlo lo stesso (21/10).
	 */
	@Override
	public String messaggioConferma(Form f, boolean nuovo) {
		if (!nuovo) return null;
		String nome = norm(f.valore(Anagrafica.RAGIONE_SOCIALE)), piva = norm(f.valore(Anagrafica.PARTITA_IVA)),
				cf = norm(f.valore(Anagrafica.CODICE_FISCALE)), via = norm(f.valore(VIA)), civico = norm(f.valore(CIVICO)),
				citta = norm(f.valore(Anagrafica.CITTA));
		java.util.LinkedHashMap<Integer, String> trovati = new java.util.LinkedHashMap<>();
		DbInterno db = new DbInterno(contesto);
		String a = Anagrafica.NOME_TABELLA;
		cerca(db, trovati, "select * from " + a + " where lower(trim(" + Anagrafica.RAGIONE_SOCIALE + ")) = ?", nome, "stesso nome");
		cerca(db, trovati, "select * from " + a + " where lower(trim(" + Anagrafica.PARTITA_IVA + ")) = ?", piva, "stessa partita IVA");
		cerca(db, trovati, "select * from " + a + " where lower(trim(" + Anagrafica.CODICE_FISCALE + ")) = ?", cf, "stesso codice fiscale");
		if (!via.isEmpty() && !citta.isEmpty()) {
			String i = ClientiIndirizzi.NOME_TABELLA;
			// stesso indirizzo: sul cliente (copia "via civico" della sede) o tra gli indirizzi dei clienti
			cerca(db, trovati, "select * from " + a + " where lower(trim(" + Anagrafica.INDIRIZZO + ")) = ? and lower(trim("
					+ Anagrafica.CITTA + ")) = '" + citta.replace("'", "''") + "'", (via + " " + civico).trim(), "stesso indirizzo");
			cerca(db, trovati, "select " + a + ".* from " + i + " join " + a + " on " + a + "." + Anagrafica.ID_ANAGRAFICA + " = " + i + "."
					+ ClientiIndirizzi.ID_ANAGRAFICA + " where lower(trim(" + i + "." + ClientiIndirizzi.INDIRIZZO + ")) = ? and lower(trim(coalesce("
					+ i + "." + ClientiIndirizzi.CIVICO + ", ''))) = '" + civico.replace("'", "''") + "' and lower(trim(" + i + "."
					+ ClientiIndirizzi.CITTA + ")) = '" + citta.replace("'", "''") + "'", via, "stesso indirizzo");
		}
		db.close();
		if (trovati.isEmpty()) return null;
		StringBuilder m = new StringBuilder("Ci sono gia' clienti simili:\n");
		int n = 0;
		for (String t : trovati.values()) {
			if (n++ == 5) {
				m.append("\n...");
				break;
			}
			m.append("\n• ").append(t);
		}
		return m.append("\n\nCreo lo stesso il nuovo cliente?").toString();
	}

	private static String norm(String s) {
		return s == null ? "" : s.trim().toLowerCase(java.util.Locale.ITALIAN);
	}

	/** Clienti trovati dalla query (un parametro, vuoto = niente ricerca) con il motivo: "Rossi Srl · Torino (stesso nome)". */
	private static void cerca(DbInterno db, java.util.Map<Integer, String> trovati, String sql, String valore, String motivo) {
		if (valore.isEmpty()) return;
		for (Object o : db.eseguiSelect(sql, new String[] { valore })) {
			ContentValues c = (ContentValues) o;
			Integer id = c.getAsInteger(Anagrafica.ID_ANAGRAFICA);
			if (id == null) continue;
			String gia = trovati.get(id);
			if (gia == null) {
				String citta = c.getAsString(Anagrafica.CITTA);
				trovati.put(id, c.getAsString(Anagrafica.RAGIONE_SOCIALE) + (citta == null || citta.isEmpty() ? "" : " · " + citta) + " (" + motivo + ")");
			} else if (!gia.contains(motivo)) {
				trovati.put(id, gia.substring(0, gia.length() - 1) + ", " + motivo + ")");
			}
		}
	}

	@Override
	public void primaDiSalvare(ContentValues val, boolean nuovo) {
		// sul cliente resta anche la copia "via civico" della sede
		if (nuovo) val.put(Anagrafica.INDIRIZZO, (ospite.getForm().valore(VIA) + " " + ospite.getForm().valore(CIVICO)).trim());
	}

	@Override
	public void dopoSalvataggio(DbInterno db, ContentValues val, boolean nuovo) {
		if (!nuovo) return;
		Form f = ospite.getForm();
		String via = f.valore(VIA), civico = f.valore(CIVICO), cap = f.valore(Anagrafica.CAP), citta = f.valore(Anagrafica.CITTA),
				prov = f.valore(Anagrafica.PROVINCIA);
		if ((via + civico + cap + citta + prov).trim().isEmpty()) return;
		// va al server nello stesso invio; SedePrincipaleListener allinea poi il cliente alla sede
		ClientiIndirizzi tabInd = new ClientiIndirizzi();
		ContentValues sede = tabInd.getValoriLogInserimento(db);
		sede.put(ClientiIndirizzi.ID_ANAGRAFICA, val.getAsInteger(Anagrafica.ID_ANAGRAFICA));
		sede.put(ClientiIndirizzi.TIPO, ClientiIndirizzi.TIPI[0][0]);
		sede.put(ClientiIndirizzi.INDIRIZZO, via);
		sede.put(ClientiIndirizzi.CIVICO, civico);
		sede.put(ClientiIndirizzi.CAP, cap);
		sede.put(ClientiIndirizzi.CITTA, citta);
		sede.put(ClientiIndirizzi.PROVINCIA, prov);
		sede.put(ClientiIndirizzi.NAZIONE, "IT");
		sede.put(ClientiIndirizzi.PRINCIPALE, 1);
		sede.put(AbstractTable.ATTIVO, 1);
		tabInd.inserisciRecord(db, sede);
	}
}
