package pfa.app.econtab.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Aree;
import pfa.app.econtab.db.table.AssCodiciLinee;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.CategorieComponenti;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.ComponentiCantComposti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComponentiComposti;
import pfa.app.econtab.db.table.Composizioni;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Ditte;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.ElementiCodici;
import pfa.app.econtab.db.table.Foto;
import pfa.app.econtab.db.table.FotoElementi;
import pfa.app.econtab.db.table.IconeModificate;
import pfa.app.econtab.db.table.Iva;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Listini;
import pfa.app.econtab.db.table.LivelliUtente;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.LocaliPorteFinestre;
import pfa.app.econtab.db.table.Manodopera;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.db.table.PlaccheModuli;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.db.table.Relazioni;
import pfa.app.econtab.db.table.Unita;
import pfa.app.econtab.db.table.UnitaMisura;
import pfa.app.econtab.db.table.Utenti;
import pfa.app.econtab.db.table.UtentiDitta;
import pfa.app.econtab.server.RecordEliminati;
import pfa.app.econtab.utils.Sessione;

public class DbInterno extends SQLiteOpenHelper {
	public static final String DATABASE_NAME = "ECONTAB.db";
	public static final String DATABASE_NAME_ZIP = ".econtab.db";
	public static final int SCHEMA_VERSION = 22;

	private Context cont = null;

	public DbInterno(Context context) {
		super(context, DATABASE_NAME, null, SCHEMA_VERSION);
		setContext(context);

	}

