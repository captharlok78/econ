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
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.db.table.UnitaMisura;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabSpinner;
import pfa.app.general.simplecropimage.CropImage;

public class ElementoModActivity extends EConTabDettaglioActivity {

	private EConTabSpinner spinnerUDM = null;
	private EConTabSpinner spinnerTubo = null;
	private EConTabSpinner spinnerCavo = null;
	private ImageButton icona = null;
	private Switch checkBoxPreferito = null;
	private Switch checkBoxPlacca = null;
	private int idCategoria = 0;
	private Bitmap iconaBitmap = null;

	private boolean cancellaIcona = false;
	private String nomeIcona = "";

	private boolean elementoCantiere = false;

    private boolean iconaCambiata = false;

	private LinearLayout linearLineaPlacca = null;
	private LinearLayout linearUsaLineaPlaccaPredefinita = null;
	private Switch checkBoxUsaLinePlaccaPredefinita = null;
	private EConTabSpinner spinner_linea = null;
	private EConTabSpinner spinner_placca = null;
	private int idLocale = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ElementoDettaglioActivity onCreate ENTER");
		setContentView(R.layout.activity_elemento_mod);
		icona = (ImageButton) findViewById(R.id.imageButtonIcona);
		linearLineaPlacca = (LinearLayout)findViewById(R.id.linear_linea_placca);
		linearLineaPlacca.setVisibility(View.GONE);
		linearUsaLineaPlaccaPredefinita = (LinearLayout)findViewById(R.id.linear_usa_linea_locale);
		linearUsaLineaPlaccaPredefinita.setVisibility(View.GONE);
		checkBoxPreferito = (Switch) findViewById(R.id.switch_preferito);
		checkBoxPlacca = (Switch) findViewById(R.id.switch_placca);
		checkBoxUsaLinePlaccaPredefinita = (Switch)findViewById(R.id.switch_usa_linea_locale);
		checkBoxUsaLinePlaccaPredefinita.setChecked(true);
		checkBoxPlacca.setChecked(true);


