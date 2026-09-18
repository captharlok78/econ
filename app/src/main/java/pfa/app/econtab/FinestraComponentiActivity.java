package pfa.app.econtab;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.CategorieComponenti;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.ComponentiCantComposti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComponentiComposti;
import pfa.app.econtab.db.table.Composizioni;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.fragments.ComponentiCategoriaFragment;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabItemMenu;

public class FinestraComponentiActivity extends EConTabActivity implements OnClickListener {

	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

	int numeroCategorie = 0;
	TabHost tabHost = null;
	HorizontalScrollView scroll_tab = null;
	private LinearLayout categorie = null;
	int selezionato = 0;

	int categoriaSelezionata = 0;
	int idCategoriaIntent = 0;
	int idElementoIntent = 0;
	int idElementoCantIntent = 0;

	int idComponenteIntent = 0;
	int idComponenteCantIntent = 0;

	boolean frutti = false;

	ImageButton buttonChiudi = null;

	ArrayList<EConTabItemMenu> listaCategorie = null;

	private TextView titolo = null;

	private boolean composizioneLibera = false;
	private int numSpazi = 0;
	private int numSpaziOrigine = 0;

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {

			ComponentiCategoriaFragment fragment = new ComponentiCategoriaFragment();
			Bundle args = new Bundle();
			int idCategoria = listaCategorie.get(position).getIdCategoria();

			args.putInt(CategorieComponenti.ID_CATEGORIA_COMPONENTE, idCategoria);

			if (idCategoria == Componenti.COPRISCATOLA || idCategoria == Componenti.PORTAFRUTTI || idCategoria == Componenti.CENTRALINI) {
				args.putInt(Componenti.SPAZI_OSPITATI, getIntent().getIntExtra(Componenti.SPAZI_OSPITATI, 0));
			}

			if ((idCategoria == Componenti.SCATOLE || idCategoria==Componenti.CENTRALINI) && numSpazi>0){
				args.putInt(Componenti.SPAZI_OSPITATI, numSpazi);

			}

			fragment.setArguments(args);

			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return numeroCategorie;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: FinestraComponentiActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
        setVisualizzazionePopup();
		setContentView(R.layout.activity_finestra_componenti);


		titolo = (TextView) findViewById(R.id.textViewTitolo);

		scroll_tab = (HorizontalScrollView) findViewById(R.id.scroll_categorie);
		categorie = (LinearLayout) findViewById(R.id.categorie);
		idCategoriaIntent = getIntent().getIntExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, 0);
		idElementoIntent = getIntent().getIntExtra(Elementi.ID_ELEMENTO, 0);
		idElementoCantIntent = getIntent().getIntExtra(ElementiCantiere.ID_ELEMENTO_CANT, 0);

		idComponenteIntent = getIntent().getIntExtra(Componenti.ID_COMPONENTE, 0);
		idComponenteCantIntent = getIntent().getIntExtra(ComponentiCantiere.ID_COMPONENTE_CANT, 0);

		composizioneLibera = getIntent().getBooleanExtra("COMPOSIZIONELIBERA", false);
		numSpazi = getIntent().getIntExtra("NUM_SPAZI",0);
		numSpaziOrigine = getIntent().getIntExtra("NUM_SPAZI_ORIGINE",0);