	public void reset()
	{
		SQLiteDatabase db = getReadableDatabase();

		ArrayList<String> tabelle = getNomiTabelle(db);

		ArrayList<String> tabelleDaLasciare = new ArrayList<String>();
		//tabelleDaLasciare.add(Utenti.NOME_TABELLA);
		//tabelleDaLasciare.add(UtentiDitta.NOME_TABELLA);
		//tabelleDaLasciare.add(LivelliUtente.NOME_TABELLA);
		//tabelleDaLasciare.add(Ditte.NOME_TABELLA);

		for (int i = 0; i < tabelle.size(); i++) {

			if (!tabelleDaLasciare.contains(tabelle.get(i))) {
				db.execSQL("DROP TABLE " + tabelle.get(i));
			}
		}

		db.execSQL(new Anagrafica().getSQL_create());
		db.execSQL(new Aree().getSQL_create());
		db.execSQL(new Cantieri().getSQL_create());
		db.execSQL(new CategorieComponenti().getSQL_create());
		db.execSQL(new CategorieGenerali().getSQL_create());
		db.execSQL(new Componenti().getSQL_create());
		db.execSQL(new ComponentiCantiere().getSQL_create());
		db.execSQL(new Composizioni().getSQL_create());
		db.execSQL(new ComposizioniCantiere().getSQL_create());
		db.execSQL(new Costruttori().getSQL_create());
		db.execSQL(new Elementi().getSQL_create());
		db.execSQL(new ElementiCantiere().getSQL_create());
		db.execSQL(new Foto().getSQL_create());
		db.execSQL(new FotoElementi().getSQL_create());
		db.execSQL(new Iva().getSQL_create());
		db.execSQL(new Linee().getSQL_create());
		db.execSQL(new Locali().getSQL_create());
		db.execSQL(new LocaliPorteFinestre().getSQL_create());
		db.execSQL(new Manodopera().getSQL_create());
		db.execSQL(new Placche().getSQL_create());
		db.execSQL(new PlaccheModuli().getSQL_create());
		db.execSQL(new PreventiviDettaglio().getSQL_create());
		db.execSQL(new Unita().getSQL_create());
		db.execSQL(new UnitaMisura().getSQL_create());
		db.execSQL(new Preventivi().getSQL_create());
		db.execSQL(new Listini().getSQL_create());
		db.execSQL(new ElementiCodici().getSQL_create());
		db.execSQL(new Collegamenti().getSQL_create());
		db.execSQL(new ComponentiComposti().getSQL_create());
		db.execSQL(new ComponentiCantComposti().getSQL_create());
		db.execSQL(new RecordEliminati().getSQL_create());
        db.execSQL(new Relazioni().getSQL_create());
        db.execSQL(new Rapportini().getSQL_create());
        db.execSQL(new RapportiniDettaglio().getSQL_create());

        db.execSQL(new Utenti().getSQL_create());
        db.execSQL(new UtentiDitta().getSQL_create());
        db.execSQL(new LivelliUtente().getSQL_create());
        db.execSQL(new Ditte().getSQL_create());
		db.execSQL(new AssCodiciLinee().getSQL_create());
		db.execSQL(new IconeModificate().getSQL_create());

		db.execSQL("VACUUM");

		Sessione.setDataUltimaSincronizzazione("19700101000000", getContext());
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		// TODO Auto-generated method stub
		/**
		 * Creazione tabella
		 */

		db.execSQL(new Anagrafica().getSQL_create());
		db.execSQL(new Aree().getSQL_create());
		db.execSQL(new Cantieri().getSQL_create());
		db.execSQL(new CategorieComponenti().getSQL_create());
		db.execSQL(new CategorieGenerali().getSQL_create());
		db.execSQL(new Componenti().getSQL_create());
		db.execSQL(new ComponentiCantiere().getSQL_create());
		db.execSQL(new Composizioni().getSQL_create());
		db.execSQL(new ComposizioniCantiere().getSQL_create());
		db.execSQL(new Costruttori().getSQL_create());
		db.execSQL(new Ditte().getSQL_create());
		db.execSQL(new Elementi().getSQL_create());
		db.execSQL(new ElementiCantiere().getSQL_create());
		db.execSQL(new Foto().getSQL_create());
		db.execSQL(new FotoElementi().getSQL_create());
		db.execSQL(new Iva().getSQL_create());
		db.execSQL(new Linee().getSQL_create());
		db.execSQL(new Locali().getSQL_create());
		db.execSQL(new LocaliPorteFinestre().getSQL_create());
		db.execSQL(new Manodopera().getSQL_create());
		db.execSQL(new Placche().getSQL_create());
		db.execSQL(new PlaccheModuli().getSQL_create());
		db.execSQL(new PreventiviDettaglio().getSQL_create());
		db.execSQL(new Unita().getSQL_create());
		db.execSQL(new UnitaMisura().getSQL_create());
		db.execSQL(new Utenti().getSQL_create());
		db.execSQL(new UtentiDitta().getSQL_create());
		db.execSQL(new LivelliUtente().getSQL_create());
		db.execSQL(new Preventivi().getSQL_create());
		db.execSQL(new Listini().getSQL_create());
		db.execSQL(new ElementiCodici().getSQL_create());
		db.execSQL(new Collegamenti().getSQL_create());
		db.execSQL(new ComponentiComposti().getSQL_create());
		db.execSQL(new ComponentiCantComposti().getSQL_create());
        db.execSQL(new RecordEliminati().getSQL_create());
        db.execSQL(new Relazioni().getSQL_create());
        db.execSQL(new Rapportini().getSQL_create());
        db.execSQL(new RapportiniDettaglio().getSQL_create());
		db.execSQL(new AssCodiciLinee().getSQL_create());
		db.execSQL(new IconeModificate().getSQL_create());

		/**
		 * Inserimento dati di default
		 */
		//db.insert(Ditte.NOME_TABELLA, null, ditte.getValoriInsertDefault());

		//db.insert(Utenti.NOME_TABELLA, null, utenti.getValoriInsertDefault());
		//db.insert(UtentiDitta.NOME_TABELLA, null, utentiDitta.getValoriInsertDefault());

		/*
		 * ArrayList<ContentValues> costruttoridefault = costruttori.getCostruttoriDefault(); for (int i = 0; i <
		 * costruttoridefault.size(); i++) { db.insert(Costruttori.NOME_TABELLA, null, costruttoridefault.get(i)); }
		 */

		// ArrayList<ContentValues> categoriedefault =
		// categorie.getCategorieDefault();
		// for (int i=0;i<categoriedefault.size();i++){
		// db.insert(CategorieGenerali.NOME_TABELLA, null,
		// categoriedefault.get(i));
		// }

		/*
		 * ArrayList<ContentValues> udmDefault = udm.getUdmDefault(); for (int i = 0; i < udmDefault.size(); i++) {
		 * db.insert(UnitaMisura.NOME_TABELLA, null, udmDefault.get(i)); }
		 */

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Il primo controllo sulla oldVersion serve per non eseguire le stesse modifiche ad ogni nuova versione di db
		// (andrebbe in errore)
		if (oldVersion < 2) {
			if (oldVersion < newVersion) {
				db.execSQL("Alter table " + Collegamenti.NOME_TABELLA + " add column " + Collegamenti.METRI_CAVO_UNI + " NUMERIC");
				db.execSQL("Alter table " + Collegamenti.NOME_TABELLA + " add column " + Collegamenti.QTA_CAVO + " INTEGER");
			}
		}
		if (oldVersion < 3) {
			if (oldVersion < newVersion) {
				db.execSQL("Alter table " + Locali.NOME_TABELLA + " add column " + Locali.PREFERITO_SN + " INTEGER");
			}
		}
		if (oldVersion < 4) {
			if (oldVersion < newVersion) {
				db.execSQL("Alter table " + Collegamenti.NOME_TABELLA + " add column " + Collegamenti.ID_ORDINE + " INTEGER");
			}
		}

		if (oldVersion < 5) {
			if (oldVersion < newVersion) {
				db.execSQL(new ComponentiComposti().getSQL_create());
				db.execSQL(new ComponentiCantComposti().getSQL_create());
			}
		}

		if (oldVersion < 6) {
			if (oldVersion < newVersion) {
				db.execSQL(new LivelliUtente().getSQL_create());

				db.execSQL("Alter table " + Utenti.NOME_TABELLA + " add column " + Utenti.PASSWORD + " TEXT");
				db.execSQL("Alter table " + Utenti.NOME_TABELLA + " add column " + Utenti.E_MAIL + " TEXT");
				db.execSQL("Alter table " + Utenti.NOME_TABELLA + " add column " + Utenti.TELEFONO + " TEXT");
				db.execSQL("Alter table " + Utenti.NOME_TABELLA + " add column " + Utenti.RICORDA_PASSWORD + " INTEGER");

				// ricreo la tabella UtentiDitta per un errore nel nome dei campi
				db.execSQL("Drop table " + UtentiDitta.NOME_TABELLA);
				db.execSQL(new UtentiDitta().getSQL_create());

				db.insert(UtentiDitta.NOME_TABELLA, null, new UtentiDitta().getValoriInsertDefault());
			}
		}

		if (oldVersion < 7) {
			if (oldVersion < newVersion) {
				db.execSQL(new RecordEliminati().getSQL_create());
			}
		}

		if (oldVersion < 8) {
			if (oldVersion < newVersion) {
				ArrayList<String> tabelle = getNomiTabelle(db);
				for (int i = 0; i < tabelle.size(); i++) {
					if (!tabelle.get(i).equalsIgnoreCase(RecordEliminati.NOME_TABELLA)) {
						db.execSQL("Alter table " + tabelle.get(i) + " add column " + AbstractTable.IN_SERVER + " INTEGER");
					}
				}
			}
		}

		if (oldVersion < 9) {
			if (oldVersion < newVersion) {
				db.execSQL("Alter table " + Collegamenti.NOME_TABELLA + " add column " + Collegamenti.ID_COLLEGAMENTO_TUBO + " INTEGER");
			}
		}

		if (oldVersion < 11) {
			if (oldVersion < newVersion) {

				db.execSQL("ALTER TABLE " + Locali.NOME_TABELLA + " RENAME TO " + Locali.NOME_TABELLA + "_old;");

				// Creating the table on its new format (no redundant columns)
				db.execSQL(new Locali().getSQL_create());

				// Populating the table with the data
				List<String> updatedTableColumns = getTableColumns(db, Locali.NOME_TABELLA);
				List<String> oldTableColumns = getTableColumns(db, Locali.NOME_TABELLA + "_old");
				String columnsSeperated = TextUtils.join(",", updatedTableColumns);
				String columnsSeperatedOld = TextUtils.join(",", oldTableColumns);

				db.execSQL("INSERT INTO " + Locali.NOME_TABELLA + "(" + columnsSeperated + ") SELECT " + columnsSeperatedOld + " FROM "
						+ Locali.NOME_TABELLA + "_old;");
				db.execSQL("DROP TABLE " + Locali.NOME_TABELLA + "_old;");

			}
		}

        if (oldVersion<12){
            if (oldVersion<newVersion){
                db.execSQL(new Relazioni().getSQL_create());
            }
        }
        if (oldVersion<13){
            if (oldVersion<newVersion){
                db.execSQL(new Rapportini().getSQL_create());
                db.execSQL(new RapportiniDettaglio().getSQL_create());
            }
        }
        if (oldVersion<14){
            if (oldVersion<newVersion){
                db.execSQL("Alter table " + Anagrafica.NOME_TABELLA + " add column " + Anagrafica.CODICE_ESTERNO + " TEXT");

            }
        }
		if (oldVersion<15){
			if (oldVersion<newVersion){
				db.execSQL("Alter table " + ElementiCantiere.NOME_TABELLA + " add column " + ElementiCantiere.NON_CONTEGGIARE_PREVENTIVI + " INTEGER");
				db.execSQL("Alter table " + ComponentiCantiere.NOME_TABELLA + " add column " + ComponentiCantiere.NON_CONTEGGIARE_PREVENTIVI + " INTEGER");
			}
		}

		if (oldVersion<16){
			if (oldVersion<newVersion){
				db.execSQL("Alter table " + ComponentiCantiere.NOME_TABELLA + " add column " + ComponentiCantiere.ID_PREVENTIVO + " INTEGER");
			}
		}

		if (oldVersion<17){
			if (oldVersion<newVersion){
				db.execSQL("Alter table " + ElementiCantiere.NOME_TABELLA + " add column " + ElementiCantiere.ID_LINEA + " INTEGER");
				db.execSQL("Alter table " + ElementiCantiere.NOME_TABELLA + " add column " + ElementiCantiere.ID_PLACCA + " INTEGER");
			}
		}
		if (oldVersion<18){
			if (oldVersion<newVersion){
				db.execSQL("Alter table " + PreventiviDettaglio.NOME_TABELLA + " add column " + PreventiviDettaglio.ID_LINEA + " INTEGER");
			}
		}
		if (oldVersion<19){
			if (oldVersion<newVersion){
				db.execSQL(new AssCodiciLinee().getSQL_create());
			}
		}
		if (oldVersion<20){
			if (oldVersion<newVersion){
				db.execSQL(new IconeModificate().getSQL_create());
			}
		}

		if (oldVersion<21){
			if (oldVersion<newVersion){
				// Collega direttamente unita/aree/locali alla ditta (come gia' cantieri/rapportini),
				// cosi' il meccanismo generico di AbstractTable (stampa/filtro automatico per
				// id_ditta) funziona anche per queste tabelle. Backfill dei dati gia' presenti
				// risalendo la gerarchia cantiere->unita->aree->locali, stesso criterio usato
				// lato server (Version20260918120000).
				db.execSQL("Alter table " + Unita.NOME_TABELLA + " add column " + Unita.ID_DITTA + " INTEGER");
				db.execSQL("Alter table " + Aree.NOME_TABELLA + " add column " + Aree.ID_DITTA + " INTEGER");
				db.execSQL("Alter table " + Locali.NOME_TABELLA + " add column " + Locali.ID_DITTA + " INTEGER");

				db.execSQL("UPDATE " + Unita.NOME_TABELLA + " SET " + Unita.ID_DITTA + " = (SELECT "
						+ Cantieri.ID_DITTA + " FROM " + Cantieri.NOME_TABELLA + " WHERE " + Cantieri.NOME_TABELLA + "."
						+ Cantieri.ID_CANTIERE + " = " + Unita.NOME_TABELLA + "." + Unita.ID_CANTIERE + ")");

				db.execSQL("UPDATE " + Aree.NOME_TABELLA + " SET " + Aree.ID_DITTA + " = (SELECT "
						+ Unita.ID_DITTA + " FROM " + Unita.NOME_TABELLA + " WHERE " + Unita.NOME_TABELLA + "."
						+ Unita.ID_UNITA + " = " + Aree.NOME_TABELLA + "." + Aree.ID_UNITA + ")");

				db.execSQL("UPDATE " + Locali.NOME_TABELLA + " SET " + Locali.ID_DITTA + " = (SELECT "
						+ Aree.ID_DITTA + " FROM " + Aree.NOME_TABELLA + " WHERE " + Aree.NOME_TABELLA + "."
						+ Aree.ID_AREA + " = " + Locali.NOME_TABELLA + "." + Locali.ID_AREA + ")");
			}
		}

		if (oldVersion<22){
			if (oldVersion<newVersion){
				// Unita' di misura gestite dal server (tabella unita_misura, codici di 2 lettere maiuscole):
				// manodopera e righe di rapportino ne ricevono una, e i valori liberi gia' presenti vengono
				// ricondotti ai codici (stessa mappatura della migrazione server Version20260921140000).
				db.execSQL("Alter table " + Manodopera.NOME_TABELLA + " add column " + Manodopera.UNITA_MISURA + " TEXT");
				db.execSQL("Alter table " + RapportiniDettaglio.NOME_TABELLA + " add column " + RapportiniDettaglio.UNITA_MISURA + " TEXT");
				String[] tabelleUdm = { Elementi.NOME_TABELLA, Componenti.NOME_TABELLA, ElementiCantiere.NOME_TABELLA,
						ComponentiCantiere.NOME_TABELLA, PreventiviDettaglio.NOME_TABELLA };
				String[][] sinonimi = { { "PZ", "'pz','pezzo','pezzi','oggetto','um','nr','n'" }, { "CM", "'cm'" },
						{ "MT", "'m','mt','metro','metri','ml'" }, { "MQ", "'mq'" }, { "OR", "'h','hh','or','ora','ore'" } };
				for (String tabella : tabelleUdm) {
					for (String[] s : sinonimi) {
						db.execSQL("UPDATE " + tabella + " SET unita_misura = '" + s[0] + "' WHERE lower(trim(unita_misura)) IN (" + s[1] + ")");
					}
				}
			}
		}
	}