		if (getIntent().getExtras().containsKey("CANTIERE")) {
			elementoCantiere = true;
			linearUsaLineaPlaccaPredefinita.setVisibility(View.VISIBLE);
			if (!checkBoxUsaLinePlaccaPredefinita.isChecked()){
				linearLineaPlacca.setVisibility(View.VISIBLE);
			}
			checkBoxPlacca.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (b){
						linearUsaLineaPlaccaPredefinita.setVisibility(View.VISIBLE);
						if (!checkBoxUsaLinePlaccaPredefinita.isChecked()){
							linearLineaPlacca.setVisibility(View.VISIBLE);
						}
					}
					else {
						linearUsaLineaPlaccaPredefinita.setVisibility(View.GONE);
						linearLineaPlacca.setVisibility(View.GONE);
					}
				}
			});
			checkBoxUsaLinePlaccaPredefinita.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
					if (b){

						linearLineaPlacca.setVisibility(View.GONE);
					}
					else {
						linearLineaPlacca.setVisibility(View.VISIBLE);
					}
				}
			});
		}


		super.onCreate(savedInstanceState);
		idCategoria = getIntent().getIntExtra(CategorieGenerali.ID_CATEGORIA_GENERALE, 0);

		// verifico che non sia una stringa
		if (idCategoria == 0) {
			String idCategoriaStringa = getIntent().getStringExtra(CategorieGenerali.ID_CATEGORIA_GENERALE);
			if (idCategoriaStringa != null) {
				idCategoria = Integer.parseInt(idCategoriaStringa);
			}
		}

		DbInterno db = new DbInterno(this);
		ContentValues where = new ContentValues();
		where.put(CategorieGenerali.ID_CATEGORIA_GENERALE, idCategoria);
		ContentValues recordCat = db.getRecord(new CategorieGenerali(), where);
		db.close();

		if (recordCat != null) {
			setText(R.id.textView_categoria, recordCat.getAsString(CategorieGenerali.NOME));
			// questa schermata � raggiungibile dalla composizione quando sono nel cantiere quindi � inutile
			// rimandare alla composizione

			if (recordCat.getAsInteger(CategorieGenerali.CONFIGURABILE_SN) == 1 && elementoCantiere == false) {
				findViewById(R.id.linear_composizione).setVisibility(View.VISIBLE);
			} else {
				findViewById(R.id.linear_composizione).setVisibility(View.INVISIBLE);
			}

			if (recordCat.getAsInteger(CategorieGenerali.CAVI_SN) != 1) {
				findViewById(R.id.linear_cavo).setVisibility(View.GONE);
			}
			if (recordCat.getAsInteger(CategorieGenerali.TUBO_SN) != 1) {
				findViewById(R.id.linear_tubo).setVisibility(View.GONE);
			}

			if (recordCat.getAsInteger(CategorieGenerali.LINEA_SN) == 1) {
				findViewById(R.id.linear_placca).setVisibility(View.VISIBLE);
				linearUsaLineaPlaccaPredefinita.setVisibility(View.VISIBLE);
			} else {
				findViewById(R.id.linear_placca).setVisibility(View.INVISIBLE);
				linearUsaLineaPlaccaPredefinita.setVisibility(View.INVISIBLE);
			}
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

		// nel caso di tubi o cavi nascondo i campi tubi cavi altezza icona e composizione
		if (idCategoria == CategorieGenerali.CAVI || idCategoria == CategorieGenerali.TUBI) {
			findViewById(R.id.linear_cavo).setVisibility(View.GONE);
			findViewById(R.id.linear_tubo).setVisibility(View.GONE);
			findViewById(R.id.linear_icona_composizione).setVisibility(View.GONE);
			findViewById(R.id.linear_altezza).setVisibility(View.INVISIBLE);
		}

		spinner_linea = (EConTabSpinner) findViewById(R.id.econtabSpinner_linea);
		spinner_placca = (EConTabSpinner) findViewById(R.id.econtabSpinner_placca);
		spinner_linea.setTabella(new Linee());
		spinner_placca.setTabella(new Placche());
		spinner_placca.setMessaggioDisabilitato(getString(R.string.errore_selezione_linea));
		spinner_linea.setSpinnerCollegato(spinner_placca);


		System.out.println("EConTab: ElementoDettaglioActivity onCreate EXIT");
	}

	@Override
	protected void inizializzaModifica() {
		System.out.println("EConTab: ElementoDettaglioActivity inizializzaModifica ENTER");
		// TODO Auto-generated method stub
		super.inizializzaModifica();
		DbInterno db = new DbInterno(this);
		ContentValues val = null;
		ContentValues where = new ContentValues();

		if (elementoCantiere == true) {
			where.put(ElementiCantiere.ID_ELEMENTO_CANT, getIDModifica());
			val = db.getRecord(new ElementiCantiere(), where);
		} else {
			where.put(Elementi.ID_ELEMENTO, getIDModifica());
			val = db.getRecord(new Elementi(), where);

		}

		if (val != null) {

			if (elementoCantiere == true) {
				System.out.println("EConTab: ElementoDettaglioActivity inizializzaModifica elementoCantiere TRUE");
				idLocale = val.getAsInteger(ElementiCantiere.ID_LOCALE);
				setText(R.id.editText_nome, val.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
				setText(R.id.econtabSpinner_udm, val.getAsString(ElementiCantiere.UNITA_MISURA));
				setText(R.id.editText_altezza, val.getAsString(ElementiCantiere.ALTEZZA_DA_TERRA));
				setText(R.id.econtabSpinner_cavo, val.getAsString(ElementiCantiere.ID_ELEMENTO_CAVO));
				setText(R.id.econtabSpinner_tubo, val.getAsString(ElementiCantiere.ID_ELEMENTO_TUBO));
				setText(R.id.editText_metricavo, Utility.formatNumero(val.getAsDouble(ElementiCantiere.METRI_CAVO_CANT)));
				setText(R.id.editText_metritubo, Utility.formatNumero(val.getAsDouble(ElementiCantiere.METRI_TUBO_CANT)));
				setText(R.id.econtabSpinner_linea, val.getAsString(Locali.ID_LINEA));
				setText(R.id.econtabSpinner_placca, val.getAsString(Locali.ID_PLACCA));
				if (!val.getAsString(Locali.ID_LINEA).equals("0")){
					checkBoxUsaLinePlaccaPredefinita.setChecked(false);
				}

				int placca = val.getAsInteger(ElementiCantiere.PLACCA_SN);
				if (placca == 1) {
					checkBoxPlacca.setChecked(true);
				} else {
					checkBoxPlacca.setChecked(false);

				}
				findViewById(R.id.linear_preferito).setVisibility(View.GONE);
				findViewById(R.id.econtabSpinner_udm).setEnabled(false);
				((EConTabSpinner) findViewById(R.id.econtabSpinner_udm))
						.setMessaggioDisabilitato(getString(R.string.messaggio_non_modificabile));
				// findViewById(R.id.editText_nome).setEnabled(false);
				int idELemento = val.getAsInteger(ElementiCantiere.ID_ELEMENTO);
				ContentValues whereElem = new ContentValues();
				whereElem.put(Elementi.ID_ELEMENTO, idELemento);
				ContentValues valElem = db.getRecord(new Elementi(), whereElem);
				if (valElem != null) {
					System.out.println("EConTab: ElementoDettaglioActivity inizializzaModifica valElem!=null");
					nomeIcona = valElem.getAsString(Elementi.ICONA);
					System.out.println("EConTab: ElementoDettaglioActivity inizializzaModifica nomeIcona " + nomeIcona);
					if (nomeIcona != null && !nomeIcona.equals("")) {

						icona.setImageBitmap(Utility.getIcona(this, Elementi.PATH_ICONE, nomeIcona));

					}
                   try{
                       if (nomeIcona.startsWith("ICO_C_")){
                           icona.setVisibility(View.GONE);
                       }
                   }
                   catch(Exception e){

                   }
				}

			} else {
				System.out.println("EConTab: ElementoDettaglioActivity inizializzaModifica elementoCantiere FALSE");
				setText(R.id.editText_nome, val.getAsString(Elementi.NOME_ELEMENTO));
				setText(R.id.econtabSpinner_udm, val.getAsString(Elementi.UNITA_MISURA));
				setText(R.id.editText_altezza, val.getAsString(Elementi.ALTEZZA_DA_TERRA));
				setText(R.id.econtabSpinner_cavo, val.getAsString(Elementi.ID_ELEMENTO_CAVO));
				setText(R.id.econtabSpinner_tubo, val.getAsString(Elementi.ID_ELEMENTO_TUBO));
				setText(R.id.editText_metricavo, Utility.formatNumero(val.getAsDouble(Elementi.METRI_CAVO)));
				setText(R.id.editText_metritubo, Utility.formatNumero(val.getAsDouble(Elementi.METRI_TUBO)));
				int preferito = val.getAsInteger(Elementi.PREFERITO_SN);
				int placca = val.getAsInteger(Elementi.PLACCA_SN);
				if (preferito == 1) {
					checkBoxPreferito.setChecked(true);
				}
				if (placca == 1) {
					checkBoxPlacca.setChecked(true);
				} else {
					checkBoxPlacca.setChecked(false);
				}
				nomeIcona = val.getAsString(Elementi.ICONA);
				if (nomeIcona != null && !nomeIcona.equals("")) {
					System.out.println("EConTab: ElementoDettaglioActivity inizializzaModifica nomeIcona " + nomeIcona);
					icona.setImageBitmap(Utility.getIcona(this, Elementi.PATH_ICONE, nomeIcona));

				}
			}
		}
		db.close();
		System.out.println("EConTab: ElementoDettaglioActivity inizializzaModifica EXIT");
	}

	@Override
	protected String eseguiInserimento(DbInterno db) {
		System.out.println("EConTab: ElementoDettaglioActivity eseguiInserimento ENTER");
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		if (spinnerUDM.getValue().equals("")) {
			spinnerUDM.setError(getString(R.string.errore_selezione_udm));
			return getString(R.string.errore_selezione_udm);
		}

		int preferito = 0;
		if (checkBoxPreferito.isChecked()) {
			preferito = 1;
		}
		System.out.println("EConTab: ElementoDettaglioActivity eseguiInserimento preferito " + preferito);

		int placca = 0;
		if (checkBoxPlacca.isChecked()) {
			placca = 1;
		}
		System.out.println("EConTab: ElementoDettaglioActivity eseguiInserimento placca " + placca);

		String testo = getTesto(R.id.editText_nome);
		System.out.println("EConTab: ElementoDettaglioActivity eseguiInserimento testo " + testo);

		Elementi elementi = new Elementi();

		ContentValues val = elementi.getValoriLogInserimento(db);

		val.put(Elementi.UNITA_MISURA, spinnerUDM.getValue());
		val.put(Elementi.ID_CATEGORIA_GENERALE, idCategoria);
		val.put(Elementi.NOME_ELEMENTO, testo);
		val.put(Elementi.ALTEZZA_DA_TERRA, getTesto(R.id.editText_altezza));
		val.put(Elementi.ID_ELEMENTO_CAVO, Utility.formatNumeroDB(spinnerCavo.getValue()));
		val.put(Elementi.ID_ELEMENTO_TUBO, Utility.formatNumeroDB(spinnerTubo.getValue()));
		val.put(Elementi.METRI_CAVO, Utility.formatNumeroDB(getTesto(R.id.editText_metricavo)));
		val.put(Elementi.METRI_TUBO, Utility.formatNumeroDB(getTesto(R.id.editText_metritubo)));
		val.put(Elementi.PREFERITO_SN, preferito);
		val.put(Elementi.PLACCA_SN, placca);

		if (iconaBitmap != null) {
			System.out.println("EConTab: ElementoDettaglioActivity eseguiInserimento nomeIcona " + nomeIcona);
			val.put(Elementi.ICONA, nomeIcona);
			Utility.salvaIcona(this, Elementi.PATH_ICONE, nomeIcona, iconaBitmap,false,db);
		}

		elementi.inserisciRecord(db, val);
		System.out.println("EConTab: ElementoDettaglioActivity eseguiInserimento EXIT");
		return super.eseguiInserimento(db);
	}

	@Override
	protected String eseguiAggiornamento(DbInterno db) {
		System.out.println("EConTab: ElementoDettaglioActivity eseguiAggiornamento ENTER");
		// TODO Auto-generated method stub
		if (spinnerUDM.getValue().equals("")) {
			spinnerUDM.setError(getString(R.string.errore_selezione_udm));
			return getString(R.string.errore_selezione_udm);
		}
		int preferito = 0;
		if (checkBoxPreferito.isChecked()) {
			preferito = 1;
		}

		int placca = 0;
		String idLinea = "0";
		String idPlacca = "0";
		if (checkBoxPlacca.isChecked()) {
			placca = 1;
			if (!checkBoxUsaLinePlaccaPredefinita.isChecked()){
				idLinea = spinner_linea.getValue();
				idPlacca = spinner_placca.getValue();
			}
		}

		if (elementoCantiere == true) {
			System.out.println("EConTab: ElementoDettaglioActivity eseguiAggiornamento elementoCantiere TRUE");
			ElementiCantiere elementiCantiere = new ElementiCantiere();
			ContentValues val = elementiCantiere.getValoriLogModifica(db);
			val.put(ElementiCantiere.UNITA_MISURA, spinnerUDM.getValue());
			val.put(ElementiCantiere.NOME_ELEMENTO_CANT, getTesto(R.id.editText_nome));
			val.put(ElementiCantiere.ALTEZZA_DA_TERRA, getTesto(R.id.editText_altezza));
			val.put(ElementiCantiere.ID_ELEMENTO_CAVO, Utility.formatNumeroDB(spinnerCavo.getValue()));
			val.put(ElementiCantiere.ID_ELEMENTO_TUBO, Utility.formatNumeroDB(spinnerTubo.getValue()));
			val.put(ElementiCantiere.METRI_CAVO_CANT, Utility.formatNumeroDB(getTesto(R.id.editText_metricavo)));
			val.put(ElementiCantiere.METRI_TUBO_CANT, Utility.formatNumeroDB(getTesto(R.id.editText_metritubo)));

			val.put(ElementiCantiere.PLACCA_SN, placca);
			val.put(ElementiCantiere.ID_LINEA,idLinea);
			val.put(ElementiCantiere.ID_PLACCA,idPlacca);
			ContentValues where = new ContentValues();
			where.put(ElementiCantiere.ID_ELEMENTO_CANT, getIDModifica());
			elementiCantiere.aggiornaRecord(db, val, where);

			PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();

			tabPrevDett.aggiornaRighePreventivoLocale(db, Sessione.getIdPreventivoSelezionato(),idLocale,false);

		} else {
			System.out.println("EConTab: ElementoDettaglioActivity eseguiAggiornamento elementoCantiere FALSE");
			Elementi elementi = new Elementi();

			ContentValues val = elementi.getValoriLogModifica(db);
			val.put(Elementi.UNITA_MISURA, spinnerUDM.getValue());
			val.put(Elementi.NOME_ELEMENTO, getTesto(R.id.editText_nome));
			val.put(Elementi.ALTEZZA_DA_TERRA, getTesto(R.id.editText_altezza));
			val.put(Elementi.ID_ELEMENTO_CAVO, Utility.formatNumeroDB(spinnerCavo.getValue()));
			val.put(Elementi.ID_ELEMENTO_TUBO, Utility.formatNumeroDB(spinnerTubo.getValue()));
			val.put(Elementi.METRI_CAVO, Utility.formatNumeroDB(getTesto(R.id.editText_metricavo)));
			val.put(Elementi.METRI_TUBO, Utility.formatNumeroDB(getTesto(R.id.editText_metritubo)));
			val.put(Elementi.PREFERITO_SN, preferito);
			val.put(Elementi.PLACCA_SN, placca);

			if (iconaBitmap != null) {
				System.out.println("EConTab: ElementoDettaglioActivity eseguiAggiornamento iconaBitmap NOT NULL");
				val.put(Elementi.ICONA, nomeIcona);
				Utility.salvaIcona(this, Elementi.PATH_ICONE, nomeIcona, iconaBitmap,false,db);
			} else {
				if (cancellaIcona) {
					val.put(Elementi.ICONA, "");
				}
			}

			ContentValues where = new ContentValues();
			where.put(Elementi.ID_ELEMENTO, getIDModifica());
			elementi.aggiornaRecord(db, val, where);
		}
		System.out.println("EConTab: ElementoDettaglioActivity eseguiAggiornamento EXIT");
		return super.eseguiAggiornamento(db);
	}

	public void azioniIcona(View v) {
		System.out.println("EConTab: ElementoDettaglioActivity azioniIcona ENTER");
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
						Utility.salvaIcona(ElementoModActivity.this, Elementi.PATH_ICONE, nomeIcona, iconaBitmap,false,null);
					}

					Intent intent = new Intent(ElementoModActivity.this, ImmagineActivity.class);
					intent.putExtra(ImmagineActivity.IMMAGINE, nomeIcona);
					intent.putExtra(ImmagineActivity.DIRECTORY, Elementi.PATH_ICONE);
					startActivity(intent);
					// Intent intent = new Intent();
					// intent.setAction(Intent.ACTION_VIEW);
					// File dirApp =
					// ElementoModActivity.this.getDir(Elementi.PATH_ICONE,
					// Context.MODE_PRIVATE);
					// File fileIco = new File(dirApp, nomeIcona);
					// if (fileIco.exists()) {
					// Uri uri = Uri.parse("content://pfa.app.econtab/" +
					// fileIco.getAbsolutePath());
					// intent.setDataAndType(uri, "image/png");

					// startActivity(intent);
					// }
				}
				if (which == 1) {
					Intent intent = new Intent();
					intent.setType("image/*");
					intent.setAction(Intent.ACTION_PICK);

					Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					Intent chooser = Intent.createChooser(intent, getString(R.string.seleziona_icona));
                    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[] { takePhotoIntent });

					File fileTemp = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"temp_foto.png");
					takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileTemp));

					startActivityForResult(chooser, 1);
				}

				if (which == 2) {
					Utility.mostraConfermaCancellazioneDialog(ElementoModActivity.this, new DialogInterface.OnClickListener() {

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
		System.out.println("EConTab: ElementoDettaglioActivity azioniIcona EXIT");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("EConTab: ElementoDettaglioActivity onActivityResult ENTER");
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			System.out.println("EConTab: ElementoDettaglioActivity onActivityResult requestCode 1");
			if (resultCode == RESULT_OK) {


                Intent cropIntent = new Intent(this,CropImage.class);

				Uri selectedImage = null;
				File fileTemp = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"temp_foto.png");
                try {
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

               /* Uri selectedImage = data.getData();

				Intent cropIntent = new Intent("com.android.camera.action.CROP");
				// indicate image type and Uri of image
				cropIntent.setDataAndType(selectedImage, "image/*");
				// set crop properties
				cropIntent.putExtra("crop", "true");
				// indicate aspect of desired crop
				cropIntent.putExtra("aspectX", 0);
				cropIntent.putExtra("aspectY", 0);
				// indicate output X and Y
				//cropIntent.putExtra("outputX", -1);
				//cropIntent.putExtra("outputY", -1);
				//cropIntent.putExtra("scale", true);

				// retrieve data on return
				cropIntent.putExtra("return-data", true);*/
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
					System.out.println("EConTab: ElementoDettaglioActivity onActivityResult nomeIcona " + nomeIcona);
				}

			}

		}

		if (requestCode == 2) {
			System.out.println("EConTab: ElementoDettaglioActivity onActivityResult requestCode 2");
			if (resultCode == RESULT_OK) {
				//Bundle extras = data.getExtras();
				// get the cropped bitmap from extras
				//iconaBitmap = extras.getParcelable("data");
				//nomeIcona = "ICO_" + System.currentTimeMillis() + ".png";
				// set image bitmap to image view

                String path = data.getStringExtra(CropImage.IMAGE_PATH);
                if (path == null) {

                    return;
                }


                nomeIcona = "ICO_" + System.currentTimeMillis() + ".png";
                iconaBitmap = BitmapFactory.decodeFile(path);
                icona.setImageBitmap(iconaBitmap);
                iconaCambiata = true;
				System.out.println("EConTab: ElementoDettaglioActivity onActivityResult nomeIcona " + nomeIcona);
			}
		}
		System.out.println("EConTab: ElementoDettaglioActivity onActivityResult EXIT");
	}

	public void apriComposizione(View v) {
		System.out.println("EConTab: ElementoDettaglioActivity apriComposizione ENTER");
		if (getIDModifica() == 0) {
			Utility.mostraConfermaDialog(getString(R.string.attenzione), getString(R.string.messaggio_composizione), this, "OK",
					getString(R.string.annulla), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface arg0, int which) {
							// TODO Auto-generated method stub
							if (which == DialogInterface.BUTTON_POSITIVE) {

								DbInterno db = new DbInterno(ElementoModActivity.this);
								Elementi elementi = new Elementi();

								ContentValues val = elementi.getValoriLogInserimento(db);
								int id_elemenento_inserito = val.getAsInteger(Elementi.ID_ELEMENTO);
								String result = eseguiInserimento(db);
								db.close();

								if (result.equals(SALVATAGGIO_OK)) {
									getIntent().putExtra("ID", id_elemenento_inserito);
									setModalita(MODIFICA);
									Class activityComposizione = ComposizioneLiberaActivity.class;
									if (idCategoria == CategorieGenerali.QUADRI) {
										activityComposizione = ComposizioneQuadroActivity.class;
									}
									if (idCategoria == CategorieGenerali.SCATOLE_PRESE || idCategoria==CategorieGenerali.SCATOLE_INTERRUTTORI) {
										activityComposizione = ComposizioneActivity.class;
									}
									Intent intent = new Intent(ElementoModActivity.this, activityComposizione);
									intent.putExtra(Elementi.ID_ELEMENTO, id_elemenento_inserito);
									intent.putExtra(Elementi.NOME_ELEMENTO, getTesto(R.id.editText_nome));
									startActivity(intent);
								}

							}

						}
					});
		} else {
			Class activityComposizione = ComposizioneLiberaActivity.class;
			if (idCategoria == CategorieGenerali.QUADRI) {
				activityComposizione = ComposizioneQuadroActivity.class;
			}
			if (idCategoria == CategorieGenerali.SCATOLE_PRESE || idCategoria==CategorieGenerali.SCATOLE_INTERRUTTORI) {
				activityComposizione = ComposizioneActivity.class;
			}
			Intent intent = new Intent(ElementoModActivity.this, activityComposizione);
			if (elementoCantiere == true) {
				intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, getIDModifica());
			} else {
				intent.putExtra(Elementi.ID_ELEMENTO, getIDModifica());
			}

			intent.putExtra(Elementi.NOME_ELEMENTO, getTesto(R.id.editText_nome));
			startActivity(intent);
		}
		System.out.println("EConTab: ElementoDettaglioActivity apriComposizione EXIT");
	}


    @Override
    protected void onResume() {
		System.out.println("EConTab: ElementoDettaglioActivity onResume ENTER");
        super.onResume();
        if (elementoCantiere == false && getIDModifica()!=0 && !iconaCambiata) {
            DbInterno db = new DbInterno(this);
            ContentValues val = null;
            ContentValues where = new ContentValues();
            where.put(Elementi.ID_ELEMENTO, getIDModifica());
            val = db.getRecord(new Elementi(), where);
            if (val != null) {
                nomeIcona = val.getAsString(Elementi.ICONA);
                if (nomeIcona != null && !nomeIcona.equals("")) {
                    icona.setImageBitmap(Utility.getIcona(this, Elementi.PATH_ICONE, nomeIcona));
                }
            }
        }

        if (iconaCambiata){
            iconaCambiata = false;
        }
		System.out.println("EConTab: ElementoDettaglioActivity onResume EXIT");
    }
}