		frutti = getIntent().getBooleanExtra("FRUTTI", false);
		creaMenuCategorie();

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {

				categorie.getChildAt(selezionato).setSelected(false);
				categorie.getChildAt(position).setSelected(true);
				selezionato = position;

				EConTabItemMenu viewSel = (EConTabItemMenu) categorie.getChildAt(position);
				titolo.setText(viewSel.getTesto());

				scroll_tab.smoothScrollTo(viewSel.getLeft() - viewSel.getWidth(), 0);

			}
		});

		mViewPager.setCurrentItem(selezionato);
		System.out.println("EConTab: FinestraComponentiActivity onCreate EXIT");
	}

	public void chiudi(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		System.out.println("EConTab: FinestraComponentiActivity onWindowFocusChanged ENTER");
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		if (selezionato > 1) {

			scroll_tab.smoothScrollTo(categorie.getChildAt(selezionato).getLeft() - categorie.getChildAt(selezionato).getWidth(), 0);
		}
		System.out.println("EConTab: FinestraComponentiActivity onWindowFocusChanged EXIT");
	}

	private void creaMenuCategorie() {
		System.out.println("EConTab: FinestraComponentiActivity creaMenuCategorie ENTER");
		// TODO Auto-generated method stub
		DbInterno db = new DbInterno(this);
		CategorieComponenti tabCat = new CategorieComponenti();
		ContentValues where = new ContentValues();
		if (idCategoriaIntent != 0) {
			where.put(CategorieComponenti.ID_CATEGORIA_COMPONENTE, idCategoriaIntent);
			scroll_tab.setVisibility(View.GONE);
			try{
				findViewById(R.id.textView2).setVisibility(View.GONE);
			}
			catch (Exception ex){

			}


		}

		if (frutti == true) {
			where.put(CategorieComponenti.TIPO, CategorieComponenti.TIPO_FRUTTO);
		}

		ArrayList<Object> lista = db.eseguiSelect(tabCat, where, null);
		db.close();
		numeroCategorie = lista.size();

		for (int i = 0; i < lista.size(); i++) {
			ContentValues val = (ContentValues) lista.get(i);
			EConTabItemMenu item = new EConTabItemMenu(this);
			item.setTesto(val.getAsString(CategorieComponenti.NOME));
			item.setIcona(CategorieComponenti.PATH_ICONE, val.getAsString(CategorieComponenti.ICONA));

			item.setOnClickListener(this);
			item.setTag("" + i);
			item.setIdCategoria(val.getAsInteger(CategorieComponenti.ID_CATEGORIA_COMPONENTE));
			if (listaCategorie == null) {
				listaCategorie = new ArrayList<EConTabItemMenu>();
			}
			listaCategorie.add(item);

			// seleziono il primo
			if (i == 0) {
				item.setSelected(true);
				titolo.setText(item.getTesto());
			}

			categorie.addView(item);

		}
		System.out.println("EConTab: FinestraComponentiActivity creaMenuCategorie EXIT");
	}

	public void aggiungiElemento(final ContentValues val) {
		System.out.println("EConTab: FinestraComponentiActivity aggiungiElemento ENTER");
		// TODO Auto-generated method stub

		// Prendo il record della categoria

		DbInterno dbCat = new DbInterno(this);
		ContentValues whereCAT = new ContentValues();
		whereCAT.put(CategorieComponenti.ID_CATEGORIA_COMPONENTE, val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE));
		ContentValues valCAT = dbCat.getRecord(new CategorieComponenti(), whereCAT);

		dbCat.close();

		if (valCAT != null) {

			if (composizioneLibera) {
				View campi = View.inflate(this, R.layout.dialog_qtacomponente_layout, null);
				final TextView etichetta = (TextView) campi.findViewById(R.id.textView_qta);
				final EditText qta = (EditText) campi.findViewById(R.id.editText_qta);
				final TextView componentiTxt = (TextView) campi.findViewById(R.id.textView_componenti);
				DbInterno db = new DbInterno(this);
				int numPresenti = 0;
				if (idElementoIntent != 0) {


					String sql = "Select count(*) as numero from composizioni where id_elemento="+idElementoIntent+" and id_componente=" + val.getAsInteger(Componenti.ID_COMPONENTE);

					ArrayList<Object> result = db.eseguiSelect(sql, null);
					if (result!=null && result.size()>0){
						ContentValues resCount = (ContentValues)result.get(0);
						numPresenti = resCount.getAsInteger("numero");
					}
				}
				if (idElementoCantIntent != 0) {

					String sql = "Select count(*) as numero from composizioni_cantiere inner join componenti_cantiere on composizioni_cantiere.id_componente_cant=componenti_cantiere.id_componente_cant where composizioni_cantiere.id_elemento_cant="+idElementoCantIntent+" and componenti_cantiere.id_componente=" + val.getAsInteger(Componenti.ID_COMPONENTE);

					ArrayList<Object> result = db.eseguiSelect(sql, null);
					if (result!=null && result.size()>0){
						ContentValues resCount = (ContentValues)result.get(0);
						numPresenti = resCount.getAsInteger("numero");
					}
				}
				db.close();
				componentiTxt.setText("Componenti uguali già aggiunti in questo elemento: " + numPresenti);
				qta.setText("1");

				etichetta.setText(getString(R.string.quantita) + " " + val.getAsString(Componenti.UNITA_MISURA).trim());

				Utility.mostraDialogPersonalizzato(
						val.getAsString(Componenti.NOME_COMPONENTE).toUpperCase(Locale.getDefault()),
						new BitmapDrawable(getResources(), Utility.getIconaScalata(this, Componenti.PATH_ICONE,
								val.getAsString(Componenti.ICONA))), this, campi, this.getString(R.string.conferma),
						this.getString(R.string.annulla), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {
								if (i==DialogInterface.BUTTON_POSITIVE){
									int qtaInt = 0;
									try{
										qtaInt = Integer.parseInt(qta.getText().toString());
									}
									catch (Exception ex){

									}
									DbInterno db = new DbInterno(FinestraComponentiActivity.this);
									if (qtaInt>0){
										for (int n=0;n<qtaInt;n++){
											if (idElementoIntent != 0) {
												Composizioni tabComposizioni = new Composizioni();

												tabComposizioni.aggiungiComponenteLibero(db, val, idElementoIntent);
											}
											if (idElementoCantIntent != 0) {
												ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
												boolean componenteAggiuntivoScatola = getIntent().getBooleanExtra("COMPONENTE_AGGIUNTIVO_SCATOLA",false);

												tabComposizioni.aggiungiComponenteLibero(db, val, idElementoCantIntent,componenteAggiuntivoScatola);
											}
										}

									}
									db.close();
								}
								setResult(RESULT_OK);
								finish();
							}
						});
			} else {

				if (val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.SCATOLE) {
					//se non posso sostituire i componenti perchè ci sono relazioni allora cambio solo la scatola e mantengo
					//i frutti
					if (numSpazi>0 && idElementoCantIntent!=0){
						ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
						DbInterno db = new DbInterno(FinestraComponentiActivity.this);
						tabComposizioni.sostituisciScatola(val, idElementoCantIntent, db, false, false,false);
						db.close();
						setResult(RESULT_OK);
						finish();
					}
					else{
						String[] items = new String[3];
						if (idElementoCantIntent!=0 && numSpaziOrigine==val.getAsInteger(Componenti.SPAZI_OSPITATI)){
							items = new String[4];
						}
						items[0] = getString(R.string.con_portafrutti);
						items[1] = getString(R.string.senza_portafrutti);
						items[2] = getString(R.string.con_copriscatola);
						if (idElementoCantIntent!=0 && numSpaziOrigine==val.getAsInteger(Componenti.SPAZI_OSPITATI)){
							items[3] = getString(R.string.mantieni_composizione_frutti);
						}
						Utility.mostraSelezioneDialog(getString(R.string.completa_scatola), items, this, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								if (idElementoIntent != 0) {
									Composizioni tabComposizioni = new Composizioni();
									boolean portafrutti = false;
									boolean senzaPortafrutti = false;
									boolean copriscatola = false;

									if (which == 0) {
										portafrutti = true;
									}
									if (which == 1) {
										senzaPortafrutti = true;
									}
									if (which == 2) {
										copriscatola = true;
									}

									DbInterno db = new DbInterno(FinestraComponentiActivity.this);
									if (getIntent().getBooleanExtra("MODIFICA", false)) {
										tabComposizioni.sostituisciScatola(val, idElementoIntent, db, portafrutti,senzaPortafrutti, copriscatola);
									} else {
										tabComposizioni.componiScatola(val, idElementoIntent, db, portafrutti,senzaPortafrutti, copriscatola);
									}

									db.close();
								}
								if (idElementoCantIntent != 0) {
									ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
									boolean portafrutti = false;
									boolean senzaPortafrutti = false;
									boolean copriscatola = false;
									if (which == 0) {
										portafrutti = true;
									}
									if (which == 1) {
										senzaPortafrutti = true;
									}
									if (which == 2) {
										copriscatola = true;
									}
									if (which==3){
										portafrutti = false;
										copriscatola = false;
									}
									DbInterno db = new DbInterno(FinestraComponentiActivity.this);
									tabComposizioni.sostituisciScatola(val, idElementoCantIntent, db, portafrutti,senzaPortafrutti, copriscatola);

									db.close();
								}
								setResult(RESULT_OK);
								finish();
							}
						});
					}
				}

				if (valCAT.getAsInteger(CategorieComponenti.ID_CATEGORIA_COMPONENTE) == Componenti.COPRISCATOLA) {
					DbInterno db = new DbInterno(this);
					if (idElementoIntent != 0) {
						Composizioni tabComposizioni = new Composizioni();

						tabComposizioni.sostituisciCopriscatola(db, val, idElementoIntent);
					}
					if (idElementoCantIntent != 0) {
						ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();

						tabComposizioni.sostituisciCopriscatola(db, val, idElementoCantIntent);
					}
					db.close();
					setResult(RESULT_OK);
					finish();
				}

				if (valCAT.getAsInteger(CategorieComponenti.ID_CATEGORIA_COMPONENTE) == Componenti.PORTAFRUTTI) {
					DbInterno db = new DbInterno(this);

					if (idElementoIntent != 0) {
						Composizioni tabComposizioni = new Composizioni();
						tabComposizioni.sostituisciPortafrutti(db, val, idElementoIntent);
					}
					if (idElementoCantIntent != 0) {
						ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
						tabComposizioni.sostituisciPortafrutti(db, val, idElementoCantIntent);
					}
					db.close();
					setResult(RESULT_OK);
					finish();
				}

				if (valCAT.getAsString(CategorieComponenti.TIPO).equals(CategorieComponenti.TIPO_FRUTTO)) {
					DbInterno db = new DbInterno(this);

					if (idElementoIntent != 0) {
						Composizioni tabComposizioni = new Composizioni();
						int posizioneIniziale = getIntent().getIntExtra(Composizioni.POSIZIONE_INIZIALE, 1);
						tabComposizioni.aggiungiFrutto(db, val, idElementoIntent, posizioneIniziale);
					}
					if (idElementoCantIntent != 0) {
						ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
						int posizioneIniziale = getIntent().getIntExtra(ComposizioniCantiere.POSIZIONE_INIZIALE, 1);
						tabComposizioni.aggiungiFrutto(db, val, idElementoCantIntent, posizioneIniziale);
					}

					db.close();
					setResult(RESULT_OK);
					finish();
				}

				if (val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.CENTRALINI) {
					DbInterno db = new DbInterno(this);
					if (numSpazi>0 && idElementoCantIntent!=0){
						ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();


						tabComposizioni.sostituisciCentralinoQuadro(db,val, idElementoCantIntent);


					}
					else{
						if (idElementoIntent != 0) {
							Composizioni tabComposizioni = new Composizioni();
							tabComposizioni.aggiungiCentralinoQuadro(db, val, idElementoIntent);
						}
						if (idElementoCantIntent != 0) {
							ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
							tabComposizioni.aggiungiCentralinoQuadro(db, val, idElementoCantIntent);
						}
					}
					db.close();
					setResult(RESULT_OK);
					finish();
				}

				if (val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.COMPONENTI_QUADRI) {
					DbInterno db = new DbInterno(this);

					if (idElementoIntent != 0) {
						Composizioni tabComposizioni = new Composizioni();
						tabComposizioni.aggiungiComponenteQuadro(db, val, idElementoIntent);
					}
					if (idElementoCantIntent != 0) {
						ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
						tabComposizioni.aggiungiComponenteQuadro(db, val, idElementoCantIntent);
					}
					db.close();
					setResult(RESULT_OK);
					finish();
				}

				if (val.getAsInteger(Componenti.ID_CATEGORIA_COMPONENTE) == Componenti.COMPONENTI_INTERRUTTORI) {
					DbInterno db = new DbInterno(this);
					if (idComponenteIntent != 0) {
						ComponentiComposti tabComp = new ComponentiComposti();
						ContentValues valINS = tabComp.getValoriLogInserimento(db);
						valINS.put(ComponentiComposti.ID_COMPONENTE_PADRE, idComponenteIntent);
						valINS.put(ComponentiComposti.ID_COMPONENTE_FIGLIO, val.getAsInteger(Componenti.ID_COMPONENTE));
						tabComp.inserisciRecord(db, valINS);
					}
					if (idComponenteCantIntent != 0) {
						// inserisco prima il componente_cantiere
						ComponentiCantiere tabCompCant = new ComponentiCantiere();
						ContentValues valCompCS = tabCompCant.inserisciComponenteCantiere(db, val);

						ComponentiCantComposti tabComp = new ComponentiCantComposti();
						ContentValues valINS = tabComp.getValoriLogInserimento(db);
						valINS.put(ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE, idComponenteCantIntent);
						valINS.put(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO,
								valCompCS.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
						tabComp.inserisciRecord(db, valINS);

						// se � il primo componente aggiunto aggiorno il preventivo togliendo il componente padre
						/*ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
						ContentValues valElem = tabComposizioni.getElementoCantiereDaComponente(db, idComponenteCantIntent);
						if (valElem != null && valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO) != 0) {
							int idPreventivo = valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
							if (tabComp.getComposizioneComponente(db, idComponenteCantIntent).size() == 1) {
								PreventiviDettaglio tabPrev = new PreventiviDettaglio();
								ContentValues whereComp = new ContentValues();
								whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, idComponenteCantIntent);
								ContentValues componente = db.getRecord(new ComponentiCantiere(), whereComp);
								componente.put(ElementiCantiere.ID_LOCALE, valElem.getAsInteger(ElementiCantiere.ID_LOCALE));
								tabPrev.aggiornaRigaPreventivo(db, idPreventivo, componente, -1, PreventiviDettaglio.MATERIALE, true);
							}
						}*/

					}
					db.close();
					Toast.makeText(this, getString(R.string.elemento_aggiunto), Toast.LENGTH_SHORT).show();
					// setResult(RESULT_OK);
					// finish();

				}
			}
		}
		System.out.println("EConTab: FinestraComponentiActivity aggiungiElemento EXIT");
	}

	@Override
	public void onClick(View v) {
		System.out.println("EConTab: FinestraComponentiActivity onClick");
		// TODO Auto-generated method stub
		if (v instanceof EConTabItemMenu) {
			mViewPager.setCurrentItem(Integer.parseInt(v.getTag().toString()));
		}
	}
}