	private List<String> getTableColumns(SQLiteDatabase db, String tableName) {
		ArrayList<String> columns = new ArrayList<String>();
		String cmd = "pragma table_info(" + tableName + ");";
		Cursor cur = db.rawQuery(cmd, null);

		while (cur.moveToNext()) {
			columns.add(cur.getString(cur.getColumnIndex("name")));
		}
		cur.close();

		return columns;
	}

	/**
	 * Ritorna il numero di record trovati
	 * 
	 * @param tabella
	 * @param where
	 * @return
	 */
	public int eseguiCount(AbstractTable tabella, ContentValues where) {
		int result = 0;
		String SQL = "Select count(*) as numero from " + tabella.getNomeTabella();
		String[] valori = null;
		if (where != null && where.size() > 0) {
			valori = new String[where.size()];
			SQL = SQL + " where ";
			boolean first = true;
			int cont = 0;
			Iterator<String> iter = where.keySet().iterator();
			while (iter.hasNext()) {
				String nomeCampo = iter.next();
				if (first) {
					SQL = SQL + nomeCampo + "=?";
					first = false;
				} else {
					SQL = SQL + " and " + nomeCampo + "=?";
				}
				valori[cont] = where.getAsString(nomeCampo);
			}
		}

		Cursor c = getReadableDatabase().rawQuery(SQL, valori);

		if (c.getCount() > 0) {
			c.moveToNext();
			result = c.getInt(c.getColumnIndex("numero"));
		}
		c.close();
		return result;
	}

