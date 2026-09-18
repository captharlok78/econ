package pfa.app.econtab.views;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import pfa.app.econtab.ComposizioneActivity;
import pfa.app.econtab.ComposizioneLiberaActivity;
import pfa.app.econtab.ComposizioneQuadroActivity;
import pfa.app.econtab.ElementoDettaglioActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Collegamenti;
import pfa.app.econtab.db.table.ComponentiCantiere;
import pfa.app.econtab.db.table.ComposizioniCantiere;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.db.table.PreventiviDettaglio;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;

public class EConTabElementoElettrico extends AbstractEConTabElemento {

	public static final String COLORE_CANTIERE = "#bbf2f2f2";

	public static final String COLORE_PRESE = "#bbee6e6e";
	public static final String COLORE_INTERRUTTORI = "#bb6ed1c5";
	public static final String COLORE_UTILIZZATORI = "#bbeca86f";
	public static final String COLORE_QUADRI = "#bbb870ea";
	public static final String COLORE_SCATOLE = "#bb71e88a";
	public static final String COLORE_ALLARMI = "#bb7388e4";
	public static final String COLORE_AUTOMAZIONI = "#bb31436a";
	public static final String COLORE_CITOFONIA = "#bb518f40";
	public static final String COLORE_TELEFONIA = "#bbfde909";
	public static final String COLORE_VARIO = "#bbe855b8";


	private int idCategoria = 0;
	private int idPreventivo = 0;


	private int idCantiere = 0;
	private int idElementoListino = 0;
	private int bordoPiantina = 0;
	// private ImageView immagine = null;
	private String testoNumero = null;
	private String descrizione = null;
	private RelativeLayout sfondo = null;
	private TextView testoNumeroView = null;
	private TextView testoCollegamentiView = null;
	private TextView testoCollegamentiTubiView = null;
	private int numeroCollegamentiCavi = 0;
	private int numeroCollegamentiTubi = 0;
	private String icona = "";

	private AlertDialog di = null;

	private boolean popupVisibile = false;
	private EConTabPiantinaGridView piantina = null;
	private boolean aggiornaCollegamenti = false;

	private boolean inMovimento = false;

	private boolean abilitaLayout = true;

    private int numeroRelazioni = 0;



    private Paint relazioniPaint = null;
    private RectF rettangoloRelazioni = null;

	private boolean elementoAltroOrdine = false;
	private int idPreventivoSelezionato;
	private int numeroComponentiPreventivoSelezionato;


	public EConTabElementoElettrico(Context context) {
		super(context);
		init();

        relazioniPaint = new Paint();
        relazioniPaint.setStyle(Paint.Style.STROKE);
        int larghezza = (int)context.getResources().getDimensionPixelSize(R.dimen.bordo_piantina);
        relazioniPaint.setStrokeWidth(larghezza);


	}

    @Override
    protected void onDraw(Canvas canvas) {
        if (numeroRelazioni>0) {
            int w = canvas.getWidth();
            float larghezzaTratto = w / 15;
            if (larghezzaTratto < 2) {
                larghezzaTratto = 2;
            }
            relazioniPaint.setStrokeWidth(larghezzaTratto);

            relazioniPaint.setColor(Color.RED);
            canvas.drawLine(w - w / 20 + larghezzaTratto / 2, w - w / 20 - larghezzaTratto / 2, w - w / 4 + larghezzaTratto / 2, w - w / 4 - larghezzaTratto / 2, relazioniPaint);
            relazioniPaint.setColor(Color.parseColor("#354f9a"));
            canvas.drawLine(w - w / 20 - larghezzaTratto / 2, w - w / 20 + larghezzaTratto / 2, w - w / 4 - larghezzaTratto / 2, w - w / 4 + larghezzaTratto / 2, relazioniPaint);
        }
        super.onDraw(canvas);
    }



    private void init() {
		// TODO Auto-generated method stub
		// LayoutInflater li = (LayoutInflater)
		// getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// li.inflate(R.layout.econtab_elemento_elettrico_layout, this, true);
		View v = inflate(getContext(), R.layout.econtab_elemento_elettrico_layout, null);
		addView(v);
		if (!isInEditMode()) {

			// immagine = (ImageView) findViewById(R.id.immagine);

			sfondo = (RelativeLayout) findViewById(R.id.sfondo);
			testoNumeroView = (TextView) findViewById(R.id.textView_testo);
			testoCollegamentiView = (TextView) findViewById(R.id.textView_collegamenti);
			testoCollegamentiTubiView = (TextView) findViewById(R.id.textView_collegamenti_tubi);
			setBackgroundResource(R.drawable.bg_elemento_elettrico_bordo_trasparente);

			// bordoPiantina =
			// getResources().getDimensionPixelSize(R.dimen.bordo_piantina);

		}

	}

