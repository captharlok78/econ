package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import pfa.app.econtab.GestAbbonamentoActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.server.RecordEliminati;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;

public abstract class AbstractTable {
	// ESEMPIO QUERY PER ESTRARRE LE COSTANTI DEI NOMI DEI CAMPI
	/**
	 * select 'public static final String '+Upper(sys.columns.name)+'="'+sys.columns.name+'";' , case when
	 * sys.columns.is_identity=1 then 'aggiungiCampoAutoincremento(' + Upper(sys.columns.name) + ');' when
	 * sys.systypes.name='date' then 'aggiungiCampo(' + Upper(sys.columns.name) + ',INTEGER);' when
	 * sys.systypes.name='int' then 'aggiungiCampo(' + Upper(sys.columns.name) + ',INTEGER);' when
	 * sys.systypes.name='bit' then 'aggiungiCampo(' + Upper(sys.columns.name) + ',INTEGER);' when
	 * sys.systypes.name='varchar' then 'aggiungiCampo(' + Upper(sys.columns.name) + ',TEXT);' when
	 * sys.systypes.name='char' then 'aggiungiCampo(' + Upper(sys.columns.name) + ',TEXT);' when
	 * sys.systypes.name='decimal' then 'aggiungiCampo(' + Upper(sys.columns.name) + ',NUMERIC);' end from sys.columns
	 * inner join sys.systypes on sys.columns.system_type_id = sys.systypes.xtype where sys.columns.object_id = (select
	 * object_id from sys.tables where tables.name='anagrafica') order by sys.columns.column_id
	 * 
	 */

	// ESEMPIO QUERY PER LE CHIAVI
	/**
	 * SELECT 'aggiungiCampoChiave('+Upper(COLUMN_NAME)+');' FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE
	 * OBJECTPROPERTY(OBJECT_ID(constraint_name), 'IsPrimaryKey') = 1 AND table_name = 'anagrafica'
	 * 
	 */

	public static final String INTEGER = "INTEGER";
	public static final String TEXT = "TEXT";
	public static final String NUMERIC = "NUMERIC";
	public static final String DATE = "DATE";

	public static final String ID_OPERATORE_INS = "id_operatore_ins";
	public static final String DATA_INS = "data_ins";
	public static final String ID_OPERATORE_MOD = "id_operatore_mod";
	public static final String DATA_MOD = "data_mod";
	public static final String IN_SERVER = "in_server";

	public static final String ID_DITTA = "id_ditta";

	public static final int VALORE_PRIMO_ID = -1;

	private String nomeTabella = "";
	private String campoNumeratore = "";
	private HashMap<String, String> campi = null;
	private ArrayList<String> campiChiavi = null;
	private ArrayList<String> campiAutoincremento = null;
	private ArrayList<String> campiOrdinati = null;

	public final String getNomeCampoTabella(String nomecampo) {
		return nomeTabella + "." + nomecampo;
	}

	public final String getAlias(String nomeCampo) {
		return nomeTabella + "_" + nomeCampo;
	}

	public String getNomeTabella() {
		return nomeTabella;
	}

	public void setNomeTabella(String nomeTabella) {
		this.nomeTabella = nomeTabella;
	}

	/**
	 * Aggiunge un campo alla tabella
	 * 
	 * @param nomecampo
	 * @param tipo
	 */
	public final void aggiungiCampo(String nomecampo, String tipo) {
		aggiungiCampo(nomecampo, tipo, false);

	}

	/**
	 * Aggiunge un nuovo campo di tipo autoincremento Il tipo � sempre INTEGER
	 * 
	 * @param nomecampo
	 */
	public final void aggiungiCampoAutoincremento(String nomecampo) {
		aggiungiCampo(nomecampo, INTEGER, true);
	}

	private void aggiungiCampo(String nomecampo, String tipo, boolean autoincremento) {
		if (campi == null) {
			campi = new HashMap<String, String>();
		}
		if (campiOrdinati == null) {
			campiOrdinati = new ArrayList<String>();
		}
		campiOrdinati.add(nomecampo);
		campi.put(nomecampo, tipo);
		if (autoincremento == true) {
			if (campiAutoincremento == null) {
				campiAutoincremento = new ArrayList<String>();
			}
			campiAutoincremento.add(nomecampo);
		}
	}

	/**
	 * Aggiunge un campo come chiave
	 * 
	 * @param nomecampo
	 * 
	 */
	public final void aggiungiCampoChiave(String nomecampo) {
		if (campiChiavi == null) {
			campiChiavi = new ArrayList<String>();
		}
		campiChiavi.add(nomecampo);
	}