	/** Righe di una pagina + numero totale di risultati (senza paginazione), per {@link #eseguiSelectPaginato}. */
	public static class PaginaRisultati {
		public ArrayList<Object> righe;
		public int totaleRisultati;
	}

	/**
	 * Esegue una query paginata (count totale + pagina corrente) su una selezione libera, anche con join.
	 * Sostituisce la costruzione manuale di stringhe SQL con LIMIT/OFFSET fatta finora nei singoli moduli.
	 *
	 * @param selectSql la lista di colonne selezionate (es. "*" o "cantieri.*, anagrafica.ragione_sociale" -
	 *            va qualificata esplicitamente se fromJoinSql unisce tabelle con campi omonimi)
	 * @param fromJoinSql la parte "from ... [join ...]" (senza la parola "from")
	 * @param whereSql la parte where (senza la parola "where"), pu� essere vuota o null
	 * @param whereArgs argomenti per i placeholder "?" in whereSql (usati sia per il count che per la pagina)
	 * @param orderBySql la parte order by (senza la parola "order by")
	 * @param limit numero massimo di righe per pagina
	 * @param offset offset della pagina (paginaCorrente * limit)
	 */
	public PaginaRisultati eseguiSelectPaginato(String selectSql, String fromJoinSql, String whereSql, String[] whereArgs,
			String orderBySql, int limit, int offset) {
		PaginaRisultati risultato = new PaginaRisultati();
		String whereClause = (whereSql != null && whereSql.length() > 0) ? " where " + whereSql : "";

		ArrayList<Object> conteggio = eseguiSelect(
				"Select count(*) as n from " + fromJoinSql + whereClause, whereArgs);
		risultato.totaleRisultati = conteggio.isEmpty() ? 0 : ((ContentValues) conteggio.get(0)).getAsInteger("n");

		String sql = "Select " + selectSql + " from " + fromJoinSql + whereClause
				+ " order by " + orderBySql
				+ " limit " + limit + " offset " + offset;
		risultato.righe = eseguiSelect(sql, whereArgs);

		return risultato;
	}

