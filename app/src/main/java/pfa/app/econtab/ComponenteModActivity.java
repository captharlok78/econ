package pfa.app.econtab;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import pfa.app.econtab.adapters.ComposizioneComponenteAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.CategorieComponenti;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.ComponentiCantComposti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComponentiComposti;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.UnitaMisura;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;
import pfa.app.general.simplecropimage.CropImage;

public class ComponenteModActivity extends EConTabDettaglioActivity implements OnItemClickListener {
	private EConTabSpinner spinnerUDM = null;
	private EConTabSpinner spinnerTubo = null;
	private EConTabSpinner spinnerCavo = null;

	private ImageButton icona = null;
	private Switch checkBoxPreferito = null;
	private Switch checkBoxLinea = null;
	private Switch checkBoxTappo = null;
	private EditText editPosti = null;
	private int idCategoria = 0;
	private Bitmap iconaBitmap = null;

	private boolean cancellaIcona = false;
	private String nomeIcona = "";

	private boolean elementoCantiere = false;

	private ListView listaComposizione = null;
	private ArrayList<Object> dati = null;
	private ComposizioneComponenteAdapter adapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ComponenteModActivity onCreate ENTER");
		setContentView(R.layout.activity_componente_mod);

		listaComposizione = (ListView) findViewById(R.id.lista_comp);
		registerForContextMenu(listaComposizione);
		listaComposizione.setOnItemClickListener(this);

		icona = (ImageButton) findViewById(R.id.imageButtonIcona);