	/**
	 * 
	 * @return la stringa di creazione tabella SQL
	 */
	public final String getSQL_create() {
		StringBuffer sql = new StringBuffer();
		sql.append("CREATE TABLE " + nomeTabella);
		sql.append(" (");
		Iterator<String> iter = campi.keySet().iterator();
		boolean primo = true;
		while (iter.hasNext()) {
			String campo = iter.next();
			String tipo = campi.get(campo);
			if (tipo.equals(DATE)) {
				tipo = "NUMERIC";
			}
            if (tipo.equals(TEXT) && isChiave(campo)){
                tipo = tipo + " COLLATE NOCASE";
            }
			if (primo) {
				primo = false;
				sql.append(campo + " " + tipo);
			} else {
				sql.append("," + campo + " " + tipo);
			}

			if (isAutoincremento(campo)) {
				sql.append(" autoincrement");
			}

		}

		if (campiChiavi != null) {
			boolean primaChiave = true;
			sql.append(", PRIMARY KEY (");
			for (int i = 0; i < campiChiavi.size(); i++) {
				if (primaChiave) {
					sql.append(campiChiavi.get(i));
					primaChiave = false;
				} else {
					sql.append("," + campiChiavi.get(i));
				}
			}
			sql.append(")");
		}
		sql.append(")");

		return sql.toString();
	}

	/**
	 * Ritorna true se il campo � un autoincrementale
	 * 
	 * @param campo
	 * @return
	 */
	public final boolean isAutoincremento(String campo) {
		if (campiAutoincremento == null) {
			return false;
		}
		return campiAutoincremento.contains(campo);
	}

	/**
	 * Ritorna true se il campo fa parte della chiave
	 * 
	 * @param campo
	 * @return
	 */
	public final boolean isChiave(String campo) {
		if (campiChiavi == null) {
			return false;
		}
		return campiChiavi.contains(campo);
	}

	/**
	 * Metodo utilizzato per inserire i valori in tabella alla creazione del db
	 * 
	 * @return i valori per l'inserimneto di dati in tabella
	 */
	public ContentValues getValoriInsertDefault() {
		ContentValues val = new ContentValues();
		Iterator<String> iter = campi.keySet().iterator();
		while (iter.hasNext()) {
			String campo = iter.next();
			String tipo = campi.get(campo);
			if (!isAutoincremento(campo)) {
				if (tipo.equals(INTEGER)) {
					val.put(campo, 0);
				}
				if (tipo.equals(TEXT)) {
					val.put(campo, "");
				}
				if (tipo.equals(NUMERIC)) {
					val.put(campo, 0);
				}
				if (campo.equals(ID_OPERATORE_INS)) {
					val.put(campo, VALORE_PRIMO_ID);
				}
				if (campo.equals(DATA_INS)) {
					val.put(campo, Utility.dataToNumber(Calendar.getInstance()));
				}

			}
		}
		return val;
	}

	public ContentValues getValoriLogInserimento(DbInterno db) {
		ContentValues val = getValoriInsertDefault();
		val.put(ID_OPERATORE_INS, Sessione.getIdOperatore(db.getContext()));
		val.put(DATA_INS, Utility.dataToNumber(Calendar.getInstance()));
		if (getCampoNumeratore().length() > 0) {
			val.put(getCampoNumeratore(), db.getProssimoId(this));
		}
		if (campi.containsKey(ID_DITTA) && !nomeTabella.equals(Ditte.NOME_TABELLA)) {
			val.put(ID_DITTA, Sessione.getDittaSelezionata());
		}
		return val;
	}

	public ContentValues getValoriLogModifica(DbInterno db) {
		ContentValues val = new ContentValues();
		val.put(ID_OPERATORE_MOD, Sessione.getIdOperatore(db.getContext()));
		val.put(DATA_MOD, Utility.dataToNumber(Calendar.getInstance()));
		return val;
	}

	public String getTipoCampo(String nomecampo) {
		return campi.get(nomecampo);
	}

	public ArrayList<String> getNomiCampi() {
		return campiOrdinati;
	}

	public ArrayList<String> getCampiChiave() {
		return campiChiavi;
	}

	public String getCampoNumeratore() {
		return campoNumeratore;
	}

	public void setCampoNumeratore(String campoNumeratore) {
		this.campoNumeratore = campoNumeratore;
	}

	public final ContentValues getFiltroPerChiave(ContentValues val) {
		ContentValues where = new ContentValues();

		for (int i = 0; i < campiChiavi.size(); i++) {
			if (getTipoCampo(campiChiavi.get(i)).equals(AbstractTable.INTEGER)) {
				where.put(campiChiavi.get(i), val.getAsInteger(campiChiavi.get(i)));
			}
			if (getTipoCampo(campiChiavi.get(i)).equals(AbstractTable.TEXT)) {
				where.put(campiChiavi.get(i), val.getAsString(campiChiavi.get(i)));
			}
		}
		return where;
	}

	public final String getFiltro(ContentValues val) {
		String where = "";
		boolean first = true;
		if (val != null) {
			Iterator<String> iter = val.keySet().iterator();
			while (iter.hasNext()) {
				String nomecampo = iter.next();
				if (first) {
					if (getTipoCampo(nomecampo).equals(AbstractTable.INTEGER)) {
						where = where + getNomeCampoTabella(nomecampo) + " = " + val.getAsInteger(nomecampo);
					}
					if (getTipoCampo(nomecampo).equals(AbstractTable.TEXT)) {
						where = where + getNomeCampoTabella(nomecampo) + " = '" + val.getAsString(nomecampo) + "'";
					}
					first = false;
				} else {
					if (getTipoCampo(nomecampo).equals(AbstractTable.INTEGER)) {
						where = where + " AND " + getNomeCampoTabella(nomecampo) + " = " + val.getAsInteger(nomecampo);
					}
					if (getTipoCampo(nomecampo).equals(AbstractTable.TEXT)) {
						where = where + " AND " + getNomeCampoTabella(nomecampo) + " = '" + val.getAsString(nomecampo) + "'";
					}
				}
			}
		}

		return where;
	}