	/**
	 * Esegue una query libera su pi� tabelle in join
	 *
	 * @param sql
	 * @return
	 */
	public ArrayList<Object> eseguiSelect(String sql, String[] args) {
		return eseguiSelect(sql,args,getReadableDatabase());
	}

    /**
     * Esegue una query libera su pi� tabelle in join
     *
     * @param sql
     * @return
     */
    public ArrayList<Object> eseguiSelect(String sql, String[] args,SQLiteDatabase db) {
        return _eseguiSelect(sql,args,db);
    }



    /**
     * Esegue una query libera su pi� tabelle in join
     *
     * @param sql
     * @return
     */
    private ArrayList<Object> _eseguiSelect(String sql, String[] args,SQLiteDatabase db) {
        ArrayList<Object> records = new ArrayList<Object>();
        Cursor c = db.rawQuery(sql, args);
        if (c.getCount() > 0) {
            while (c.moveToNext()) {
                ContentValues record = new ContentValues();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    int tipo = c.getType(i);

                    if (tipo == Cursor.FIELD_TYPE_FLOAT) {
                        record.put(c.getColumnName(i), c.getDouble(i));
                    }
                    if (tipo == Cursor.FIELD_TYPE_STRING) {
                        record.put(c.getColumnName(i), c.getString(i));
                    }
                    if (tipo == Cursor.FIELD_TYPE_INTEGER) {
                        record.put(c.getColumnName(i), c.getLong(i));
                    }
                    if (tipo == Cursor.FIELD_TYPE_NULL) {
                        record.putNull(c.getColumnName(i));
                    }
                }
                records.add(record);
            }
        }

