package pfa.app.econtab.views;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.utils.Utility;

public class EConTabSpinner extends RelativeLayout implements EConTabSpecialView {
	private static class EConTabSpinneClickListener implements OnClickListener {
		private EConTabSpinner spinner = null;

		public EConTabSpinneClickListener(EConTabSpinner spinner) {
			this.spinner = spinner;
		}

		@Override
		public void onClick(View v) {
			spinner.mostraOpzioni();

		}

	}

	public static final String DESCRIZIONE = "DESC";
	public static final String VALORE = "VAL";

	private boolean refresh = false;
	private boolean nuovoInserimento = false;

	private AbstractTable tabella = null;
	private String campoCodice = null;
	private String campoDescrizione = null;
	private String messaggioDisabilitato = null;
	private String codiceSelezionato = "";

	private TextView testo = null;
	private ImageButton button = null;
	private String[] voci = null;
	private ArrayList<Object> dati = null;
	private ContentValues filtro = null;
	private EConTabSpinner spinnerCollegato = null;
	private ContentValues recordSelezionato = null;

	public EConTabSpinner(Context context) {
		// TODO Auto-generated constructor stub
		super(context);
		init();
	}

	public EConTabSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();

	}

	private void init() {

		View v = inflate(getContext(), R.layout.econtab_spinner_layout, null);
		addView(v);
		if (!isInEditMode()) {
			testo = (TextView) findViewById(R.id.textView1);
			button = (ImageButton) findViewById(R.id.button1);

			EConTabSpinneClickListener listener = new EConTabSpinneClickListener(this);
			button.setOnClickListener(listener);
			testo.setOnClickListener(listener);
		}
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		// TODO Auto-generated method stub
		super.onWindowVisibilityChanged(visibility);
		if (visibility == View.VISIBLE && nuovoInserimento == true) {
			nuovoInserimento = false;
			mostraOpzioni();
		}
	}

	public void mostraOpzioni() {
		if (!isEnabled()) {
			if (getMessaggioDisabilitato() != null) {
				Toast.makeText(getContext(), getMessaggioDisabilitato(), Toast.LENGTH_LONG).show();
			}
			return;
		}
		if (refresh) {
			refresh = false;
			setTabella(tabella, campoCodice, campoDescrizione);
		}

		Utility.mostraSelezioneDialog("", voci, getContext(), new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (which == 0 && tabella != null) {
					refresh = true;
					nuovoInserimento = true;
					Intent intent = new Intent(getContext(), tabella.getDettaglioActivity());
					if (getFiltro() != null) {
						ContentValues filtro = getFiltro();
						Iterator<String> iterFiltro = filtro.keySet().iterator();
						while (iterFiltro.hasNext()) {
							String campo = iterFiltro.next();
							intent.putExtra(campo, filtro.getAsString(campo));
						}

					}
					getContext().startActivity(intent);
				} else {
					codiceSelezionato = "";
					setRecordSelezionato(null);
					if (tabella != null) {
						if (which > 1) {
							codiceSelezionato = ((ContentValues) dati.get(which - 2)).getAsString(campoCodice);
							setRecordSelezionato((ContentValues) dati.get(which - 2));
						}
					} else {
						// caso spinner non collegato ad una tabella
						codiceSelezionato = ((ContentValues) dati.get(which)).getAsString("VAL");
						setRecordSelezionato((ContentValues) dati.get(which));
					}

					testo.setText(voci[which]);
					setError(null);
					if (getSpinnerCollegato() != null) {
						setSpinnerCollegato(getSpinnerCollegato());
					}
				}
			}
		});

	}

	public AbstractTable getTabella() {
		return tabella;
	}

	public void setTabella(AbstractTable tabella) {
		setTabella(tabella, tabella.getCampoCodicePerSpinner(), tabella.getCampoDescrizionePerSpinner());
	}

	/**
	 * Imposta la tabella relativa allo spinner <br>
	 * NB: chiamare questa funzione dopo aver impostato il valore con setValue() se si vuole che al valore venga
	 * aggiunta l'eventuale descrizione
	 * 
	 * @param tabella
	 * @param campoCodice
	 * @param campoDescrizione
	 */
	public void setTabella(AbstractTable tabella, String campoCodice, String campoDescrizione) {

		this.tabella = tabella;
		this.campoCodice = campoCodice;
		this.campoDescrizione = campoDescrizione;
		DbInterno db = new DbInterno(getContext());
		if (tabella.getSQLPerSpinner(getFiltro()).equals("")) {
			dati = db.eseguiSelect(tabella, getFiltro(), new String[] { campoDescrizione });
		} else {
			dati = db.eseguiSelect(tabella.getSQLPerSpinner(getFiltro()), null);
		}

		voci = new String[dati.size() + 2];

		voci[0] = "[ + " + getResources().getString(R.string.crea_nuovo).toUpperCase() + " ]";
		voci[1] = "";

		boolean trovato = false;
		for (int i = 0; i < dati.size(); i++) {
			ContentValues val = (ContentValues) dati.get(i);

			voci[i + 2] = tabella.formattaDescrizioneSpinner(val);
			if (codiceSelezionato.equals(val.getAsString(getCampoCodice()))) {
				testo.setText(voci[i + 2]);
				trovato = true;
				setRecordSelezionato(val);
			}

		}

		db.close();

		if (trovato == false && codiceSelezionato.length() > 0) {
			setValue("");

		}

		setSpinnerCollegato(getSpinnerCollegato());
	}

	public void setValoriSpinnerLibero(ArrayList<Object> valori) {
		dati = valori;
		voci = new String[valori.size()];

		boolean trovato = false;
		for (int i = 0; i < valori.size(); i++) {
			ContentValues val = (ContentValues) valori.get(i);
			voci[i] = val.getAsString(DESCRIZIONE);
			if (codiceSelezionato.equals(val.getAsString(VALORE))) {
				testo.setText(voci[i]);
				trovato = true;
				setRecordSelezionato(val);
			}

		}
		if (trovato == false && codiceSelezionato.length() > 0) {
			setValue("");

		}
	}

	public String getCampoCodice() {
		return campoCodice;
	}

	public void setCampoCodice(String campoCodice) {
		this.campoCodice = campoCodice;
	}

	public String getCampoDescrizione() {
		return campoDescrizione;
	}

	public void setCampoDescrizione(String campoDescrizione) {
		this.campoDescrizione = campoDescrizione;
	}

	public String getValue() {
		return codiceSelezionato;
	}

	public void setValue(String value) {
		codiceSelezionato = value;
		if (codiceSelezionato.equals("")) {
			testo.setText("");
			setRecordSelezionato(null);
		}
	}

	public String getMessaggioDisabilitato() {
		return messaggioDisabilitato;
	}

	public void setMessaggioDisabilitato(String messaggioDisabilitato) {
		this.messaggioDisabilitato = messaggioDisabilitato;
	}

	public ContentValues getFiltro() {
		return filtro;
	}

	public void setFiltro(ContentValues filtro) {
		this.filtro = filtro;
	}

	public void setFiltroEAggiorna(ContentValues filtro) {
		setFiltro(filtro);
		setTabella(getTabella(), getCampoCodice(), getCampoDescrizione());
	}

	public EConTabSpinner getSpinnerCollegato() {
		return spinnerCollegato;
	}

	public void setSpinnerCollegato(EConTabSpinner spinnerCollegato) {
		this.spinnerCollegato = spinnerCollegato;
		if (spinnerCollegato != null) {

			if (codiceSelezionato.equals("")) {
				spinnerCollegato.setEnabled(false);
				spinnerCollegato.setValue("");
			} else {
				spinnerCollegato.setEnabled(true);
				ContentValues filtroCollegato = new ContentValues();
				filtroCollegato.put(getCampoCodice(), codiceSelezionato);
				spinnerCollegato.setFiltro(filtroCollegato);
			}

			spinnerCollegato.setTabella(spinnerCollegato.getTabella(), spinnerCollegato.getCampoCodice(),
					spinnerCollegato.getCampoDescrizione());

		}
	}

	public void setError(String error) {

		testo.setError(error);

	}

	public void addTextChangeListener(TextWatcher listener) {
		testo.addTextChangedListener(listener);
	}

	public ContentValues getRecordSelezionato() {
		return recordSelezionato;
	}

	public void setRecordSelezionato(ContentValues recordSelezionato) {
		this.recordSelezionato = recordSelezionato;
	}

	@Override
	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		super.setEnabled(enabled);
		if (enabled == false) {
			testo.setTextColor(getResources().getColor(android.R.color.darker_gray));
		} else {
			testo.setTextColor(getResources().getColor(android.R.color.black));
		}
		button.setEnabled(enabled);
	}

	public void setTextSixe(float size) {
		testo.setTextSize(size);
	}

}
