package pfa.app.econtab;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import pfa.app.econtab.adapters.PiantinaAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.fragments.LocalePiantinaFragment;
import pfa.app.econtab.views.EConTabPiantinaGridView;

public class PiantinaLocaleModActivity extends EConTabDettaglioActivity implements OnItemClickListener {

	private EConTabPiantinaGridView pianta = null;
	private PiantinaAdapter adapter = null;
	private Boolean[] selezionati = null;
	private int numColonne = LocalePiantinaFragment.NUMERO_COLONNE;
	private int numRighe = LocalePiantinaFragment.NUMERO_RIGHE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: PiantinaLocaleModActivity onCreate ENTER");
		setContentView(R.layout.activity_piantina_locale_mod);
		super.onCreate(savedInstanceState);

		pianta = (EConTabPiantinaGridView) findViewById(R.id.gridView_piantina);
		 if (getIntent().getIntExtra("NUMCOLONNE",0)>0){
			 numColonne = getIntent().getIntExtra("NUMCOLONNE",0);
		 }
		if (getIntent().getIntExtra("NUMRIGHE",0)>0){
			numRighe = getIntent().getIntExtra("NUMRIGHE",0);
		}

		pianta.setNumColumns(numColonne);


		pianta.setInModifica(true);
		pianta.setOnItemClickListener(this);
		refresh();
		System.out.println("EConTab: PiantinaLocaleModActivity onCreate EXIT");
	}

	public void refresh() {
		System.out.println("EConTab: PiantinaLocaleModActivity onCreate ENTER");
		// TODO Auto-generated method stub
		pianta.invalidate();
		if (adapter == null) {
			adapter = new PiantinaAdapter(this);
			pianta.setAdapter(adapter);
			selezionati = new Boolean[numColonne * numRighe];
			DbInterno db = new DbInterno(this);
			ContentValues where = new ContentValues();
			where.put(Locali.ID_LOCALE, getIDModifica());
			ContentValues val = db.getRecord(new Locali(), where);
			db.close();

			String piantina = "";
			if (val != null) {
				piantina = val.getAsString(Locali.PIANTA_LOGICA);
			}

			if (piantina.length()<numColonne*numRighe){
				if (piantina.length()==36){
					String pianta1 = piantina.substring(0,6);
					String pianta2 = piantina.substring(6,12);
					String pianta3 = piantina.substring(12,18);
					String pianta4 = piantina.substring(18,24);
					String pianta5 = piantina.substring(24,30);
					String pianta6 = piantina.substring(30,36);

					String zeriTxt = "";
					int zeri = LocalePiantinaFragment.NUMERO_COLONNE-6;
					int righeFinali = LocalePiantinaFragment.NUMERO_RIGHE-6;
					for (int i=0;i<zeri;i++){
						zeriTxt = zeriTxt+"0";
					}

					String zeriFinale = "";
					for (int i=0;i<righeFinali*LocalePiantinaFragment.NUMERO_COLONNE;i++){
						zeriFinale = zeriFinale+"0";
					}
					piantina = pianta1+zeriTxt+pianta2+zeriTxt+pianta3+zeriTxt+pianta4+zeriTxt+pianta5+zeriTxt+pianta6+zeriTxt+zeriFinale;
				}
				else {
					while (piantina.length() < numColonne * numRighe) {
						piantina = piantina + "1";
					}
				}
			}

			for (int i = 0; i < selezionati.length; i++) {
				String sel = String.valueOf(piantina.charAt(i));
				if (sel.equals("1")) {
					selezionati[i] = true;
				} else {
					selezionati[i] = false;
				}

			}
		}

		adapter.setSelezionati(selezionati);
		adapter.notifyDataSetChanged();
		System.out.println("EConTab: PiantinaLocaleModActivity onCreate EXIT");
	}

	@Override
	public void onItemClick(AdapterView<?> adapt, View v, int position, long arg3) {
		System.out.println("EConTab: PiantinaLocaleModActivity onItemClick ENTER");
		// TODO Auto-generated method stub
		selezionati[position] = !selezionati[position];
		refresh();
		System.out.println("EConTab: PiantinaLocaleModActivity onItemClick EXIT");
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: PiantinaLocaleModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		ContentValues val = new ContentValues();
		String piantina = "";

		for (int i = 0; i < selezionati.length; i++) {
			if (selezionati[i]) {
				piantina = piantina + "1";
			} else {
				piantina = piantina + "0";
			}
		}
		val.put(Locali.PIANTA_LOGICA, piantina);
		ContentValues where = new ContentValues();
		where.put(Locali.ID_LOCALE, getIDModifica());
		Locali tabLocali = new Locali();
		tabLocali.aggiornaRecord(db, val, where);
		System.out.println("EConTab: PiantinaLocaleModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	public void impostaPianta(View v) {
		System.out.println("EConTab: PiantinaLocaleModActivity impostaPianta ENTER");
		if (v.getId() == R.id.button_pianta_0111) {
			for (int i = 0; i < numColonne*numRighe/2; i++) {
				if (i%numColonne>=numRighe/2){
					selezionati[i] = true;
				}
				else {
					selezionati[i] = false;
				}

			}
			for (int i=numColonne*numRighe/2;i<selezionati.length;i++){
				selezionati[i] = true;
			}
		}

		if (v.getId() == R.id.button_pianta_1110) {
			for (int i = 0; i < numColonne*numRighe/2; i++) {
				selezionati[i] = true;
			}
			for (int i=numColonne*numRighe/2;i<selezionati.length;i++){
				if (i%numColonne>=numRighe/2){
					selezionati[i] = false;
				}
				else {
					selezionati[i] = true;
				}
			}
		}

		if (v.getId() == R.id.button_pianta_1011) {
			for (int i = 0; i < numColonne*numRighe/2; i++) {
				if (i%numColonne>=numRighe/2){
					selezionati[i] = false;
				}
				else {
					selezionati[i] = true;
				}

			}
			for (int i=numColonne*numRighe/2;i<selezionati.length;i++){
				selezionati[i] = true;
			}
		}

		if (v.getId() == R.id.button_pianta_1101) {
			for (int i = 0; i < numColonne*numRighe/2; i++) {
				selezionati[i] = true;
			}
			for (int i=numColonne*numRighe/2;i<selezionati.length;i++){
				if (i%numColonne>=numRighe/2){
					selezionati[i] = true;
				}
				else {
					selezionati[i] = false;
				}
			}
		}

		if (v.getId() == R.id.button_pianta_1111) {
			for (int i = 0; i < selezionati.length; i++) {
				selezionati[i] = true;

			}
		}

		if (v.getId() == R.id.button_pianta_rectoriz) {
			for (int i = 0; i < selezionati.length; i++) {
				selezionati[i] = true;
			}
			for (int i=0;i<numColonne;i++){
				selezionati[i] = false;
				selezionati[numColonne+i] = false;
				selezionati[numColonne*(numRighe-2)+i] = false;
				selezionati[numColonne*(numRighe-1)+i] = false;
			}



		}

		if (v.getId() == R.id.button_pianta_rectvert) {
			for (int i = 0; i < selezionati.length; i++) {
				selezionati[i] = true;
			}
			for (int i=0;i<numRighe;i++){
				selezionati[numColonne*i] = false;
				selezionati[numColonne*i+1] = false;
				selezionati[numColonne*(i+1)-2] = false;
				selezionati[numColonne*(i+1)-1] = false;
			}
		}
		refresh();
		System.out.println("EConTab: PiantinaLocaleModActivity impostaPianta EXIT");
	}

}