	@Override
	public void ricalcolaPosizione() {
		// TODO Auto-generated method stub
		// immagine.getLayoutParams().width = getLarghezzaPiantina() / 15;
		// immagine.getLayoutParams().height = getAltezzaPiantina() / 15;

		int dimensioneMinima = (int) (getResources().getDimensionPixelSize(R.dimen.altezza_campo) / 1.5);
		int maxPixelSize = getResources().getDimensionPixelSize(R.dimen.testo_immagine_grande);

		int dimensione = getAltezzaPiantina() / 16;

		if (dimensione < dimensioneMinima) {
			dimensione = dimensioneMinima;
		}


		Utility.autoScaleTextViewTextToHeight(testoCollegamentiView, (int) (dimensione / 1.8), (int) (maxPixelSize / 1.5), 10);
		Utility.autoScaleTextViewTextToHeight(testoCollegamentiTubiView, (int) (dimensione / 1.8), (int) (maxPixelSize / 1.5), 10);

		int dimResult = Utility.autoScaleTextViewTextToHeight(testoNumeroView, (int) (dimensione / 1.1), (int) (maxPixelSize / 0.9), 12);
		if (dimResult > dimensione) {
			dimensione = dimResult;
		}

		sfondo.getLayoutParams().height = dimensione;
		sfondo.getLayoutParams().width = dimensione;

		int dimCollegamenti = (int) (dimensione / 2);
		testoCollegamentiView.getLayoutParams().height = dimCollegamenti;
		testoCollegamentiView.getLayoutParams().width = dimCollegamenti;

		testoCollegamentiTubiView.getLayoutParams().height = dimCollegamenti;
		testoCollegamentiTubiView.getLayoutParams().width = dimCollegamenti;

		// getAltezzaPiantina() / 15);
		super.ricalcolaPosizione();
	}

