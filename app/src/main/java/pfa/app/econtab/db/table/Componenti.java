package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.utils.Utility;

public class Componenti extends AbstractTable {
	public static final String NOME_TABELLA = "componenti";

	public static final String ID_COMPONENTE = "id_componente";
	public static final String ID_CATEGORIA_COMPONENTE = "id_categoria_componente";
	public static final String NOME_COMPONENTE = "nome_componente";
	public static final String LINEA_SN = "linea_sn";
	public static final String SPAZI_OCCUPATI = "spazi_occupati";
	public static final String SPAZI_OSPITATI = "spazi_ospitati";
	public static final String TAPPO_SN = "tappo_sn";
	public static final String METRI_CAVO = "metri_cavo";
	public static final String ID_ELEMENTO_CAVO = "id_elemento_cavo";
	public static final String METRI_TUBO = "metri_tubo";
	public static final String ID_ELEMENTO_TUBO = "id_elemento_tubo";
	public static final String ICONA = "icona";
	public static final String UNITA_MISURA = "unita_misura";
	public static final String PREFERITO_SN = "preferito_sn";

	public static final String PATH_ICONE = "icone_componenti";
	public static final int SCATOLE = 1;
	public static final int PORTAFRUTTI = 2;
	public static final int PRESE = 3;
	public static final int INTERRUTTORI = 4;
	public static final int COPRISCATOLA = 5;
	public static final int CENTRALINI = 7;
	public static final int COMPONENTI_QUADRI = 8;
	public static final int COMPONENTI_INTERRUTTORI = 9;

	public Componenti() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_COMPONENTE);

		aggiungiCampo(ID_COMPONENTE, INTEGER);
		aggiungiCampo(ID_CATEGORIA_COMPONENTE, INTEGER);
		aggiungiCampo(NOME_COMPONENTE, TEXT);
		aggiungiCampo(LINEA_SN, INTEGER);
		aggiungiCampo(SPAZI_OCCUPATI, NUMERIC);
		aggiungiCampo(SPAZI_OSPITATI, INTEGER);
		aggiungiCampo(TAPPO_SN, INTEGER);
		aggiungiCampo(METRI_CAVO, NUMERIC);
		aggiungiCampo(ID_ELEMENTO_CAVO, INTEGER);
		aggiungiCampo(METRI_TUBO, NUMERIC);
		aggiungiCampo(ID_ELEMENTO_TUBO, INTEGER);
		aggiungiCampo(ICONA, TEXT);
		aggiungiCampo(UNITA_MISURA, TEXT);
		aggiungiCampo(PREFERITO_SN, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_COMPONENTE);
	}

	@Override
	public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {
		// TODO Auto-generated method stub
		ContentValues daCancellare = db.getRecord(this, getFiltroPerChiave(val));
		if (daCancellare != null && daCancellare.getAsInteger(TAPPO_SN) == 1) {
			ArrayList<Object> tappi = db.eseguiSelect("Select count(*) as numero from " + NOME_TABELLA + " where " + TAPPO_SN + "=1", null);
			if (tappi.size() == 1) {
				String messaggio = ctx.getString(R.string.cancellazione_non_possibile_tappo);
				Utility.mostraDialog(ctx.getString(R.string.attenzione), messaggio, ctx, "OK");
				return false;
			}
		}

		// se l'elemento � usato in qualche cantiere non lo faccio cancellare
		ArrayList<Object> utilizzi = db.eseguiSelect("Select count(*) as numero from " + ComponentiCantiere.NOME_TABELLA + " where "
				+ ComponentiCantiere.ID_COMPONENTE + "=" + val.getAsInteger(ID_COMPONENTE), null);
		if (utilizzi.size() > 0) {
			ContentValues cant = (ContentValues) utilizzi.get(0);
			if (cant.getAsInteger("numero") > 0) {
				String messaggio = ctx.getString(R.string.cancellazione_non_possibile, cant.getAsString("numero"));
				Utility.mostraDialog(ctx.getString(R.string.attenzione), messaggio, ctx, "OK");
				return false;
			}
		}

		return super.cancellazionePossibile(db, val, ctx);
	}

}