	/**
	 * Da sovrascrivere per controlli prima di cnacellare
	 * 
	 * @param db
	 * @param val
	 * @return
	 */
	public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {
		return true;
	}

	public Class getDettaglioActivity() {
		return null;
	}

	/** False per le tabelle gestite dal server e solo scaricate (es. unita' di misura): l'app non puo' crearne record. */
	public boolean isCreabileDaApp() {
		return true;
	}

	/**
	 * Ritorna la query personalizzata per i record da mostrare negli spinner di EConTab
	 * 
	 * @return
	 */
	public String getSQLPerSpinner(ContentValues filtro) {
		return "";
	}

	/**
	 * Da sovrascrivere
	 * 
	 * @return
	 */
	public String getCampoCodicePerSpinner() {
		return campiChiavi.get(0);
	}

	/**
	 * Da sovrascrivere
	 * 
	 * @return
	 */
	public String getCampoDescrizionePerSpinner() {
		return campiChiavi.get(0);
	}

	/**
	 * Funzione che serve per eliminare i record dalle tabelle che puntano al record da eliminare
	 * 
	 * @param db
	 * @param val
	 */
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {

	}

	/**
	 * Funzione che serve per eseguire operazioni accessorie all'inserimento
	 * 
	 * @param db
	 * @param val
	 */
	protected void inserimentoCorrelati(DbInterno db, ContentValues val) {

	}

	/**
	 * Funzione che serve per eseguire operazioni accessorie all'aggiornamento
	 * 
	 * @param db
	 * @param val
	 * @param where
	 */
	protected void aggiornamentoCorrelati(DbInterno db, ContentValues val, ContentValues where) {

	}

	/**
	 * Elimina il record ed effettua eventuali azioni correlate
	 * 
	 * @param db
	 * @param val
	 * @return
	 */
	public int cancellaRecord(DbInterno db, ContentValues val) {
		return cancellaRecord(db, val, true);
	}

	/**
	 * Elimina il record ed effettua eventuali azioni correlate
	 * 
	 * @param db
	 * @param val
	 * @return
	 */
	public int cancellaRecord(DbInterno db, ContentValues val, boolean perchiave) {
		eliminaCorrelati(db, val);

		// per le installazioni server inserisco le chiavi dei record cancellati
		// in record_eliminati per eliminarli successivamente anche dal server al momento della sincronizzazione
		if (Sessione.isLicenzaBusiness(db.getContext())
				|| pfa.app.econtab.api.TokenManager.getInstance(db.getContext()).hasToken()) {
			_scriviRecordEliminati(db, val, perchiave);
		}
		if (perchiave) {
			return db.delete(getNomeTabella(), getFiltroPerChiave(val));
		} else {
			return db.delete(getNomeTabella(), val);
		}

	}

	/**
	 * per le installazioni server inserisco le chiavi dei record cancellati in record_eliminati per eliminarli
	 * successivamente anche dal server al momento della sincronizzazione
	 * 
	 * @param db
	 * @param val
	 * @param perchiave
	 */
	private void _scriviRecordEliminati(DbInterno db, ContentValues val, boolean perchiave) {
		// TODO Auto-generated method stub
		ContentValues whereChiave = getFiltroPerChiave(val);
		ArrayList<Object> recordEliminati = null;
		if (perchiave) {
			recordEliminati = db.eseguiSelect(this, whereChiave, null);
		} else {
			recordEliminati = db.eseguiSelect(this, val, null);
		}

		RecordEliminati tabRecordEliminati = new RecordEliminati();
		for (int i = 0; i < recordEliminati.size(); i++) {
			ContentValues curr = (ContentValues) recordEliminati.get(i);
			// inserisco il record solo se se � un record gi� presente
			// sul server
			if (curr.getAsInteger(IN_SERVER) == 1) {
				String valorechiave = "";
				for (int k = 0; k < campiChiavi.size(); k++) {
					if (getTipoCampo(campiChiavi.get(k)).equals(AbstractTable.INTEGER)) {
						valorechiave = valorechiave + curr.getAsInteger(campiChiavi.get(k)) + "|";
					}
					if (getTipoCampo(campiChiavi.get(k)).equals(AbstractTable.TEXT)) {
						valorechiave = valorechiave + curr.getAsString(campiChiavi.get(k)) + "|";
					}
				}
				valorechiave = valorechiave.substring(0, valorechiave.length() - 1);
				// se va in errore significa che esiste gi�
				try {
					ContentValues valIns = tabRecordEliminati.getValoriLogInserimento(db);
					valIns.put(RecordEliminati.TABELLA, getNomeTabella());
					valIns.put(RecordEliminati.CHIAVE_RECORD, valorechiave);
					tabRecordEliminati.inserisciRecord(db, valIns);
				} catch (Exception e) {

				}

			}
		}
	}