		checkBoxPreferito = (Switch) findViewById(R.id.switch_preferito);
		checkBoxLinea = (Switch) findViewById(R.id.switch_linea);
		checkBoxTappo = (Switch) findViewById(R.id.switch_tappo);
		editPosti = (EditText) findViewById(R.id.editText_posti);
		idCategoria = getIntent().getIntExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, 0);

		if (getIntent().getExtras().containsKey("CANTIERE")) {
			elementoCantiere = true;
		}

		super.onCreate(savedInstanceState);

		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(CategorieComponenti.ID_CATEGORIA_COMPONENTE, idCategoria);
		ContentValues recordCat = db.getRecord(new CategorieComponenti(), where);
		db.close();

		if (recordCat != null) {
			setText(R.id.textView_categoria, recordCat.getAsString(CategorieComponenti.NOME));

		}

		if (idCategoria == Componenti.SCATOLE || idCategoria == Componenti.CENTRALINI) {
			findViewById(R.id.linear_cavo).setVisibility(View.GONE);
			findViewById(R.id.linear_tubo).setVisibility(View.VISIBLE);
		}

		if (idCategoria == Componenti.INTERRUTTORI || idCategoria==Componenti.PRESE){

			findViewById(R.id.linear_cavo).setVisibility(View.VISIBLE);
			findViewById(R.id.linear_tubo).setVisibility(View.GONE);

		}

		if (idCategoria == Componenti.COMPONENTI_INTERRUTTORI) {
			findViewById(R.id.linear_tappo).setVisibility(View.GONE);
			findViewById(R.id.linear_cavo).setVisibility(View.GONE);
			findViewById(R.id.linear_tubo).setVisibility(View.GONE);
			findViewById(R.id.linear_altezza).setVisibility(View.INVISIBLE);
			findViewById(R.id.linear_lista_comp).setVisibility(View.GONE);
		}

		if (idCategoria == Componenti.SCATOLE || idCategoria == Componenti.PORTAFRUTTI || idCategoria == Componenti.CENTRALINI) {
			setText(R.id.textView_posti, getString(R.string.posti_ospitati));
		} else {
			setText(R.id.textView_posti, getString(R.string.posti_occupati));
		}
		spinnerUDM = (EConTabSpinner) findViewById(R.id.econtabSpinner_udm);
		spinnerUDM.setTabella(new UnitaMisura());

		spinnerTubo = (EConTabSpinner) findViewById(R.id.econtabSpinner_tubo);
		ContentValues filtroTubi = new ContentValues();
		filtroTubi.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.TUBI);
		spinnerTubo.setFiltro(filtroTubi);
		spinnerTubo.setTabella(new Elementi());

		spinnerCavo = (EConTabSpinner) findViewById(R.id.econtabSpinner_cavo);
		ContentValues filtroCavo = new ContentValues();
		filtroCavo.put(Elementi.ID_CATEGORIA_GENERALE, CategorieGenerali.CAVI);
		spinnerCavo.setFiltro(filtroCavo);
		spinnerCavo.setTabella(new Elementi());
		System.out.println("EConTab: ComponenteModActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: ComponenteModActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		ContentValues val = null;
		if (elementoCantiere == true) {
			where.put(ComponentiCantiere.ID_COMPONENTE_CANT, getIDModifica());
			val = db.getRecord(new ComponentiCantiere(), where);
		} else {
			where.put(Componenti.ID_COMPONENTE, getIDModifica());
			val = db.getRecord(new Componenti(), where);
		}

		db.close();
		if (val != null) {

			if (elementoCantiere == true) {
				setText(R.id.editText_nome, val.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
				setText(R.id.econtabSpinner_udm, val.getAsString(ComponentiCantiere.UNITA_MISURA));
				setText(R.id.econtabSpinner_tubo, val.getAsString(ComponentiCantiere.ID_ELEMENTO_TUBO));
				setText(R.id.econtabSpinner_cavo, val.getAsString(ComponentiCantiere.ID_ELEMENTO_CAVO));
				setText(R.id.editText_metricavo, Utility.formatNumero(val.getAsDouble(ComponentiCantiere.METRI_CAVO_CANT)));
				setText(R.id.editText_metritubo, Utility.formatNumero(val.getAsDouble(ComponentiCantiere.METRI_TUBO_CANT)));
				if (idCategoria == Componenti.SCATOLE || idCategoria == Componenti.PORTAFRUTTI) {
					setText(R.id.editText_posti, val.getAsString(ComponentiCantiere.SPAZI_OSPITATI_CANT));
				} else {
					setText(R.id.editText_posti, Utility.formatNumero(val.getAsDouble(ComponentiCantiere.SPAZI_OCCUPATI_CANT)));
				}

				int idComponente = val.getAsInteger(ComponentiCantiere.ID_COMPONENTE);
				ContentValues whereComp = new ContentValues();
				whereComp.put(Componenti.ID_COMPONENTE, idComponente);

				ContentValues recComp = db.getRecord(new Componenti(), whereComp);
				if (recComp != null) {
					int preferito = recComp.getAsInteger(Componenti.PREFERITO_SN);
					int tappo = recComp.getAsInteger(Componenti.TAPPO_SN);
					int linea = recComp.getAsInteger(Componenti.LINEA_SN);
					if (preferito == 1) {
						checkBoxPreferito.setChecked(true);
					}
					if (tappo == 1) {
						checkBoxTappo.setChecked(true);
						findViewById(R.id.editText_nome).setEnabled(false);
						findViewById(R.id.editText_posti).setEnabled(false);
						findViewById(R.id.econtabSpinner_udm).setEnabled(false);
					}
					if (linea == 1) {
						checkBoxLinea.setChecked(true);
					}
					nomeIcona = recComp.getAsString(Componenti.ICONA);
					if (nomeIcona != null && !nomeIcona.equals("")) {

						icona.setImageBitmap(Utility.getIcona(this, Componenti.PATH_ICONE, nomeIcona));
					}
				}
				findViewById(R.id.textView_preferito).setVisibility(View.GONE);
				checkBoxPreferito.setVisibility(View.GONE);
				checkBoxTappo.setEnabled(false);
				checkBoxLinea.setEnabled(false);
				findViewById(R.id.editText_posti).setEnabled(false);
				findViewById(R.id.econtabSpinner_udm).setEnabled(false);
				((EConTabSpinner) findViewById(R.id.econtabSpinner_udm))
						.setMessaggioDisabilitato(getString(R.string.messaggio_non_modificabile));
			} else {
				setText(R.id.editText_nome, val.getAsString(Componenti.NOME_COMPONENTE));
				setText(R.id.econtabSpinner_udm, val.getAsString(Componenti.UNITA_MISURA));
				setText(R.id.econtabSpinner_tubo, val.getAsString(Componenti.ID_ELEMENTO_TUBO));
				setText(R.id.econtabSpinner_cavo, val.getAsString(Componenti.ID_ELEMENTO_CAVO));
				setText(R.id.editText_metricavo, Utility.formatNumero(val.getAsDouble(Componenti.METRI_CAVO)));
				setText(R.id.editText_metritubo, Utility.formatNumero(val.getAsDouble(Componenti.METRI_TUBO)));
				if (idCategoria == Componenti.SCATOLE || idCategoria == Componenti.PORTAFRUTTI || idCategoria == Componenti.CENTRALINI) {
					setText(R.id.editText_posti, val.getAsString(Componenti.SPAZI_OSPITATI));
				} else {
					setText(R.id.editText_posti, Utility.formatNumero(val.getAsDouble(Componenti.SPAZI_OCCUPATI)));
				}

				int preferito = val.getAsInteger(Componenti.PREFERITO_SN);
				int tappo = val.getAsInteger(Componenti.TAPPO_SN);
				int linea = val.getAsInteger(Componenti.LINEA_SN);
				if (preferito == 1) {
					checkBoxPreferito.setChecked(true);
				}
				if (tappo == 1) {
					checkBoxTappo.setChecked(true);
					findViewById(R.id.editText_nome).setEnabled(false);
					findViewById(R.id.editText_posti).setEnabled(false);
					findViewById(R.id.econtabSpinner_udm).setEnabled(false);
				}
				if (linea == 1) {
					checkBoxLinea.setChecked(true);
				}
				nomeIcona = val.getAsString(Componenti.ICONA);
				if (nomeIcona != null && !nomeIcona.equals("")) {

					icona.setImageBitmap(Utility.getIcona(this, Componenti.PATH_ICONE, nomeIcona));
				}
			}
		}
		System.out.println("EConTab: ComponenteModActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: ComponenteModActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		if (spinnerUDM.getValue().equals("")) {

			spinnerUDM.setError(getString(R.string.errore_selezione_udm));
			return getString(R.string.errore_selezione_udm);
		}

		if (getTesto(R.id.editText_posti).equals("") && idCategoria != Componenti.COMPONENTI_INTERRUTTORI) {
			if (idCategoria == Componenti.SCATOLE || idCategoria == Componenti.PORTAFRUTTI || idCategoria == Componenti.CENTRALINI) {
				editPosti.setError(getString(R.string.errore_posti_ospitati));
				return getString(R.string.errore_posti_ospitati);
			} else {
				editPosti.setError(getString(R.string.errore_posti_occupati));
				return getString(R.string.errore_posti_occupati);
			}

		}

		Componenti comp = new Componenti();

		ContentValues val = comp.getValoriLogInserimento(db);
		val.put(Componenti.UNITA_MISURA, spinnerUDM.getValue());
		val.put(Componenti.ID_CATEGORIA_COMPONENTE, idCategoria);
		val.put(Componenti.NOME_COMPONENTE, getTesto(R.id.editText_nome));
		val.put(Componenti.METRI_CAVO, Utility.formatNumeroDB(getTesto(R.id.editText_metricavo)));
		val.put(Componenti.ID_ELEMENTO_CAVO, Utility.formatNumeroDB(spinnerCavo.getValue()));
		val.put(Componenti.METRI_TUBO, Utility.formatNumeroDB(getTesto(R.id.editText_metritubo)));
		val.put(Componenti.ID_ELEMENTO_TUBO, Utility.formatNumeroDB(spinnerTubo.getValue()));
		if (idCategoria == Componenti.SCATOLE || idCategoria == Componenti.PORTAFRUTTI || idCategoria == Componenti.CENTRALINI) {
			val.put(Componenti.SPAZI_OSPITATI, Utility.formatNumeroDB(getTesto(R.id.editText_posti)));
		} else {
			val.put(Componenti.SPAZI_OCCUPATI, Utility.formatNumeroDB(getTesto(R.id.editText_posti)));
		}

		int preferito = 0;
		if (checkBoxPreferito.isChecked()) {
			preferito = 1;
		}
		int linea = 0;
		if (checkBoxLinea.isChecked()) {
			linea = 1;
		}
		int tappo = 0;
		if (checkBoxTappo.isChecked()) {
			tappo = 1;
		}
		val.put(Componenti.PREFERITO_SN, preferito);
		val.put(Componenti.TAPPO_SN, tappo);
		val.put(Componenti.LINEA_SN, linea);
		if (iconaBitmap != null) {

			val.put(Componenti.ICONA, nomeIcona);
			Utility.salvaIcona(this, Componenti.PATH_ICONE, nomeIcona, iconaBitmap,false,db);
		}

		comp.inserisciRecord(db, val);
		System.out.println("EConTab: ComponenteModActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: ComponenteModActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		if (spinnerUDM.getValue().equals("")) {
			spinnerUDM.setError(getString(R.string.errore_selezione_udm));
			return getString(R.string.errore_selezione_udm);
		}

		if (getTesto(R.id.editText_posti).equals("") && idCategoria != Componenti.COMPONENTI_INTERRUTTORI) {
			if (idCategoria == Componenti.SCATOLE || idCategoria == Componenti.PORTAFRUTTI || idCategoria == Componenti.CENTRALINI) {
				editPosti.setError(getString(R.string.errore_posti_ospitati));
				return getString(R.string.errore_posti_ospitati);
			} else {
				editPosti.setError(getString(R.string.errore_posti_occupati));
				return getString(R.string.errore_posti_occupati);
			}

		}
		if (elementoCantiere == true) {
			ComponentiCantiere compCant = new ComponentiCantiere();
			ContentValues val = compCant.getValoriLogModifica(db);

			val.put(ComponentiCantiere.UNITA_MISURA, spinnerUDM.getValue());
			val.put(ComponentiCantiere.NOME_COMPONENTE_CANT, getTesto(R.id.editText_nome));
			val.put(ComponentiCantiere.METRI_CAVO_CANT, Utility.formatNumeroDB(getTesto(R.id.editText_metricavo)));
			val.put(ComponentiCantiere.ID_ELEMENTO_CAVO, Utility.formatNumeroDB(spinnerCavo.getValue()));
			val.put(ComponentiCantiere.METRI_TUBO_CANT, Utility.formatNumeroDB(getTesto(R.id.editText_metritubo)));
			val.put(ComponentiCantiere.ID_ELEMENTO_TUBO, Utility.formatNumeroDB(spinnerTubo.getValue()));
			if (idCategoria == Componenti.SCATOLE || idCategoria == Componenti.PORTAFRUTTI || idCategoria == Componenti.CENTRALINI) {
				val.put(ComponentiCantiere.SPAZI_OSPITATI_CANT, Utility.formatNumeroDB(getTesto(R.id.editText_posti)));
			} else {
				val.put(ComponentiCantiere.SPAZI_OCCUPATI_CANT, Utility.formatNumeroDB(getTesto(R.id.editText_posti)));
			}

			ContentValues where = new ContentValues();
			where.put(ComponentiCantiere.ID_COMPONENTE_CANT, getIDModifica());
			compCant.aggiornaRecord(db, val, where);
		} else {
			Componenti comp = new Componenti();

			ContentValues val = comp.getValoriLogModifica(db);
			val.put(Componenti.UNITA_MISURA, spinnerUDM.getValue());
			val.put(Componenti.NOME_COMPONENTE, getTesto(R.id.editText_nome));
			val.put(Componenti.METRI_CAVO, Utility.formatNumeroDB(getTesto(R.id.editText_metricavo)));
			val.put(Componenti.ID_ELEMENTO_CAVO, Utility.formatNumeroDB(spinnerCavo.getValue()));
			val.put(Componenti.METRI_TUBO, Utility.formatNumeroDB(getTesto(R.id.editText_metritubo)));
			val.put(Componenti.ID_ELEMENTO_TUBO, Utility.formatNumeroDB(spinnerTubo.getValue()));
			if (idCategoria == Componenti.SCATOLE || idCategoria == Componenti.PORTAFRUTTI || idCategoria == Componenti.CENTRALINI) {
				val.put(Componenti.SPAZI_OSPITATI, getTesto(R.id.editText_posti));
			} else {
				val.put(Componenti.SPAZI_OCCUPATI, Utility.formatNumeroDB(getTesto(R.id.editText_posti)));
			}

			int preferito = 0;
			if (checkBoxPreferito.isChecked()) {
				preferito = 1;
			}
			int linea = 0;
			if (checkBoxLinea.isChecked()) {
				linea = 1;
			}
			int tappo = 0;
			if (checkBoxTappo.isChecked()) {
				tappo = 1;
			}
			val.put(Componenti.PREFERITO_SN, preferito);
			val.put(Componenti.TAPPO_SN, tappo);
			val.put(Componenti.LINEA_SN, linea);
			if (iconaBitmap != null) {

				val.put(Componenti.ICONA, nomeIcona);
				Utility.salvaIcona(this, Componenti.PATH_ICONE, nomeIcona, iconaBitmap,false,db);
			} else {
				if (cancellaIcona) {
					val.put(Componenti.ICONA, "");
				}
			}

			ContentValues where = new ContentValues();
			where.put(Componenti.ID_COMPONENTE, getIDModifica());
			comp.aggiornaRecord(db, val, where);
		}
		System.out.println("EConTab: ComponenteModActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	public void azioniIcona(View v) {
		System.out.println("EConTab: ComponenteModActivity azioniIcona ENTER");
		String[] items = null;

		if (elementoCantiere == true) {
			items = new String[1];
			items[0] = getString(R.string.visualizza);
		} else {
			items = new String[3];
			items[0] = getString(R.string.visualizza);
			items[1] = getString(R.string.modifica);
			items[2] = getString(R.string.elimina);
		}
		Utility.mostraSelezioneDialog(getString(R.string.icona), items, this, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (which == 0) {
					if (iconaBitmap != null) {
						Utility.salvaIcona(ComponenteModActivity.this, Componenti.PATH_ICONE, nomeIcona, iconaBitmap);
					}

					Intent intent = new Intent(ComponenteModActivity.this, ImmagineActivity.class);
					intent.putExtra(ImmagineActivity.IMMAGINE, nomeIcona);
					intent.putExtra(ImmagineActivity.DIRECTORY, Componenti.PATH_ICONE);
					startActivity(intent);

				}
				if (which == 1) {
					Intent intent = new Intent();
					intent.setType("image/*");
					intent.setAction(Intent.ACTION_PICK);

					Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

					Intent chooser = Intent.createChooser(intent, getString(R.string.seleziona_icona));
					chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { takePhotoIntent });

					File fileTemp = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"temp_foto.png");
					takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileTemp));

					startActivityForResult(chooser, 1);
				}

				if (which == 2) {
					Utility.mostraConfermaCancellazioneDialog(ComponenteModActivity.this, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							if (which == DialogInterface.BUTTON_POSITIVE) {
								iconaBitmap = null;
								icona.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.no_image));
								cancellaIcona = true;
							}
							dialog.cancel();
						}
					});

				}
			}

		});
		System.out.println("EConTab: ComponenteModActivity azioniIcona EXIT");
	}

	public void nuovoComponente(View v) {
		System.out.println("EConTab: ComponenteModActivity nuovoComponente ENTER");
		if (getIDModifica() == 0) {

			DbInterno db = new DbInterno(this);
			Componenti componenti = new Componenti();

			ContentValues val = componenti.getValoriLogInserimento(db);
			int id_componente_inserito = val.getAsInteger(Elementi.ID_ELEMENTO);
			String result = eseguiInserimento(db);
			db.close();

			if (result.equals(SALVATAGGIO_OK)) {
				getIntent().putExtra("ID", id_componente_inserito);
				setModalita(MODIFICA);
				Intent intent = new Intent(this, FinestraComponentiActivity.class);
				intent.putExtra(Componenti.ID_COMPONENTE, getIDModifica());
				intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.COMPONENTI_INTERRUTTORI);
				startActivity(intent);
			}

		} else {
			Intent intent = new Intent(this, FinestraComponentiActivity.class);
			if (elementoCantiere) {
				intent.putExtra(ComponentiCantiere.ID_COMPONENTE_CANT, getIDModifica());
			} else {
				intent.putExtra(Componenti.ID_COMPONENTE, getIDModifica());
			}

			intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.COMPONENTI_INTERRUTTORI);
			startActivity(intent);
		}
		System.out.println("EConTab: ComponenteModActivity nuovoComponente EXIT");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("EConTab: ComponenteModActivity onActivityResult ENTER");
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			System.out.println("EConTab: ComponenteModActivity onActivityResult requestCode 1");
			if (resultCode == RESULT_OK) {

                Intent cropIntent = new Intent(this,CropImage.class);

                Uri selectedImage = null;
                File fileTemp = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"temp_foto.png");
                try {
                	selectedImage = data.getData();
                    InputStream inputStream = getContentResolver().openInputStream(data.getData());

                    FileOutputStream fileOutputStream = new FileOutputStream(fileTemp);

                    Utility.copyStream(inputStream, fileOutputStream);
                    fileOutputStream.close();
                    inputStream.close();
                } catch (Exception e) {
					selectedImage = Uri.fromFile(fileTemp);
                    e.printStackTrace();
                }


                // tell CropImage activity to look for image to crop

                cropIntent.putExtra(CropImage.IMAGE_PATH, fileTemp.getPath());
                cropIntent.setData(selectedImage);

                // allow CropImage activity to rescale image
                cropIntent.putExtra(CropImage.SCALE, true);

                // if the aspect ratio is fixed to ratio 3/2
                cropIntent.putExtra(CropImage.ASPECT_X, 0);
                cropIntent.putExtra(CropImage.ASPECT_Y, 0);
				// start the activity - we handle returning in onActivityResult
				try {
					startActivityForResult(cropIntent, 2);
				} catch (Exception e) {

					String[] filePathColumn = { MediaStore.Images.Media.DATA };

					Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
					cursor.moveToFirst();
					int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
					String filePath = cursor.getString(columnIndex);
					cursor.close();

					iconaBitmap = Utility.decodeSampledBitmapFromFile(filePath,
							getResources().getDimensionPixelSize(R.dimen.larghezza_icona),
							getResources().getDimensionPixelSize(R.dimen.larghezza_icona));
					icona.setImageBitmap(iconaBitmap);
					nomeIcona = "ICO_" + System.currentTimeMillis() + ".png";
					System.out.println("EConTab: ComponenteModActivity onActivityResult nomeIcona " + nomeIcona);
				}

			}

		}

		if (requestCode == 2) {
			System.out.println("EConTab: ComponenteModActivity onActivityResult requestCode 2");
			if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(CropImage.IMAGE_PATH);
                if (path == null) {
                    return;
                }
                nomeIcona = "ICO_" + System.currentTimeMillis() + ".png";
                iconaBitmap = BitmapFactory.decodeFile(path);
                icona.setImageBitmap(iconaBitmap);
				System.out.println("EConTab: ComponenteModActivity onActivityResult nomeIcona " + nomeIcona);
			}
		}
		System.out.println("EConTab: ComponenteModActivity onActivityResult EXIT");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		ricerca();
	}

	public void ricerca() {
		System.out.println("EConTab: ComponenteModActivity ricerca ENTER");
		// TODO Auto-generated method stub
		listaComposizione.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
			adapter = new ComposizioneComponenteAdapter(this, dati, R.layout.list_item_composizione_componente);
			adapter.setComponenteCantiere(elementoCantiere);
			listaComposizione.setAdapter(adapter);

		}

		dati.clear();

		DbInterno db = new DbInterno(this);

		String SQL = "";
		if (elementoCantiere) {
			Join j0 = new Join(ComponentiCantComposti.NOME_TABELLA, ComponentiCantiere.NOME_TABELLA);
			j0.addCampiDiJoin(ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO, ComponentiCantiere.ID_COMPONENTE_CANT);

			Join j1 = new Join(ComponentiCantiere.NOME_TABELLA, Componenti.NOME_TABELLA);
			j1.addCampiDiJoin(ComponentiCantiere.ID_COMPONENTE, Componenti.ID_COMPONENTE);

			SQL = "Select " + ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE + "," + ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO
					+ "," + Componenti.ICONA + "," + ComponentiCantiere.NOME_TABELLA + ".*  from " + ComponentiCantComposti.NOME_TABELLA
					+ j0.getSQLJoin() + j1.getSQLJoin() + " where " + ComponentiCantComposti.ID_COMPONENTE_CANT_PADRE + " = "
					+ getIDModifica() + " order by " + ComponentiCantComposti.ID_COMPONENTE_CANT_FIGLIO + " desc ";

		} else {
			Join j0 = new Join(ComponentiComposti.NOME_TABELLA, Componenti.NOME_TABELLA);
			j0.addCampiDiJoin(ComponentiComposti.ID_COMPONENTE_FIGLIO, Componenti.ID_COMPONENTE);

			SQL = "Select " + Componenti.NOME_TABELLA + ".*," + ComponentiComposti.ID_COMPONENTE_COMPOSTO + " from "
					+ ComponentiComposti.NOME_TABELLA + j0.getSQLJoin() + " where " + ComponentiComposti.ID_COMPONENTE_PADRE + " = "
					+ getIDModifica() + " order by " + ComponentiComposti.ID_COMPONENTE_COMPOSTO + " desc ";

		}

		if (!SQL.equals("")) {
			ArrayList<Object> elems = db.eseguiSelect(SQL, null);
			dati.addAll(elems);
		}

		db.close();
		adapter.notifyDataSetChanged();
		_impostaAltezzaListaComposizione();
		System.out.println("EConTab: ComponenteModActivity ricerca EXIT");
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		System.out.println("EConTab: ComponenteModActivity onItemClick");
		// TODO Auto-generated method stub
		ContentValues item = (ContentValues) listaComposizione.getItemAtPosition(position);
		_apriModifica(item);
	}

	private void _apriModifica(ContentValues item) {
		System.out.println("EConTab: ComponenteModActivity _apriModifica ENTER");
		Intent intent = new Intent(this, ComponenteModActivity.class);

		if (elementoCantiere) {
			intent.putExtra("ID", item.getAsInteger(ComponentiCantiere.ID_COMPONENTE_CANT));
			intent.putExtra("CANTIERE", "SI");
		} else {
			intent.putExtra("ID", item.getAsInteger(Componenti.ID_COMPONENTE));
		}

		intent.putExtra(CategorieComponenti.ID_CATEGORIA_COMPONENTE, Componenti.COMPONENTI_INTERRUTTORI);
		apriFinestraModifica(intent, 0);
		System.out.println("EConTab: ComponenteModActivity _apriModifica EXIT");
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		System.out.println("EConTab: ComponenteModActivity onCreateContextMenu ENTER");
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		ContentValues item = (ContentValues) listaComposizione.getItemAtPosition(info.position);
		if (elementoCantiere) {
			menu.setHeaderTitle(item.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT));
		} else {
			menu.setHeaderTitle(item.getAsString(Componenti.NOME_COMPONENTE));
		}
		menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
		menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));
		System.out.println("EConTab: ComponenteModActivity onCreateContextMenu EXIT");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("EConTab: ComponenteModActivity onContextItemSelected");
		// TODO Auto-generated method stub
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		ContentValues val = (ContentValues) listaComposizione.getItemAtPosition(info.position);
		if (item.getItemId() == 1) {
			_apriModifica(val);
		}
		if (item.getItemId() == 2) {
			if (elementoCantiere) {
				confermaCancellazione(new ComponentiCantComposti(), val, true);

			} else {
				confermaCancellazione(new ComponentiComposti(), val, true);
			}

		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void aggiornaDopoCancellazione() {
		System.out.println("EConTab: ComponenteModActivity aggiornaDopoCancellazione");
		// se non ci sono pi� componenti aggiorno il preventivo aggiungendo il componente padre
		/*DbInterno db = new DbInterno(this);
		ComponentiCantComposti tabComp = new ComponentiCantComposti();
		ComposizioniCantiere tabComposizioni = new ComposizioniCantiere();
		ContentValues valElem = tabComposizioni.getElementoCantiereDaComponente(db, getIDModifica());
		if (valElem != null && valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO) != 0) {
			int idPreventivo = valElem.getAsInteger(ElementiCantiere.ID_PREVENTIVO);
			if (tabComp.getComposizioneComponente(db, getIDModifica()).size() == 0) {
				PreventiviDettaglio tabPrev = new PreventiviDettaglio();
				ContentValues whereComp = new ContentValues();
				whereComp.put(ComponentiCantiere.ID_COMPONENTE_CANT, getIDModifica());
				ContentValues componente = db.getRecord(new ComponentiCantiere(), whereComp);
				componente.put(ElementiCantiere.ID_LOCALE, valElem.getAsInteger(ElementiCantiere.ID_LOCALE));
				tabPrev.aggiornaRigaPreventivo(db, idPreventivo, componente, 1, PreventiviDettaglio.MATERIALE, true);
			}
		}

		db.close();*/
		// non ricarico la pagina ma solo la lista dei componenti
		ricerca();

	}

	private void _impostaAltezzaListaComposizione() {

		if (adapter != null) {
			int totalHeight = 0;
			int desiredWidth = MeasureSpec.makeMeasureSpec(listaComposizione.getWidth(), MeasureSpec.UNSPECIFIED);
			View listItem = null;
			for (int i = 0; i < adapter.getCount(); i++) {
				try {
					listItem = adapter.getView(i, listItem, listaComposizione);
					if (i == 0) {
						listItem.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, LayoutParams.WRAP_CONTENT));
					}

					listItem.measure(desiredWidth, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
					totalHeight += listItem.getMeasuredHeight();
				} catch (Exception e) {
					totalHeight += 20;
				}

			}

			ViewGroup.LayoutParams params = listaComposizione.getLayoutParams();
			params.height = totalHeight + (listaComposizione.getDividerHeight() * (adapter.getCount() - 1));

			listaComposizione.setLayoutParams(params);
			listaComposizione.requestLayout();
		}

	}

}