	@Override
	public void aggiungiInDb(Context context, int idLocale) {
		System.out.println("EConTab: EConTabElementoElettrico aggiungiInDb ENTER");
		// TODO Auto-generated method stub

		super.aggiungiInDb(context, idLocale);
		DbInterno db = new DbInterno(context);
		db.getReadableDatabase().beginTransaction();
		try {
			ContentValues whereElem = new ContentValues();
			int IdElementoListino = getIdElementoListino();
			System.out.println("EConTab: EConTabElementoElettrico aggiungiInDb IdElementoListino " + IdElementoListino);
			whereElem.put(Elementi.ID_ELEMENTO, IdElementoListino);
			ContentValues valElem = db.getRecord(new Elementi(), whereElem);

			ElementiCantiere elementiCantiere = new ElementiCantiere();
			ContentValues val = elementiCantiere.getValoriLogInserimento(db);
			val.put(ElementiCantiere.ID_LOCALE, idLocale);
			val.put(ElementiCantiere.ID_ELEMENTO, IdElementoListino);
			val.put(ElementiCantiere.NOME_ELEMENTO_CANT, valElem.getAsString(Elementi.NOME_ELEMENTO));
			val.put(ElementiCantiere.UNITA_MISURA, valElem.getAsString(Elementi.UNITA_MISURA));
			val.put(ElementiCantiere.ALTEZZA_DA_TERRA, valElem.getAsDouble(Elementi.ALTEZZA_DA_TERRA));
			val.put(ElementiCantiere.PLACCA_SN, valElem.getAsInteger(Elementi.PLACCA_SN));
			if(pfa.app.econtab.Globals.EXTRA_CAVI_TUBI_IN_PREVENTIVO == true) {
				val.put(ElementiCantiere.METRI_CAVO_CANT, 0);
				val.put(ElementiCantiere.METRI_TUBO_CANT, 0);
			}
			else {
				val.put(ElementiCantiere.METRI_CAVO_CANT, valElem.getAsDouble(Elementi.METRI_CAVO));
				val.put(ElementiCantiere.METRI_TUBO_CANT, valElem.getAsDouble(Elementi.METRI_TUBO));
			}
			val.put(ElementiCantiere.ID_ELEMENTO_CAVO, valElem.getAsInteger(Elementi.ID_ELEMENTO_CAVO));
			val.put(ElementiCantiere.ID_ELEMENTO_TUBO, valElem.getAsInteger(Elementi.ID_ELEMENTO_TUBO));
			val.put(ElementiCantiere.ID_PREVENTIVO, getIdPreventivo());
			// se sto aggiungendo un elemento ad un preventivo non scrivo subito l'id cantiere (verrà scritto quando il
			// preventivo sarà consolidato)
			if (getIdPreventivo() == 0) {
				val.put(ElementiCantiere.ID_CANTIERE, getIdCantiere());
			}

			val.put(ElementiCantiere.POS_X, 100);
			val.put(ElementiCantiere.POS_Y, 100);

			val.put(ElementiCantiere.NUMERO_IDENTIFICATIVO, elementiCantiere.getProssimoNumero(db, getIdCantiere()));

			elementiCantiere.inserisciRecord(db, val);

			if (val.getAsInteger(ElementiCantiere.ID_PREVENTIVO)!=0){
				PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();
				tabPrevDett.aggiornaRighePreventivoLocale(db,val.getAsInteger(ElementiCantiere.ID_PREVENTIVO),val.getAsInteger(ElementiCantiere.ID_LOCALE));
			}

			setIdElemento(val.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT));
			db.getReadableDatabase().setTransactionSuccessful();

		} catch (Exception e) {
			Utility.mostraDialog("Errore", Log.getStackTraceString(e), getContext(), "OK");
		} finally {
			db.getReadableDatabase().endTransaction();
			db.close();
		}
		System.out.println("EConTab: EConTabElementoElettrico aggiungiInDb EXIT");
	}

	@Override
	public void aggiornaDb(Context context) {
		// TODO Auto-generated method stub
		super.aggiornaDb(context);
		DbInterno db = new DbInterno(context);
		db.getReadableDatabase().beginTransaction();
		try {
			ElementiCantiere elementi = new ElementiCantiere();
			ContentValues val = elementi.getValoriLogModifica(db);

			val.put(ElementiCantiere.POS_X, getPositionPercentX());
			val.put(ElementiCantiere.POS_Y, getPositionPercentY());

			ContentValues where = new ContentValues();
			where.put(ElementiCantiere.ID_ELEMENTO_CANT, getIdElemento());

			elementi.aggiornaRecord(db, val, where);
			db.getReadableDatabase().setTransactionSuccessful();
		} catch (Exception e) {

		} finally {
			db.getReadableDatabase().endTransaction();
			db.close();
		}

	}

	@Override
	public void cancellaDaDb(Context context) {
		// TODO Auto-generated method stub
		super.cancellaDaDb(context);
		DbInterno db = new DbInterno(context);

		ContentValues where = new ContentValues();
		where.put(ElementiCantiere.ID_ELEMENTO_CANT, getIdElemento());
		ElementiCantiere elementiCantiere = new ElementiCantiere();

		// db.delete(ElementiCantiere.NOME_TABELLA, where);
		// elementiCantiere.eliminaCorrelati(db, where);
		db.getReadableDatabase().beginTransaction();
		try {
			ArrayList<Object> preventiviDaAggiornare = db.eseguiSelect("Select distinct componenti_cantiere.id_preventivo  from componenti_cantiere inner join composizioni_cantiere on componenti_cantiere.id_componente_cant=composizioni_cantiere.id_componente_cant inner join elementi_cantiere on composizioni_cantiere.id_elemento_cant=elementi_cantiere.id_elemento_cant where elementi_cantiere.id_elemento_cant="+getIdElemento()+" and  componenti_cantiere.id_preventivo<>0",null);
			ContentValues val = db.getRecord("Select * from elementi_cantiere where id_elemento_cant="+getIdElemento());
			elementiCantiere.cancellaRecord(db, where);

			PreventiviDettaglio tabPrevDett = new PreventiviDettaglio();

			//poichè alcuni elementi potrebbero essere composti da componenti appartenenti ad ordini diversi allora estraggo prima tutti i preventivi interessati e per ognuno chiamo la funzione di aggiornamento
			if (preventiviDaAggiornare.size()==0){
				tabPrevDett.aggiornaRighePreventivoLocale(db,val.getAsInteger(ElementiCantiere.ID_PREVENTIVO),val.getAsInteger(ElementiCantiere.ID_LOCALE));
			}
			else{
				for (int i=0;i<preventiviDaAggiornare.size();i++){
					tabPrevDett.aggiornaRighePreventivoLocale(db,((ContentValues)preventiviDaAggiornare.get(i)).getAsInteger(ComponentiCantiere.ID_PREVENTIVO),val.getAsInteger(ElementiCantiere.ID_LOCALE));
				}
			}


			db.getReadableDatabase().setTransactionSuccessful();
		} catch (Exception e) {
			Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
		} finally {
			db.getReadableDatabase().endTransaction();
			db.close();
		}

	}

	public int getIdCategoria() {
		return idCategoria;
	}

	public void setIdCategoria(int idCategoria) {
		this.idCategoria = idCategoria;

		if (getIdPreventivo() == 0 || isElementoAltroOrdine()) {
			// sfondo.setBackgroundColor(Color.parseColor(COLORE_CANTIERE));
			if (getIdPreventivo()==0){
				sfondo.setBackgroundResource(R.drawable.elemento_cantiere);
				testoNumeroView.setTextColor(Color.parseColor(Utility.getColoreCategoria(idCategoria)));
			}
			if (isElementoAltroOrdine()){
				sfondo.setBackgroundResource(R.drawable.elemento_altro_ordine);
				testoNumeroView.setTextColor(Color.parseColor(Utility.getColoreCategoria(idCategoria)));
			}


		} else {
			sfondo.setBackgroundResource(Utility.getIdSfondoDaCategoria(idCategoria));
			//per le automazioni metto testo bianco sennò non si legge
			if (idCategoria == 9) {
				testoNumeroView.setTextColor(Color.parseColor("#FFFFFF"));
			}


			// sfondo.setBackgroundColor(Color.parseColor(Utility.getColoreCategoria(idCategoria)));

		}
	}

	public int getIdPreventivo() {
		return idPreventivo;
	}

	public void setIdPreventivo(int idPreventivo) {
		this.idPreventivo = idPreventivo;
	}

	public int getIdCantiere() {
		return idCantiere;
	}

	public void setIdCantiere(int idCantiere) {
		this.idCantiere = idCantiere;
	}

	public int getIdElementoListino() {
		return idElementoListino;
	}

	public void setIdElementoListino(int idElementoListino) {
		this.idElementoListino = idElementoListino;

	}

	public String getIcona() {
		return icona;
	}

	public void setIcona(String icona) {
		this.icona = icona;
		// if (icona != null && icona.trim().length() > 0 &&
		// !icona.equals("null")) {
		// immagine.setImageBitmap(Utility.getIcona(getContext(),
		// Elementi.PATH_ICONE, icona));
		// }
	}

	public void setNumero(String numero) {
		testoNumero = numero;
		testoNumeroView.setText(numero);
	}

	public String getNumero() {
		return testoNumero;
	}

	@Override
	public void impostaBordoSelezione() {
		// TODO Auto-generated method stub
		if (isSelected()) {
			setBackgroundResource(R.drawable.bg_elemento_elettrico_selezionato);
		} else {
			setBackgroundResource(R.drawable.bg_elemento_elettrico_bordo_trasparente);
		}
	}

	public void mostraPopup() {
		/*if (isElementoAltroOrdine()){
			String[] opzioni = new String[2];
			opzioni[0] = getContext().getString(R.string.copia_elemento_su_ordine_corrente_senza_conteggiare);
			//opzioni[1] = getContext().getString(R.string.copia_elemento_su_ordine_corrente_conteggiando);
			opzioni[1] = getContext().getString(R.string.apri_elemento_solo_in_visualizzazione);
			Utility.mostraSelezioneDialog(getContext().getString(R.string.modifiche_ammesse_solo_per_ordine_corrente), opzioni, getContext(), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					if (i == 0) {
						_copiaElemento(false);
					}

					if (i == 1) {
						_apri(getIdElemento());
					}
				}
			});
		}
		else{
			_apri(getIdElemento());

		}*/
		boolean mostraCopia = false;
		if (Sessione.getIdPreventivoSelezionato()!=0 && getIdPreventivo()!=Sessione.getIdPreventivoSelezionato()){
			//verifico se sono su un preventivo non posso modificare un elemento di cantiere o di un altro ordine senza copiarlo
			int idPrevSel = Sessione.getIdPreventivoSelezionato();
			DbInterno db = new DbInterno(getContext());
			ContentValues recPrev = db.getRecord("Select * from preventivi where id_preventivo="+idPrevSel);
			db.close();
			if (recPrev!=null && recPrev.getAsString(Preventivi.TIPO).equals(Preventivi.TIPO_PREVENTIVO)){
				mostraCopia = true;
			}
		}
		if (mostraCopia){
			String[] opzioni = new String[2];
			opzioni[0] = getContext().getString(R.string.copia_elemento_su_preventivo_corrente_senza_conteggiare);
			//opzioni[1] = getContext().getString(R.string.copia_elemento_su_ordine_corrente_conteggiando);
			opzioni[1] = getContext().getString(R.string.apri_elemento_solo_in_visualizzazione);
			Utility.mostraSelezioneDialog(getContext().getString(R.string.modifiche_non_permesse_preventivo), opzioni, getContext(), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					if (i == 0) {
						_copiaElemento(false);
					}

					if (i == 1) {
						_apri(getIdElemento(),true);
					}
				}
			});
		}
		else{
			_apri(getIdElemento(),false);
		}


	}

	private void _copiaElemento(boolean conteggia) {
		ElementiCantiere tabElem = new ElementiCantiere();
		DbInterno db = new DbInterno(getContext());
		tabElem.copiaElementiPreventivo(db,getIdPreventivo(),idPreventivoSelezionato,getIdElemento(),conteggia);
		db.close();
		piantina.aggiornaElementi(idPreventivoSelezionato);
	}

	private void _apri(int idElemento,boolean soloVisualizzazione) {
		piantina.setRefreshElementi(true);
		ContentValues rec = null;
		aggiornaCollegamenti = false;
		DbInterno db = new DbInterno(getContext());

		/*
		 * boolean modificheBloccate = false; if (getIdPreventivo() != 0) { ContentValues wherePrev = new
		 * ContentValues(); wherePrev.put(Preventivi.ID_PREVENTIVO, getIdPreventivo()); ContentValues recPrev =
		 * db.getRecord(new Preventivi(), wherePrev); if (recPrev != null) {
		 *
		 * if (!recPrev.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO)) { modificheBloccate = true; } } }
		 */

		ContentValues where = new ContentValues();
		where.put(CategorieGenerali.ID_CATEGORIA_GENERALE, idCategoria);
		ContentValues recordCat = db.getRecord(new CategorieGenerali(), where);

		ElementiCantiere tabElemCant = new ElementiCantiere();
		Elementi tabElem = new Elementi();

		Join j1 = new Join(ElementiCantiere.NOME_TABELLA, Elementi.NOME_TABELLA);
		j1.addCampiDiJoin(ElementiCantiere.ID_ELEMENTO, Elementi.ID_ELEMENTO);

		Join j2 = new Join(ElementiCantiere.NOME_TABELLA,ElementiCantiere.NOME_TABELLA,Join.LEFT_JOIN);
		j2.setAliasTabella("elementi_cantiere_origine");
		j2.addCampiDiJoin(ElementiCantiere.ID_ELEMENTO_CANT_ORIGINE,ElementiCantiere.ID_ELEMENTO_CANT);

		ArrayList<Object> record = db.eseguiSelect(
				"Select " + tabElemCant.getNomeCampoTabella("*") + ",elementi_cantiere_origine.id_preventivo as id_preventivo_origine," + tabElem.getNomeCampoTabella(Elementi.ICONA) + " from "
						+ ElementiCantiere.NOME_TABELLA + j1.getSQLJoin() + j2.getSQLJoin()+ " where " + ElementiCantiere.NOME_TABELLA+"."+ElementiCantiere.ID_ELEMENTO_CANT + " = "
						+ idElemento, null);
		db.close();
		if (record != null && record.size() > 0) {
			rec = (ContentValues) record.get(0);
		}

		if (recordCat != null && recordCat.getAsInteger(CategorieGenerali.CONFIGURABILE_SN) == 1) {
			// ComposizioneActivity
			Class activityComposizione = ComposizioneLiberaActivity.class;
			if (idCategoria == CategorieGenerali.QUADRI) {
				activityComposizione = ComposizioneQuadroActivity.class;
			}
			if (idCategoria == CategorieGenerali.SCATOLE_PRESE || idCategoria == CategorieGenerali.SCATOLE_INTERRUTTORI) {
				activityComposizione = ComposizioneActivity.class;
			}

			Intent intent = new Intent(getContext(), activityComposizione);
			intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElemento);
			if(pfa.app.econtab.Globals.BUG_FIX_CLICK_ON_ELEMENTO_IN_PIANTINA_DOPO_SYNCRONIZZAZIONE == false) {
				intent.putExtra(Elementi.NOME_ELEMENTO, rec.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
				intent.putExtra("ID_PREVENTIVO_SELEZIONATO", getIdPreventivoSelezionato());
				intent.putExtra("ELEMENTO_ALTRO_ORDINE", soloVisualizzazione);
				if (rec.get("id_preventivo_origine") != null) {
					intent.putExtra("id_preventivo_origine", rec.getAsInteger("id_preventivo_origine"));
				}
			}
			else
			{
				if(rec != null) {
					intent.putExtra(Elementi.NOME_ELEMENTO, rec.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
					intent.putExtra("ID_PREVENTIVO_SELEZIONATO", getIdPreventivoSelezionato());
					intent.putExtra("ELEMENTO_ALTRO_ORDINE", soloVisualizzazione);
					if (rec.get("id_preventivo_origine") != null) {
						intent.putExtra("id_preventivo_origine", rec.getAsInteger("id_preventivo_origine"));
					}
				}
				else {
					intent.putExtra(Elementi.NOME_ELEMENTO, "error");
					intent.putExtra("ID_PREVENTIVO_SELEZIONATO", getIdPreventivoSelezionato());
					intent.putExtra("ELEMENTO_ALTRO_ORDINE", soloVisualizzazione);
					intent.putExtra("id_preventivo_origine", "error");
				}
			}
			getContext().startActivity(intent);
		} else {
			// DettaglioElementoActivity
			Intent intent = new Intent(getContext(), ElementoDettaglioActivity.class);
			intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, idElemento);
			intent.putExtra("ELEMENTO_ALTRO_ORDINE",soloVisualizzazione);
			intent.putExtra("ID_PREVENTIVO_SELEZIONATO",getIdPreventivoSelezionato());
			getContext().startActivity(intent);
		}
	}

	private Spanned _getComponentiConNote(int idElemento) {
		// TODO Auto-generated method stub
		String compTxt = "";
		DbInterno db = new DbInterno(getContext());
		Collegamenti tabCollegamenti = new Collegamenti();

		ArrayList<Integer> componentiCollegati = tabCollegamenti.getComponentiCollegatiElemento(db, idElemento);

		ComposizioniCantiere tabCompos = new ComposizioniCantiere();
		ArrayList<Object> composizioni = tabCompos.getComposizioneElemento(db, idElemento);
		for (int i = 0; i < composizioni.size(); i++) {
			ContentValues currVal = (ContentValues) composizioni.get(i);
			String moduliOccupati = currVal.getAsString(ComposizioniCantiere.MODULI_OCCUPATI_CANT);
			String nota = currVal.getAsString(ComponentiCantiere.NOTA);
			if (nota.trim().length() > 0) {
				nota = " (" + nota + ")";
			}

			if (moduliOccupati.trim().length() > 0) {
				moduliOccupati = moduliOccupati + " - ";
			}
			String collegato = "";
			if (componentiCollegati.contains(currVal.getAsInteger(ComposizioniCantiere.ID_COMPONENTE_CANT))) {
				collegato = " <font color='#0099cc'> <i>" + getContext().getString(R.string.collegato) + "</i></font>";
			}
			compTxt = compTxt + moduliOccupati + currVal.getAsString(ComponentiCantiere.NOME_COMPONENTE_CANT) + nota + collegato + "<br>";
		}
		db.close();
		return Html.fromHtml(compTxt);
	}

	public boolean isPopupVisibile() {
		// TODO Auto-generated method stub
		return popupVisibile;
	}

	public void setPopupVisibile(boolean popupVisibile) {
		this.popupVisibile = popupVisibile;
	}

	public void setNumeroCollegamenti(int collegamentiTotCavi, int collegamentiTotTubi) {
		// TODO Auto-generated method stub
		testoCollegamentiView.setText("" + collegamentiTotCavi);
		testoCollegamentiTubiView.setText("" + collegamentiTotTubi);
		if (collegamentiTotCavi == 0) {
			testoCollegamentiView.setVisibility(View.GONE);

		} else {
			testoCollegamentiView.setVisibility(View.VISIBLE);

		}

		if (collegamentiTotTubi == 0) {

			testoCollegamentiTubiView.setVisibility(View.GONE);
		} else {

			testoCollegamentiTubiView.setVisibility(View.VISIBLE);
		}
		numeroCollegamentiCavi = collegamentiTotCavi;
		numeroCollegamentiTubi = collegamentiTotTubi;

	}

	public int getNumeroCollegamentiCavi() {
		return numeroCollegamentiCavi;
	}

	public int getNumeroCollegamentiTubi() {
		return numeroCollegamentiTubi;
	}

	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public void setPiantina(EConTabPiantinaGridView piantina) {
		// TODO Auto-generated method stub
		this.piantina = piantina;
	}

	public void setInMovimento(boolean b) {
		// TODO Auto-generated method stub
		this.inMovimento = b;
	}

	public boolean isInMovimento() {
		return inMovimento;
	}

	public boolean isAbilitaLayout() {
		return abilitaLayout;
	}

	public void setAbilitaLayout(boolean abilitaLayout) {
		this.abilitaLayout = abilitaLayout;
	}

    public int getNumeroRelazioni() {
        return numeroRelazioni;
    }

    public void setNumeroRelazioni(int numeroRelazioni) {
        this.numeroRelazioni = numeroRelazioni;
    }



	public boolean isElementoAltroOrdine() {
		return getIdPreventivo()!=getIdPreventivoSelezionato() && numeroComponentiPreventivoSelezionato==0;
	}

	public int getIdPreventivoSelezionato() {
		return idPreventivoSelezionato;
	}

	public void setIdPreventivoSelezionato(int idPreventivoSelezionato) {
		this.idPreventivoSelezionato = idPreventivoSelezionato;
	}

	public void setNumeroComponentiPreventivoSelezionato(int numeroComponentiPreventivoSelezionato) {
		this.numeroComponentiPreventivoSelezionato = numeroComponentiPreventivoSelezionato;
	}


	/*
	 * public void mostraPopup() { setPopupVisibile(false);// imposto a false all'inizio per cancellare eventuali popup
	 * pendenti ContentValues rec = null; aggiornaCollegamenti = false; DbInterno db = new DbInterno(getContext());
	 * 
	 * boolean mostraCollegamenti = true; boolean modificheBloccate = false; if (getIdPreventivo() != 0) { ContentValues
	 * wherePrev = new ContentValues(); wherePrev.put(Preventivi.ID_PREVENTIVO, getIdPreventivo()); ContentValues
	 * recPrev = db.getRecord(new Preventivi(), wherePrev); if (recPrev != null) { mostraCollegamenti =
	 * recPrev.getAsString(Preventivi.TIPO).equals(Preventivi.TIPO_ORDINE); if
	 * (!recPrev.getAsString(Preventivi.STATO).equals(Preventivi.STATO_APERTO)) { modificheBloccate = true; } } }
	 * 
	 * ContentValues where = new ContentValues(); where.put(CategorieGenerali.ID_CATEGORIA_GENERALE, idCategoria);
	 * ContentValues recordCat = db.getRecord(new CategorieGenerali(), where);
	 * 
	 * ElementiCantiere tabElemCant = new ElementiCantiere(); Elementi tabElem = new Elementi();
	 * 
	 * Join j1 = new Join(ElementiCantiere.NOME_TABELLA, Elementi.NOME_TABELLA);
	 * j1.addCampiDiJoin(ElementiCantiere.ID_ELEMENTO, Elementi.ID_ELEMENTO);
	 * 
	 * ArrayList<Object> record = db.eseguiSelect( "Select " + tabElemCant.getNomeCampoTabella("*") + "," +
	 * tabElem.getNomeCampoTabella(Elementi.ICONA) + " from " + ElementiCantiere.NOME_TABELLA + j1.getSQLJoin() +
	 * " where " + ElementiCantiere.ID_ELEMENTO_CANT + " = " + getIdElemento(), null); db.close(); if (record != null &&
	 * record.size() > 0) { rec = (ContentValues) record.get(0); }
	 * 
	 * if (rec != null) { final ContentValues recFinal = rec; View campi = View.inflate(getContext(),
	 * R.layout.dialog_numelemento_layout, null); final EditText etichetta = (EditText)
	 * campi.findViewById(R.id.editText_etichetta); final EditText altezza = (EditText)
	 * campi.findViewById(R.id.editText_altezza); TextView labelComposzione = (TextView)
	 * campi.findViewById(R.id.titolo_composizione); final TextView txtComposizione = (TextView)
	 * campi.findViewById(R.id.textView_componenti); final LinearLayout linearComposizione = (LinearLayout)
	 * campi.findViewById(R.id.linearComposizione);
	 * 
	 * final ImageButton buttonModifica = (ImageButton) campi.findViewById(R.id.button_edit_elemento);
	 * 
	 * buttonModifica.setOnClickListener(new OnClickListener() {
	 * 
	 * @Override public void onClick(View v) { // TODO Auto-generated method stub Intent intent = new
	 * Intent(getContext(), ElementoModActivity.class); intent.putExtra("ID", getIdElemento());
	 * intent.putExtra("CANTIERE", "SI"); intent.putExtra(CategorieGenerali.ID_CATEGORIA_GENERALE, idCategoria);
	 * 
	 * getContext().startActivity(intent); di.cancel(); di = null; setPopupVisibile(true);
	 * 
	 * } });
	 * 
	 * final ImageButton buttonNota = (ImageButton) campi.findViewById(R.id.button_nota); final ImageButton
	 * buttonComposizione = (ImageButton) campi.findViewById(R.id.button_composizione_elemento);
	 * 
	 * if (mostraCollegamenti == false) { // i collegamenti sono possibili solo su elementi di ordini o su elementi di
	 * cnatiere (no preventivi) campi.findViewById(R.id.linear_collegamenti).setVisibility(View.GONE); }
	 * 
	 * final Button buttonNuovoTubo = (Button) campi.findViewById(R.id.button_nuovotubo); final Button buttonNuovoCavo =
	 * (Button) campi.findViewById(R.id.button_nuovocavo); final Button buttonNuovoTuboCavo = (Button)
	 * campi.findViewById(R.id.button_nuovotubocavo); final Button buttonVediCollegamenti = (Button)
	 * campi.findViewById(R.id.button_vedicollegamenti);
	 * 
	 * OnClickListener listenerCollegamenti = new OnClickListener() {
	 * 
	 * @Override public void onClick(View v) { aggiornaCollegamenti = true; // TODO Auto-generated method stub if
	 * (v.getId() == R.id.button_vedicollegamenti) { Intent intent = new Intent(getContext(),
	 * CollegamentiElementoActivity.class); intent.putExtra("ID_ELEMENTO", getIdElemento());
	 * getContext().startActivity(intent);
	 * 
	 * } else { Intent intent = new Intent(getContext(), CollegamentoActivity.class);
	 * intent.putExtra(Collegamenti.ID_ORDINE, piantina.getIdPreventivoSelezionato()); intent.putExtra("ID_ELEMENTO",
	 * getIdElemento()); if (v.getId() == R.id.button_nuovotubo) { intent.putExtra("TIPO", CollegamentoActivity.TUBO); }
	 * if (v.getId() == R.id.button_nuovocavo) { intent.putExtra("TIPO", CollegamentoActivity.CAVO); } if (v.getId() ==
	 * R.id.button_nuovotubocavo) { intent.putExtra("TIPO", CollegamentoActivity.TUBO_CAVO); }
	 * 
	 * getContext().startActivity(intent); } } }; buttonNuovoTubo.setOnClickListener(listenerCollegamenti);
	 * buttonNuovoCavo.setOnClickListener(listenerCollegamenti);
	 * buttonNuovoTuboCavo.setOnClickListener(listenerCollegamenti);
	 * buttonVediCollegamenti.setOnClickListener(listenerCollegamenti);
	 * 
	 * if (recordCat != null && recordCat.getAsInteger(CategorieGenerali.CONFIGURABILE_SN) == 1) {
	 * buttonNota.setVisibility(View.GONE); txtComposizione.setText(_getComponentiConNote(getIdElemento()));
	 * buttonComposizione.setOnClickListener(new OnClickListener() {
	 * 
	 * @Override public void onClick(View v) {
	 * 
	 * // TODO Auto-generated method stub Class activityComposizione = null; if (idCategoria == 4) {
	 * activityComposizione = ComposizioneQuadroActivity.class; } else { activityComposizione =
	 * ComposizioneActivity.class; } Intent intent = new Intent(getContext(), activityComposizione);
	 * 
	 * intent.putExtra(ElementiCantiere.ID_ELEMENTO_CANT, getIdElemento());
	 * 
	 * intent.putExtra(Elementi.NOME_ELEMENTO, recFinal.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
	 * 
	 * getContext().startActivity(intent); di.cancel(); di = null; setPopupVisibile(true);
	 * 
	 * } }); } else { labelComposzione.setText(getContext().getString(R.string.note));
	 * txtComposizione.setText(rec.getAsString(ElementiCantiere.NOTA)); if
	 * (txtComposizione.getText().toString().equals("")) { linearComposizione.setVisibility(View.GONE); }
	 * buttonComposizione.setVisibility(View.GONE); final View campiNota = View.inflate(getContext(),
	 * R.layout.dialog_note_layout, null); final EditText campoNota = (EditText)
	 * campiNota.findViewById(R.id.editText_nota); campoNota.setText(recFinal.getAsString(ElementiCantiere.NOTA)); final
	 * DialogInterface.OnClickListener listenerNota = new DialogInterface.OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int which) { // TODO Auto-generated method stub if (which
	 * == DialogInterface.BUTTON_POSITIVE) { DbInterno db = new DbInterno(getContext()); ElementiCantiere tabElem = new
	 * ElementiCantiere(); ContentValues where = new ContentValues(); where.put(ElementiCantiere.ID_ELEMENTO_CANT,
	 * recFinal.getAsInteger(ElementiCantiere.ID_ELEMENTO_CANT)); ContentValues valUpd =
	 * tabElem.getValoriLogModifica(db); valUpd.put(ElementiCantiere.NOTA, campoNota.getText().toString());
	 * tabElem.aggiornaRecord(db, valUpd, where); db.close(); recFinal.put(ElementiCantiere.NOTA,
	 * campoNota.getText().toString());
	 * 
	 * txtComposizione.setText(campoNota.getText().toString()); if (txtComposizione.getText().toString().equals("")) {
	 * linearComposizione.setVisibility(View.GONE); } else { linearComposizione.setVisibility(View.VISIBLE); } }
	 * 
	 * ((ViewGroup) campiNota.getParent()).removeView(campiNota); } }; buttonNota.setOnClickListener(new
	 * OnClickListener() {
	 * 
	 * @Override public void onClick(View v) { // TODO Auto-generated method stub
	 * Utility.mostraDialogPersonalizzato(recFinal.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT), getContext(),
	 * campiNota, getContext().getString(R.string.conferma), getContext().getString(R.string.annulla), listenerNota);
	 * 
	 * } });
	 * 
	 * }
	 * 
	 * DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int which) { // TODO Auto-generated method stub DbInterno
	 * db = new DbInterno(getContext()); if (which == Dialog.BUTTON_POSITIVE) {
	 * 
	 * ContentValues val = new ContentValues(); val.put(ElementiCantiere.NUMERO_IDENTIFICATIVO,
	 * etichetta.getText().toString()); setNumero(etichetta.getText().toString());
	 * 
	 * String altezzaTxt = altezza.getText().toString(); if (altezzaTxt.equals("")) { altezzaTxt = "0"; }
	 * 
	 * val.put(ElementiCantiere.ALTEZZA_DA_TERRA, Double.parseDouble(altezzaTxt)); ContentValues where = new
	 * ContentValues(); where.put(ElementiCantiere.ID_ELEMENTO_CANT, getIdElemento()); ElementiCantiere tabElementi =
	 * new ElementiCantiere(); tabElementi.aggiornaRecord(db, val, where);
	 * 
	 * }
	 * 
	 * String SQL_COLL = "Select count(*) as numero from " + Collegamenti.NOME_TABELLA + " where " +
	 * Collegamenti.ID_ELEMENTO_CANT1 + "=" + getIdElemento() + " or " + Collegamenti.ID_ELEMENTO_CANT2 + "=" +
	 * getIdElemento(); ArrayList<Object> res = db.eseguiSelect(SQL_COLL, null); ContentValues conta = (ContentValues)
	 * res.get(0); setNumeroCollegamenti(conta.getAsInteger("numero"));
	 * 
	 * db.close(); if (piantina != null && aggiornaCollegamenti) { piantina.refreshElementi(); } } };
	 * 
	 * if (modificheBloccate) { buttonNota.setVisibility(View.INVISIBLE);
	 * buttonComposizione.setVisibility(View.INVISIBLE); buttonModifica.setVisibility(View.INVISIBLE);
	 * altezza.setEnabled(false); etichetta.setEnabled(false);
	 * campi.findViewById(R.id.textViewMessaggio).setVisibility(View.VISIBLE); } else {
	 * campi.findViewById(R.id.textViewMessaggio).setVisibility(View.GONE); }
	 * 
	 * AlertDialog.Builder ab = new AlertDialog.Builder(getContext());
	 * 
	 * ab.setPositiveButton(getContext().getString(R.string.conferma), listener);
	 * ab.setNegativeButton(getContext().getString(R.string.annulla), listener);
	 * 
	 * ab.setView(campi);
	 * 
	 * ab.setTitle(rec.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT).toUpperCase(Locale.getDefault()));
	 * 
	 * ab.setIcon(new BitmapDrawable(getResources(), Utility.getIconaScalata(getContext(), Elementi.PATH_ICONE,
	 * rec.getAsString(Elementi.ICONA)))); etichetta.setText(rec.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO));
	 * altezza.setText(rec.getAsString(ElementiCantiere.ALTEZZA_DA_TERRA));
	 * 
	 * di = ab.create();
	 * 
	 * di.show();
	 * 
	 * }
	 * 
	 * }
	 */
}