	/**
	 * Inserisci il record ed effettua eventuali azioni correlate
	 * 
	 * @param db
	 * @param val
	 * @return
	 */
	public long inserisciRecord(DbInterno db, ContentValues val) {
		//System.out.println("EConTab: AbstractTable inserisciRecord val " + val);
		Iterator<String> iter = val.keySet().iterator();
		while (iter.hasNext()) {
			String nomecampo = iter.next();
			//System.out.println("EConTab: AbstractTable inserisciRecord nomecampo " + nomecampo);
			if (getTipoCampo(nomecampo).equals(INTEGER))
			{
				//System.out.println("EConTab: AbstractTable inserisciRecord INTEGER, val " + val.getAsString(nomecampo));
				if (val.getAsString(nomecampo) == null || val.getAsString(nomecampo).equals(""))
				{
					//System.out.println("EConTab: AbstractTable inserisciRecord val nomecampo 0");
					val.put(nomecampo, "0");
				}
			}
			if (getTipoCampo(nomecampo).equals(NUMERIC))
			{
				//System.out.println("EConTab: AbstractTable inserisciRecord NUMERIC, val " + val.getAsString(nomecampo));
				if (val.getAsString(nomecampo) == null || val.getAsString(nomecampo).equals(""))
				{
					//System.out.println("EConTab: AbstractTable inserisciRecord val nomecampo 0.0");
					val.put(nomecampo, "0.0");
				}
			}
		}
		/**
		 * per non modificare in giro per il programma inserisco qui l'id preventivo relativo alla tabella ComponentiCantiere
		 * prendendolo dalla sezione
		 */
		//System.out.println("EConTab: AbstractTable inserisciRecord ComponentiCantiere getNomeTabella() " + getNomeTabella());
		if (getNomeTabella().equals(ComponentiCantiere.NOME_TABELLA) ){
			//System.out.println("EConTab: AbstractTable inserisciRecord ComponentiCantiere Sessione.getIdPreventivoSelezionato())" + Sessione.getIdPreventivoSelezionato());
			val.put(ComponentiCantiere.ID_PREVENTIVO,Sessione.getIdPreventivoSelezionato());
			if (Sessione.getIdPreventivoSelezionato()!=0){
				val.put(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI,0);
			}
			else {
				val.put(ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI,1);
			}
		}
		if (controllaMaxInserimentiLicenza(db)) {
			//System.out.println("EConTab: AbstractTable inserisciRecord ComponentiCantiere insert...");
			long result = db.insert(getNomeTabella(), val);
			inserimentoCorrelati(db, val);
			//System.out.println("EConTab: AbstractTable inserisciRecord ComponentiCantiere result " + result);
			return result;
		}
		return -1;

	}


