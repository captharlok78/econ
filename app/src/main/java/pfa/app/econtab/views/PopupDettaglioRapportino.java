package pfa.app.econtab.views;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import pfa.app.econtab.R;
import pfa.app.econtab.RapportinoDettaglioModActivity;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Manodopera;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.Rapportini;
import pfa.app.econtab.db.table.RapportiniDettaglio;
import pfa.app.econtab.utils.Utility;

public class PopupDettaglioRapportino implements OnClickListener, TextWatcher {

	private View vista = null;
	private Context ctx = null;
	private AlertDialog di = null;

	private int idRapportino = 0;
    private int idRapportinoDettaglio = 0;
	private EConTabSpinner spinnerManodopera = null;
	private EditText editOre = null;
    private EditText editNota = null;

	public PopupDettaglioRapportino(Context ctx,  int idRapportino,int idRapportinoDettaglio) {
		vista = View.inflate(ctx, R.layout.dialog_rapportino_dett, null);
		this.ctx = ctx;
		this.idRapportino = idRapportino;
        this.idRapportinoDettaglio = idRapportinoDettaglio;
		impostaCampi();
	}

	private void impostaCampi() {
        spinnerManodopera = (EConTabSpinner) vista.findViewById(R.id.econtabSpinner_manodopera);
        spinnerManodopera.addTextChangeListener(this);
		editOre  = (EditText) vista.findViewById(R.id.editText_ore);
        editOre.addTextChangedListener(this);
        editNota  = (EditText) vista.findViewById(R.id.editText_nota_dett);
        if (idRapportinoDettaglio==0){
            spinnerManodopera.setTabella(new Manodopera());
        }
        else{
            DbInterno db = new DbInterno(ctx);
            ContentValues where = new ContentValues();
            where.put(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO,idRapportinoDettaglio);
            ContentValues valDett = db.getRecord(new RapportiniDettaglio(),where);
            db.close();
            if (valDett!=null){
                spinnerManodopera.setValue(""+valDett.getAsInteger(RapportiniDettaglio.ID_MANODOPERA));
                editOre.setText(Utility.formatNumero(valDett.getAsDouble(RapportiniDettaglio.ORE)));
                editNota.setText(valDett.getAsString(RapportiniDettaglio.NOTE));
            }
            spinnerManodopera.setTabella(new Manodopera());
        }
	}

	public void apriPopup() {

		di = Utility.mostraDialogPersonalizzato(ctx.getString(R.string.dettaglio_rapportino), ctx, vista, ctx.getString(R.string.salva),
				ctx.getString(R.string.annulla), this);

		controllaAbilitazioneTastoConferma();
	}

	@Override
	public void onClick(DialogInterface arg0, int which) {
		// TODO Auto-generated method stub
		if (which == DialogInterface.BUTTON_POSITIVE) {
			DbInterno db = new DbInterno(ctx);
            RapportiniDettaglio tabRappDett = new RapportiniDettaglio();
            if (idRapportinoDettaglio==0){
                ContentValues valInsert = tabRappDett.getValoriLogInserimento(db);
                valInsert.put(RapportiniDettaglio.ID_RAPPORTINO,idRapportino);
                valInsert.put(RapportiniDettaglio.ID_MANODOPERA,spinnerManodopera.getValue());
                valInsert.put(RapportiniDettaglio.ORE,Utility.formatNumeroDB(editOre.getText().toString()));
                valInsert.put(RapportiniDettaglio.NOTE, editNota.getText().toString());
                tabRappDett.inserisciRecord(db,valInsert);
            }
            else{
                ContentValues valUpd = tabRappDett.getValoriLogModifica(db);

                valUpd.put(RapportiniDettaglio.ID_MANODOPERA,spinnerManodopera.getValue());
                valUpd.put(RapportiniDettaglio.ORE,Utility.formatNumeroDB(editOre.getText().toString()));
                valUpd.put(RapportiniDettaglio.NOTE,editNota.getText().toString());
                ContentValues where = new ContentValues();
                where.put(RapportiniDettaglio.ID_RAPPORTINO_DETTAGLIO,idRapportinoDettaglio);
                tabRappDett.aggiornaRecord(db,valUpd,where);
            }


            //controllo che nell'ordine esista già la riga di manodopera, altrimneti la inserisco a 0
            int idManodopera = Integer.parseInt(spinnerManodopera.getValue());

            Rapportini tabRapp = new Rapportini();
            ContentValues whereRapp = new ContentValues();
            whereRapp.put(Rapportini.ID_RAPPORTINO,idRapportino);
            ContentValues recRapp = db.getRecord(tabRapp,whereRapp);
            if (recRapp!=null){
                int idOrdine = recRapp.getAsInteger(Rapportini.ID_ORDINE);
                ContentValues chk = new ContentValues();
                chk.put(PreventiviDettaglio.ID_PREVENTIVO,idOrdine);
                chk.put(PreventiviDettaglio.ID_MANODOPERA,idManodopera);
                PreventiviDettaglio pd = new PreventiviDettaglio();
                ContentValues recChk = db.getRecord(pd,chk);
                if (recChk==null) {
                    ContentValues whereManodopera = new ContentValues();
                    whereManodopera.put(Manodopera.ID_MANODOPERA, idManodopera);
                    ContentValues valMano = db.getRecord(new Manodopera(), whereManodopera);
                    //pd.aggiornaRigaPreventivo(db, idOrdine, valMano, 0, PreventiviDettaglio.MANOPERA, false,true);
                    pd.aggiornaRigaMaterialePreventivo(db,idOrdine,valMano,0,PreventiviDettaglio.MANOPERA,false);
                }
            }

			db.close();
            ((RapportinoDettaglioModActivity)ctx).caricaDettaglio();
		}
	}

	private void controllaAbilitazioneTastoConferma() {

        if (di!=null) {
            if (spinnerManodopera.getValue().equals("") || editOre.getText().toString().equals("")) {
                di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            } else {
                di.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            }
        }

	}
    
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
controllaAbilitazioneTastoConferma();
    }
}