        c.close();
        return records;
    }

	/**
	 * Ritorna tutto il contenuto di una tabella in ArrayList di contentVBalues (Coppie chiave--valore) utilizzo:
	 * Arraylist lista = db.eseguiSelect(NOME_TABELLA); for (int i=0;i<lista.size();i++){ ContentValue val =
	 * lista.get(i); String valoreCAMPO = val.getString(NOME_CAMPO); int valoreCAMPONUM = val.getInt(NOME_CAMPONUM);
	 * .... }
	 * 
	 * @param tabella
	 * @return
	 */
	public ArrayList<Object> eseguiSelect(AbstractTable tabella) {
		return eseguiSelect(tabella, null, null);
	}

	/**
	 * 
	 * Ritorna tutto il contenuto di una tabella con filtri nella parte where in ArrayList di contentVBalues (Coppie
	 * chiave--valore) utilizzo: Arraylist lista = db.eseguiSelect(NOME_TABELLA); for (int i=0;i<lista.size();i++){
	 * ContentValue val = lista.get(i); String valoreCAMPO = val.getString(NOME_CAMPO); int valoreCAMPONUM =
	 * val.getInt(NOME_CAMPONUM); .... }
	 * 
	 * 
	 * @param tabella
	 * @param where
	 * @param orderby
	 * @return
	 */
	public ArrayList<Object> eseguiSelect(AbstractTable tabella, ContentValues where, String[] orderby) {

		return eseguiSelect(tabella, where, orderby, true);
	}

	/**
	 * 
	 * Ritorna tutto il contenuto di una tabella con filtri nella parte where in ArrayList di contentVBalues (Coppie
	 * chiave--valore) utilizzo: Arraylist lista = db.eseguiSelect(NOME_TABELLA); for (int i=0;i<lista.size();i++){
	 * ContentValue val = lista.get(i); String valoreCAMPO = val.getString(NOME_CAMPO); int valoreCAMPONUM =
	 * val.getInt(NOME_CAMPONUM); .... }
	 * 
	 * 
	 * @param tabella
	 * @param where
	 * @param orderby
	 * @param perditta
	 *            se true aggiunge il filtro sulla ditta selezionata
	 * @return
	 */
	public ArrayList<Object> eseguiSelect(AbstractTable tabella, ContentValues where, String[] orderby, boolean perditta) {

		if (perditta && where != null && !tabella.getNomeTabella().equals(Ditte.NOME_TABELLA)) {
			if (tabella.getNomiCampi().contains(AbstractTable.ID_DITTA)) {
				where.put(AbstractTable.ID_DITTA, Sessione.getDittaSelezionata());
			}
		}
		ArrayList<Object> records = new ArrayList<Object>();
		String SQL = "Select * from " + tabella.getNomeTabella();
		String[] valori = null;
		if (where != null && where.size() > 0) {
			valori = new String[where.size()];
			SQL = SQL + " where ";
			boolean first = true;
			int cont = 0;
			Iterator<String> iter = where.keySet().iterator();
			while (iter.hasNext()) {
				String nomeCampo = iter.next();
				if (first) {
					SQL = SQL + nomeCampo + "=?";
					first = false;
				} else {
					SQL = SQL + " and " + nomeCampo + "=?";
				}
				valori[cont] = where.getAsString(nomeCampo);
				cont++;
			}
		}

		if (orderby != null && orderby.length > 0) {
			SQL = SQL + " order by ";
			boolean first = true;
			int cont = 0;
			for (int i = 0; i < orderby.length; i++) {
				String nomeCampo = orderby[i];
				if (first) {
					SQL = SQL + nomeCampo;
					first = false;
				} else {
					SQL = SQL + "," + nomeCampo;
				}
			}

		}

		Cursor c = getReadableDatabase().rawQuery(SQL, valori);
		ArrayList<String> campi = tabella.getNomiCampi();
		if (c.getCount() > 0) {
			while (c.moveToNext()) {
				ContentValues record = new ContentValues();
				for (int i = 0; i < campi.size(); i++) {
					String nomecampo = campi.get(i);
					String tipo = tabella.getTipoCampo(nomecampo);
					if (tipo.equals(AbstractTable.INTEGER)) {
						record.put(nomecampo, getInt(c, nomecampo));
					}
					if (tipo.equals(AbstractTable.TEXT)) {
						record.put(nomecampo, getString(c, nomecampo));
					}
					if (tipo.equals(AbstractTable.NUMERIC)) {
						record.put(nomecampo, getDouble(c, nomecampo));
					}
					if (tipo.equals(AbstractTable.DATE)) {
						record.put(nomecampo, getLong(c, nomecampo));
					}
				}
				records.add(record);
			}
		}
		c.close();
		return records;
	}

	/**
	 * Ritorna il primo record trovato
	 * 
	 * @param tabella
	 * @param where
	 * @return
	 */
	public ContentValues getRecord(AbstractTable tabella, ContentValues where) {
		ArrayList<Object> vals = eseguiSelect(tabella, where, null);
		if (vals.size() > 0) {
			return (ContentValues) vals.get(0);
		}
		return null;
	}


	/**
	 * Ritorna il primo record trovato
	 *
	 * @param SQL
	 * @return
	 */
	public ContentValues getRecord(String SQL) {
		ArrayList<Object> vals = eseguiSelect(SQL,null);
		if (vals.size() > 0) {
			return (ContentValues) vals.get(0);
		}
		return null;
	}

	/**
	 * Ritorna il prossimo id della tabella<br>
	 * NB: i progressivi sono i numeri negativi per evitare problemi nelle versioni server <br>
	 * in cui ci sono le sincronizzazioni tra pi� client e un server<br>
	 * I record ricevuti dal server avranno numero da 1 in su quelli del tablet (versioni locali e nuovi record)<br>
	 * avranno numeri da -2 in gi� (-1 � riservato all'inserimento erratyo di Sqlite)
	 * 
	 * @param tabella
	 * @return il rpossimo id
	 */
	public int getProssimoId(AbstractTable tabella) {
		int prossimo = -2;
		String SQL = "Select min(" + tabella.getCampoNumeratore() + ") as numeratore from " + tabella.getNomeTabella() + " order by "
				+ tabella.getCampoNumeratore() + " asc ";
		Cursor c = getReadableDatabase().rawQuery(SQL, null);
		if (c.getCount() > 0) {
			c.moveToNext();
			int num = getInt(c, "numeratore");
			if (num >= 0) {
				prossimo = -2;
			} else {
				prossimo = num - 1;
			}

		}
		c.close();

		return prossimo;
	}

	/**
	 * Ritorna l'ultimo id della tabella<br>
	 * NB: i progressivi sono i numeri negativi per evitare problemi nelle versioni server <br>
	 * in cui ci sono le sincronizzazioni tra pi� client e un server<br>
	 * I record ricevuti dal server avranno numero da 1 in su quelli del tablet (versioni locali e nuovi record)<br>
	 * avranno numeri da -1 in gi�
	 * 
	 * @param tabella
	 * @return il rpossimo id
	 */
	public int getUltimoId(AbstractTable tabella) {
		int ultimo = 0;
		String SQL = "Select min(" + tabella.getCampoNumeratore() + ") as numeratore from " + tabella.getNomeTabella() + " order by "
				+ tabella.getCampoNumeratore() + " asc ";
		Cursor c = getReadableDatabase().rawQuery(SQL, null);
		if (c.getCount() > 0) {
			c.moveToNext();
			ultimo = getInt(c, "numeratore");

		}
		c.close();

		return ultimo;
	}

	/**
	 * Inserisce un record nella tabella <br>
	 * NB: Usare la funzione inserisciRecord della tabella corrispondente che effettua anche evantuali operazioni
	 * accessorie.
	 * 
	 * @param nomeTabella
	 * @param val
	 * @return
	 */
	public long insert(String nomeTabella, ContentValues val) {
		return getReadableDatabase().insertOrThrow(nomeTabella, null, val);
	}

	/**
	 * Aggiorna un record nella tabella<br>
	 * <br>
	 * NB: Usare la funzione aggiornaRecord della tabella corrispondente che effettua anche evantuali operazioni
	 * accessorie.
	 * 
	 * 
	 * @param nomeTabella
	 * @param val
	 * @return
	 */
	public int update(String nomeTabella, ContentValues val, ContentValues where) {
		String whereString = "";
		String[] parametri = new String[where.size()];
		boolean first = true;
		Iterator iter = where.keySet().iterator();
		int cont = 0;
		while (iter.hasNext()) {
			String campo = (String) iter.next();
			if (first) {
				first = false;
				whereString = campo + "=? ";
			} else {
				whereString = whereString + " and " + campo + "=? ";
			}
			parametri[cont] = where.getAsString(campo);
			cont++;
		}
		return getReadableDatabase().update(nomeTabella, val, whereString, parametri);
	}

	public int delete(String nomeTabella, ContentValues where) {
		String whereString = "";
		String[] parametri = new String[where.size()];
		boolean first = true;
		Iterator iter = where.keySet().iterator();
		int cont = 0;
		while (iter.hasNext()) {
			String campo = (String) iter.next();
			if (first) {
				first = false;
				whereString = campo + "=? ";
			} else {
				whereString = whereString + " and " + campo + "=? ";
			}
			parametri[cont] = where.getAsString(campo);
			cont++;
		}

		return getReadableDatabase().delete(nomeTabella, whereString, parametri);
	}

	public ArrayList<String> getNomiTabelle() {
		return  getNomiTabelle(getReadableDatabase());
	}

    public ArrayList<String> getNomiTabelle(SQLiteDatabase db) {
        ArrayList<Object> tabelleDB = eseguiSelect("SELECT name FROM sqlite_master WHERE type='table'", null,db);
        ArrayList<String> nomi = new ArrayList<String>();
        for (int i = 0; i < tabelleDB.size(); i++) {
            ContentValues valCurr = (ContentValues) tabelleDB.get(i);
            nomi.add(valCurr.getAsString("name"));
        }
        return nomi;
    }

	public void delete(String SQL, String[] params) {
		getReadableDatabase().execSQL(SQL, params);
	}

	public int getInt(Cursor c, String nomeCampo) {
		return c.getInt(c.getColumnIndex(nomeCampo));
	}

	public String getString(Cursor c, String nomeCampo) {
		return c.getString(c.getColumnIndex(nomeCampo));
	}

	public float getFloat(Cursor c, String nomeCampo) {
		return c.getFloat(c.getColumnIndex(nomeCampo));
	}

	public double getDouble(Cursor c, String nomeCampo) {
		return c.getDouble(c.getColumnIndex(nomeCampo));
	}

	public long getLong(Cursor c, String nomeCampo) {
		return c.getLong(c.getColumnIndex(nomeCampo));
	}

	public Resources getResources() {
		return getContext().getResources();
	}

	public Context getContext() {
		return cont;
	}

	public void setContext(Context cont) {
		this.cont = cont;
	}
}