	public boolean controllaMaxInserimentiLicenza(final DbInterno db) {
		//System.out.println("EConTab: AbstractTable controllaMaxInserimentiLicenza ENTER");
		// TODO Auto-generated method stub
		if (Sessione.isLicenzaGratis(db.getContext())) {
			if (getMassimoNumeroRecordLicenzaGratis() > 0) {
				int numeroRecord = db.eseguiCount(this, null);
				if (numeroRecord >= getMassimoNumeroRecordLicenzaGratis()) {
					/*Toast toastLicenza = Toast.makeText(db.getContext(), db.getResources()
							.getString(R.string.messaggio_licenza_inserimenti).toUpperCase(Locale.getDefault()), Toast.LENGTH_LONG);
					toastLicenza.setGravity(Gravity.CENTER, 0, 0);

					toastLicenza.show();*/

                    Utility.mostraConfermaDialog("Upgrade EConTab",db.getContext().getString(R.string.messaggio_licenza_inserimenti),db.getContext(),db.getContext().getString(R.string.abbonati),db.getContext().getString(R.string.annulla),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i==DialogInterface.BUTTON_POSITIVE){
                                Intent intent = new Intent(db.getContext(), GestAbbonamentoActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                db.getContext().startActivity(intent);
                            }
                        }
                    });
					//System.out.println("EConTab: AbstractTable controllaMaxInserimentiLicenza EXIT (1)");
					// Utility.mostraDialog(db.getResources().getString(R.string.attenzione),
					// db.getResources().getString(R.string.messaggio_licenza_inserimenti), db.getContext(), "OK");
					return false;
				}
			}
		}
		//System.out.println("EConTab: AbstractTable controllaMaxInserimentiLicenza EXIT (2)");
		return true;

	}

	/**
	 * Per la licenza gratis ritorna il massimo numero di record inseribili nella tabella
	 * 
	 * @return
	 */
	protected int getMassimoNumeroRecordLicenzaGratis() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Aggiorna il record ed effetua eventuali azioni correlate
	 * 
	 * @param db
	 * @param val
	 * @param where
	 * @return
	 */
	public int aggiornaRecord(DbInterno db, ContentValues val, ContentValues where) {
		// per sicurezza reimposto i valori di log (dovrebbero gi� esserci in val comunque)
		val.put(ID_OPERATORE_MOD, Sessione.getIdOperatore(db.getContext()));
		val.put(DATA_MOD, Utility.dataToNumber(Calendar.getInstance()));
		// marca il record come da inviare al server (upload Mercury)
		if (campi != null && campi.containsKey(IN_SERVER)) {
			val.put(IN_SERVER, 0);
		}

		int result = db.update(getNomeTabella(), val, where);
		aggiornamentoCorrelati(db, val, where);
		return result;

	}

	public String costruisciInsertStatement() {
		// TODO Auto-generated method stub
		String SQLINS = "insert into  " + getNomeTabella();
		String c = "";
		String v = "";

		Iterator<String> iter = campi.keySet().iterator();
		while (iter.hasNext()) {
			String campo = iter.next();

			if (!isAutoincremento(campo)) {
				c = c + campo + ",";
				v = v + "?" + ",";
			}
		}
		c = " (" + c.substring(0, c.length() - 1) + ") ";
		v = " values (" + v.substring(0, v.length() - 1) + ") ";

		SQLINS = SQLINS + c + v;

		return SQLINS;
	}

	public String costruisciUpdateStatement() {
		// TODO Auto-generated method stub
		String SQLUPD = "update " + getNomeTabella() + " set ";
		String c = "";
		String w = " where ";

		Iterator<String> iter = campi.keySet().iterator();
		while (iter.hasNext()) {
			String campo = iter.next();

			if (!isChiave(campo)) {
				c = c + campo + "=?,";
			} else {
				w = w + campo + "=? and ";
			}
		}
		c = c.substring(0, c.length() - 1);
		w = w.substring(0, w.length() - 4);

		SQLUPD = SQLUPD + c + w;

		return SQLUPD;
	}

	/**
	 * Esegue la copia del record di partenza
	 * 
	 * @param db
	 * @param recOrigine
	 * @param valoriDaSovrascrivere
	 *            opzionale
	 * @return
	 */
	public ContentValues copiaRecord(DbInterno db, ContentValues recOrigine, ContentValues valoriDaSovrascrivere) {
		// TODO Auto-generated method stub
		ContentValues recCopia = getValoriLogInserimento(db);
		Iterator<String> iter = recOrigine.keySet().iterator();
		while (iter.hasNext()) {
			String campo = iter.next();
			if (campi.containsKey(campo)) {
				if (!isChiave(campo) && !isCampoDiLog(campo)) {
					if (getTipoCampo(campo).equals(INTEGER)) {
						recCopia.put(campo, recOrigine.getAsInteger(campo));
					}
					if (getTipoCampo(campo).equals(NUMERIC)) {
						recCopia.put(campo, recOrigine.getAsDouble(campo));
					}
					if (getTipoCampo(campo).equals(TEXT)) {
						recCopia.put(campo, recOrigine.getAsString(campo));
					}
					if (getTipoCampo(campo).equals(DATE)) {
						recCopia.put(campo, recOrigine.getAsLong(campo));
					}

				}
			}
		}

		if (valoriDaSovrascrivere != null) {
			iter = valoriDaSovrascrivere.keySet().iterator();
			while (iter.hasNext()) {
				String campo = iter.next();
				if (campi.containsKey(campo)) {
					if (getTipoCampo(campo).equals(INTEGER)) {
						recCopia.put(campo, valoriDaSovrascrivere.getAsInteger(campo));
					}
					if (getTipoCampo(campo).equals(NUMERIC)) {
						recCopia.put(campo, valoriDaSovrascrivere.getAsDouble(campo));
					}
					if (getTipoCampo(campo).equals(TEXT)) {
						recCopia.put(campo, valoriDaSovrascrivere.getAsString(campo));
					}
					if (getTipoCampo(campo).equals(DATE)) {
						recCopia.put(campo, valoriDaSovrascrivere.getAsLong(campo));
					}
				}
			}
		}

		// Per la copia faccio l'insert secco (senza i correlati) perch� la copia non
		// implica tutti gli automatismi della funzione di inserimento (ES. composizioni...)

		db.insert(getNomeTabella(), recCopia);
		// inserisciRecord(db, recCopia);

		return recCopia;
	}

	private boolean isCampoDiLog(String campo) {
		// TODO Auto-generated method stub
		if (campo.equals(ID_OPERATORE_INS) || campo.equals(ID_OPERATORE_MOD) || campo.equals(DATA_INS) || campo.equals(DATA_MOD)
				|| campo.equals(IN_SERVER)) {
			return true;
		}
		return false;
	}

	public void costruisciInsertParametri(SQLiteStatement st, JSONObject jsonObject, Context ctx) throws JSONException {
		// TODO Auto-generated method stub

		int index = 1;

		Iterator<String> iter = campi.keySet().iterator();
		while (iter.hasNext()) {
			String campo = iter.next();
			String tipo = campi.get(campo);
			if (!isAutoincremento(campo)) {
				if (jsonObject.has(campo)) {
					String valore = jsonObject.getString(campo).trim();
					//System.out.println("EConTab: AbstractTable costruisciInsertParametri HAS campo " + campo + ", tipo " + tipo + ", valore " + valore + ", index " + index);
					if (tipo.equals(TEXT)) {
						st.bindString(index, valore);
					}
					if (tipo.equals(INTEGER)) {
						if (valore.equalsIgnoreCase("true")) {
							st.bindLong(index, 1);
						} else {
							if (valore.equals("") || valore.equals("0") || valore.equalsIgnoreCase("false")) {
								st.bindLong(index, 0);
							} else {
								st.bindLong(index, Long.parseLong(valore));
							}
						}
					}
					if (tipo.equals(DATE)) {
						if (valore.equals("") || valore.equals("0")) {
							st.bindLong(index, 0);
						} else {
							st.bindLong(index, Utility.dataToNumber(valore));
						}
					}
					if (tipo.equals(NUMERIC)) {
						if (valore.equals("") || valore.equals("0") || valore.equals("0,0000")) {
							st.bindDouble(index, 0);
						} else {
							valore = Utility.replaceAll(valore, ",", ".");
							st.bindDouble(index, Double.parseDouble(valore));
						}
					}
				}
				else {
					if (tipo.equals(INTEGER)) {
						st.bindLong(index, 0);
						//System.out.println("EConTab: AbstractTable costruisciInsertParametri HASNT campo " + campo + ", tipo " + tipo + ", valore 0, index " + index);
					}
					if (tipo.equals(TEXT)) {
						st.bindString(index, "");
						//System.out.println("EConTab: AbstractTable costruisciInsertParametri HASNT campo " + campo + ", tipo " + tipo + ", valore , index " + index);
					}
					if (tipo.equals(NUMERIC)) {
						st.bindDouble(index, 0);
						//System.out.println("EConTab: AbstractTable costruisciInsertParametri HASNT campo " + campo + ", tipo " + tipo + ", valore 0, index " + index);
					}
					if (campo.equals(ID_OPERATORE_INS)) {
						st.bindLong(index, Sessione.getIdOperatore(ctx));
						//System.out.println("EConTab: AbstractTable costruisciInsertParametri HASNT campo " + campo + ", tipo " + tipo + ", valore " + Sessione.getIdOperatore(ctx) + ", index " + index);
					}
					if (campo.equals(DATA_INS)) {
						st.bindLong(index, Utility.dataToNumber(Calendar.getInstance()));
						//System.out.println("EConTab: AbstractTable costruisciInsertParametri HASNT campo " + campo + ", tipo " + tipo + ", valore " + Utility.dataToNumber(Calendar.getInstance()) + ", index " + index);
					}
					if (campo.equals(IN_SERVER)) {
						st.bindLong(index, 1);
						//System.out.println("EConTab: AbstractTable costruisciInsertParametri HASNT campo " + campo + ", tipo " + tipo + ", valore 1, index " + index);
					}
				}
				index++;
			}
		}
	}

	public void costruisciUpdateParametri(SQLiteStatement st, JSONObject jsonObject, Context ctx) throws JSONException {
		// TODO Auto-generated method stub

		int index = 1;

		Iterator<String> iter = campi.keySet().iterator();
		while (iter.hasNext()) {
			String campo = iter.next();
			//System.out.println("EConTab: AbstractTable costruisciUpdateParametri campo " + campo);
			String tipo = campi.get(campo);
			if (!isChiave(campo)) {
				if (jsonObject.has(campo)) {
					String valore = jsonObject.getString(campo).trim();
					//System.out.println("EConTab: AbstractTable costruisciUpdateParametri valore " + valore);
					if (tipo.equals(TEXT)) {
						st.bindString(index, valore);
					}
					if (tipo.equals(INTEGER)) {
						if (valore.equalsIgnoreCase("true")) {
							st.bindLong(index, 1);
						} else {
							if (valore.equals("") || valore.equals("0") || valore.equalsIgnoreCase("false")) {
								st.bindLong(index, 0);
							} else {
								st.bindLong(index, Long.parseLong(valore));
							}
						}

					}
					if (tipo.equals(DATE)) {
						if (valore.equals("") || valore.equals("0")) {
							st.bindLong(index, 0);
						} else {
							st.bindLong(index, Utility.dataToNumber(valore));
						}
					}
					if (tipo.equals(NUMERIC)) {
						if (valore.equals("") || valore.equals("0") || valore.equals("0,0000")) {
							st.bindDouble(index, 0);
						} else {
							valore = Utility.replaceAll(valore, ",", ".");
							st.bindDouble(index, Double.parseDouble(valore));
						}
					}
				}

				else {
					if (tipo.equals(INTEGER)) {
						st.bindLong(index, 0);
					}
					if (tipo.equals(TEXT)) {
						st.bindString(index, "");
					}
					if (tipo.equals(NUMERIC)) {
						st.bindDouble(index, 0);
					}
					if (campo.equals(ID_OPERATORE_INS)) {
						st.bindLong(index, Sessione.getIdOperatore(ctx));
					}
					if (campo.equals(DATA_INS)) {
						st.bindLong(index, Utility.dataToNumber(Calendar.getInstance()));
					}
					if (campo.equals(ID_OPERATORE_MOD)) {
						st.bindLong(index, Sessione.getIdOperatore(ctx));
					}
					if (campo.equals(DATA_MOD)) {
						st.bindLong(index, Utility.dataToNumber(Calendar.getInstance()));
					}
					if (campo.equals(IN_SERVER)) {
						st.bindLong(index, 1);
					}
				}
				index++;
			}

		}

		// giro per le chiavi
		iter = campi.keySet().iterator();
		while (iter.hasNext()) {
			String campo = iter.next();
			String tipo = campi.get(campo);
			if (isChiave(campo)) {
				if (jsonObject.has(campo)) {
					String valore = jsonObject.getString(campo).trim();
					if (tipo.equals(TEXT)) {
						st.bindString(index, valore);
					}
					if (tipo.equals(INTEGER)) {
						st.bindLong(index, Long.parseLong(valore));
					}

				} else {
					if (tipo.equals(INTEGER)) {
						st.bindLong(index, 0);
					}
					if (tipo.equals(TEXT)) {
						st.bindString(index, "");
					}

				}
				index++;
			}
		}
	}

	public String formattaDescrizioneSpinner(ContentValues val) {
		return val.getAsString(getCampoDescrizionePerSpinner());
	}

	/**
	 * Per le tabelle con id come chiave effettua il getRecord
	 * 
	 * @param db
	 * @param valoreChiave
	 * @return
	 * @throws Exception
	 */
	public ContentValues getRecordPerChiave(DbInterno db, Object valoreChiave) {
		if (campiChiavi.size() > 1) {
			Log.e("Errore chiamata getRecordPerChiave", "Funzione getRecordPerChiave non possibile in una tabella " + getNomeTabella()
					+ " con pi� di un campo chiave.");
			return null;
		}
		ContentValues where = new ContentValues();
		String nomeCampo = campiChiavi.get(0);
		if (getTipoCampo(nomeCampo).equals(INTEGER)) {
			where.put(campiChiavi.get(0), (Integer) valoreChiave);
		}
		if (getTipoCampo(nomeCampo).equals(TEXT)) {
			where.put(campiChiavi.get(0), (String) valoreChiave);
		}

		return db.getRecord(this, where);
	}

	public static AbstractTable getIstanzaTabella(String tabella) {

		// TODO Auto-generated method stub
		if (tabella.equalsIgnoreCase(Anagrafica.NOME_TABELLA)) {
			return new Anagrafica();
		}
		if (tabella.equalsIgnoreCase(Aree.NOME_TABELLA)) {
			return new Aree();
		}
		if (tabella.equalsIgnoreCase(Cantieri.NOME_TABELLA)) {
			return new Cantieri();
		}
		if (tabella.equalsIgnoreCase(CategorieComponenti.NOME_TABELLA)) {
			return new CategorieComponenti();
		}
		if (tabella.equalsIgnoreCase(CategorieGenerali.NOME_TABELLA)) {
			return new CategorieGenerali();
		}
		if (tabella.equalsIgnoreCase(Collegamenti.NOME_TABELLA)) {
			return new Collegamenti();
		}
		if (tabella.equalsIgnoreCase(Componenti.NOME_TABELLA)) {
			return new Componenti();
		}
		if (tabella.equalsIgnoreCase(ComponentiCantComposti.NOME_TABELLA)) {
			return new ComponentiCantComposti();
		}
		if (tabella.equalsIgnoreCase(ComponentiCantiere.NOME_TABELLA)) {
			return new ComponentiCantiere();
		}
		if (tabella.equalsIgnoreCase(ComponentiComposti.NOME_TABELLA)) {
			return new ComponentiComposti();
		}
		if (tabella.equalsIgnoreCase(Composizioni.NOME_TABELLA)) {
			return new Composizioni();
		}
		if (tabella.equalsIgnoreCase(ComposizioniCantiere.NOME_TABELLA)) {
			return new ComposizioniCantiere();
		}
		if (tabella.equalsIgnoreCase(Costruttori.NOME_TABELLA)) {
			return new Costruttori();
		}
		if (tabella.equalsIgnoreCase(Ditte.NOME_TABELLA)) {
			return new Ditte();
		}
		if (tabella.equalsIgnoreCase(Elementi.NOME_TABELLA)) {
			return new Elementi();
		}
		if (tabella.equalsIgnoreCase(ElementiCantiere.NOME_TABELLA)) {
			return new ElementiCantiere();
		}
		if (tabella.equalsIgnoreCase(ElementiCodici.NOME_TABELLA)) {
			return new ElementiCodici();
		}
		if (tabella.equalsIgnoreCase(Foto.NOME_TABELLA)) {
			return new Foto();
		}
		if (tabella.equalsIgnoreCase(FotoElementi.NOME_TABELLA)) {
			return new FotoElementi();
		}
		if (tabella.equalsIgnoreCase(Iva.NOME_TABELLA)) {
			return new Iva();
		}
		if (tabella.equalsIgnoreCase(Linee.NOME_TABELLA)) {
			return new Linee();
		}
		if (tabella.equalsIgnoreCase(Listini.NOME_TABELLA)) {
			return new Listini();
		}
		if (tabella.equalsIgnoreCase(LivelliUtente.NOME_TABELLA)) {
			return new LivelliUtente();
		}
		if (tabella.equalsIgnoreCase(Locali.NOME_TABELLA)) {
			return new Locali();
		}
		if (tabella.equalsIgnoreCase(LocaliPorteFinestre.NOME_TABELLA)) {
			return new LocaliPorteFinestre();
		}
		if (tabella.equalsIgnoreCase(Manodopera.NOME_TABELLA)) {
			return new Manodopera();
		}
		if (tabella.equalsIgnoreCase(Placche.NOME_TABELLA)) {
			return new Placche();
		}
		if (tabella.equalsIgnoreCase(PlaccheModuli.NOME_TABELLA)) {
			return new PlaccheModuli();
		}
		if (tabella.equalsIgnoreCase(Preventivi.NOME_TABELLA)) {
			return new Preventivi();
		}
		if (tabella.equalsIgnoreCase(PreventiviDettaglio.NOME_TABELLA)) {
			return new PreventiviDettaglio();
		}
        if (tabella.equalsIgnoreCase(Relazioni.NOME_TABELLA)) {
            return new Relazioni();
        }
        if (tabella.equalsIgnoreCase(Rapportini.NOME_TABELLA)) {
            return new Rapportini();
        }
        if (tabella.equalsIgnoreCase(RapportiniDettaglio.NOME_TABELLA)) {
            return new RapportiniDettaglio();
        }
		if (tabella.equalsIgnoreCase(Unita.NOME_TABELLA)) {
			return new Unita();
		}
		if (tabella.equalsIgnoreCase(UnitaMisura.NOME_TABELLA)) {
			return new UnitaMisura();
		}
		if (tabella.equalsIgnoreCase(Utenti.NOME_TABELLA)) {
			return new Utenti();
		}
		if (tabella.equalsIgnoreCase(UtentiDitta.NOME_TABELLA)) {
			return new UtentiDitta();
		}
		if (tabella.equalsIgnoreCase(AssCodiciLinee.NOME_TABELLA)) {
			return new AssCodiciLinee();
		}

		return null;
	}

	public ArrayList<JSONObject> getRecordNuovi(DbInterno db,boolean primaSincronizzazione) throws JSONException {
		// TODO Auto-generated method stub
		ArrayList<Object> nuovi = null;
        if (primaSincronizzazione){
            nuovi = db.eseguiSelect("Select * from " + getNomeTabella(), null);
        }
        else{
            nuovi = db.eseguiSelect("Select * from " + getNomeTabella() + " where " + IN_SERVER + "=0", null);
        }

		// if (getCampoNumeratore().equals("")) {
		// // String dataUltimaSinc = Sessione.getDataUltimaSincronizzazione(db.getContext());
		// nuovi = db.eseguiSelect("Select * from " + getNomeTabella() + " where " + IN_SERVER + "=0", null);
		// } else {
		// nuovi = db.eseguiSelect("Select * from " + getNomeTabella() + " where " + getCampoNumeratore() + "<0 ",
		// null);
		// }
		return Utility.toJSONArrayList(nuovi);
	}


    public ArrayList<JSONObject> getAllRecord(DbInterno db) throws JSONException {
        // TODO Auto-generated method stub
        ArrayList<Object> nuovi = null;
        nuovi = db.eseguiSelect("Select * from " + getNomeTabella(), null);

        return Utility.toJSONArrayList(nuovi);
    }

	/**
	 * Prendo tutti i record con data modifica superiore alla data ultima sincronizzazione ma data data inserimento
	 * minore per evitare di mandare due volte al server i record con data nuovi e modificati
	 * 
	 * @param db
	 * @return
	 * @throws JSONException
	 */
	public ArrayList<JSONObject> getRecordModificati(DbInterno db) throws JSONException {
		// TODO Auto-generated method stub
		ArrayList<Object> modificati = null;

		String dataUltimaSinc = Sessione.getDataUltimaSincronizzazione(db.getContext());
		modificati = db.eseguiSelect("Select * from " + getNomeTabella() + " where " + DATA_MOD + ">" + dataUltimaSinc + " and " + DATA_INS
				+ "<" + dataUltimaSinc, null);

		return Utility.toJSONArrayList(modificati);
	}

	/**
	 * Per ogni tabella restituisce le chiavi dei record cancellati leggendoli dalla tabella record_eliminati
	 * 
	 * @param db
	 * @return
	 * @throws JSONException
	 */
	public ArrayList<JSONObject> getRecordEliminati(DbInterno db) throws JSONException {
		// TODO Auto-generated method stub
		ArrayList<Object> eliminati = null;

		String dataUltimaSinc = Sessione.getDataUltimaSincronizzazione(db.getContext());
		eliminati = db.eseguiSelect("Select " + RecordEliminati.CHIAVE_RECORD + " from " + RecordEliminati.NOME_TABELLA + " where "
				+ RecordEliminati.TABELLA + "='" + getNomeTabella() + "'", null);

		ArrayList<Object> eliminatiCv = new ArrayList<Object>();
		for (int i = 0; i < eliminati.size(); i++) {
			ContentValues curr = (ContentValues) eliminati.get(i);
			ContentValues cvel = new ContentValues();
			String valoreChiave = curr.getAsString(RecordEliminati.CHIAVE_RECORD);
			String[] valoreChiaveSplit = valoreChiave.split("\\|");
			for (int k = 0; k < campiChiavi.size(); k++) {
				cvel.put(campiChiavi.get(k), valoreChiaveSplit[k]);
			}
			eliminatiCv.add(cvel);
		}

		return Utility.toJSONArrayList(eliminatiCv);
	}

}
