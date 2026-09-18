package pfa.app.econtab.db.table;

import android.content.ContentValues;
import android.content.Context;

import java.util.ArrayList;

import pfa.app.econtab.ElementoModActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.utils.Utility;

public class Elementi extends AbstractTable {
	public static final String NOME_TABELLA = "elementi";

	public static final String ID_ELEMENTO = "id_elemento";
	public static final String NOME_ELEMENTO = "nome_elemento";
	public static final String ID_CATEGORIA_GENERALE = "id_categoria_generale";
	public static final String METRI_CAVO = "metri_cavo";
	public static final String ID_ELEMENTO_CAVO = "id_elemento_cavo";
	public static final String METRI_TUBO = "metri_tubo";
	public static final String ID_ELEMENTO_TUBO = "id_elemento_tubo";
	public static final String ICONA = "icona";
	public static final String UNITA_MISURA = "unita_misura";
	public static final String ALTEZZA_DA_TERRA = "altezza_da_terra";
	public static final String PREFERITO_SN = "preferito_sn";
	public static final String PLACCA_SN = "placca_sn";

	public static final String PATH_ICONE = "icone_elementi";

	public Elementi() {
		setNomeTabella(NOME_TABELLA);
		setCampoNumeratore(ID_ELEMENTO);

		aggiungiCampo(ID_ELEMENTO, INTEGER);
		aggiungiCampo(NOME_ELEMENTO, TEXT);
		aggiungiCampo(ID_CATEGORIA_GENERALE, INTEGER);
		aggiungiCampo(METRI_CAVO, NUMERIC);
		aggiungiCampo(ID_ELEMENTO_CAVO, INTEGER);
		aggiungiCampo(METRI_TUBO, NUMERIC);
		aggiungiCampo(ID_ELEMENTO_TUBO, INTEGER);
		aggiungiCampo(ICONA, TEXT);
		aggiungiCampo(UNITA_MISURA, TEXT);
		aggiungiCampo(ALTEZZA_DA_TERRA, INTEGER);
		aggiungiCampo(PREFERITO_SN, INTEGER);
		aggiungiCampo(PLACCA_SN, INTEGER);
		aggiungiCampo(ID_OPERATORE_INS, INTEGER);
		aggiungiCampo(DATA_INS, DATE);
		aggiungiCampo(ID_OPERATORE_MOD, INTEGER);
		aggiungiCampo(DATA_MOD, DATE);
		aggiungiCampo(IN_SERVER, INTEGER);

		aggiungiCampoChiave(ID_ELEMENTO);
	}

	@Override
	protected void eliminaCorrelati(DbInterno db, ContentValues val) {
		// TODO Auto-generated method stub
		Composizioni tabComposizioni = new Composizioni();
		int cantiere = val.getAsInteger(ID_ELEMENTO);
		ContentValues whereComposizioni = new ContentValues();
		whereComposizioni.put(Composizioni.ID_ELEMENTO, cantiere);
		ArrayList<Object> comp = db.eseguiSelect(new Composizioni(), whereComposizioni, null);
		for (int i = 0; i < comp.size(); i++) {
			ContentValues valcompo = (ContentValues) comp.get(i);
			// ContentValues valcompowhere = tabComposizioni.getFiltroPerChiave(valcompo);
			// tabComposizioni.eliminaCorrelati(db, valcompo);
			// db.delete(Composizioni.NOME_TABELLA, valcompowhere);

			tabComposizioni.cancellaRecord(db, valcompo);
		}
		super.eliminaCorrelati(db, val);
	}

	@Override
	public boolean cancellazionePossibile(DbInterno db, ContentValues val, Context ctx) {
		// TODO Auto-generated method stub

		// se l'elemento � usato in qualche cantiere non lo faccio cancellare
		ArrayList<Object> utilizzi = db.eseguiSelect("Select count(*) as numero from " + ElementiCantiere.NOME_TABELLA + " where "
				+ ElementiCantiere.ID_ELEMENTO + "=" + val.getAsInteger(ID_ELEMENTO), null);
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

	@Override
	public String getCampoDescrizionePerSpinner() {
		// TODO Auto-generated method stub
		return NOME_ELEMENTO;
	}

	@Override
	public Class getDettaglioActivity() {
		// TODO Auto-generated method stub
		return ElementoModActivity.class;
	}

}
