package com.ncfsistemi.econ;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ncfsistemi.econ.db.DbInterno;
import com.ncfsistemi.econ.utils.FunzionalitaApp;
import com.ncfsistemi.econ.utils.Utility;

public abstract class EconDettaglioActivity extends EconActivity {
	public static final String SALVATAGGIO_OK = "OK";

	public static final int INSERIMENTO = 0;
	public static final int MODIFICA = 1;

	private int modalita = INSERIMENTO;

    private boolean chiudiAlSalvataggio = true;

	/** Record esistente senza la funzionalita' MODIFICA del modulo (RUOLI_E_CRUD.md): si consulta soltanto. */
	private boolean solaLettura = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Econ: EconDettaglioActivity onCreate ENTER");
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		if (getActionBar() != null) getActionBar().hide();

		if (getIDModifica() != 0) {
			setModalita(MODIFICA);
			inizializzaModifica();
		} else {
			// provo a vedere se � una stringa
			if (getIDModificaStringa() != null) {
				setModalita(MODIFICA);
				inizializzaModifica();
			} else {
				setModalita(INSERIMENTO);
			}

		}

		TextView headerTitolo = findViewById(R.id.headerTitolo);
		if (headerTitolo != null && getTitoloDettaglio() != null) {
			headerTitolo.setText(getTitoloDettaglio());
		}
		System.out.println("Econ: EconDettaglioActivity onCreate EXIT");
	}

	/**
	 * Dopo onCreate del modulo (campi gia' valorizzati): un record esistente senza la funzionalita' MODIFICA del modulo
	 * (moduloFunzionalita()) va in sola lettura: campi disattivati e niente Salva. Il server rifiuterebbe comunque la
	 * modifica nella sincronizzazione.
	 */
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (getModalita() == MODIFICA && !FunzionalitaApp.puoModificare(this, moduloFunzionalita())) {
			solaLettura = true;
			disattivaCampi(findViewById(android.R.id.content));
			View salva = findViewById(R.id.button_salva);
			if (salva != null) {
				salva.setVisibility(View.GONE);
			}
			Toast.makeText(this, getString(R.string.sola_lettura_licenza), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Modulo delle funzionalita' CRUD della maschera (FunzionalitaApp.CLIENTI, ...), da sovrascrivere; null = nessun
	 * controllo (le maschere con regole proprie, es. il rapportino, le applicano da sole).
	 */
	protected String moduloFunzionalita() {
		return null;
	}

	/** La maschera e' in sola lettura per la licenza (vedi onPostCreate). */
	protected boolean isSolaLettura() {
		return solaLettura;
	}

	private static void disattivaCampi(View v) {
		if (v instanceof EditText || v instanceof Spinner || v instanceof CompoundButton) {
			v.setEnabled(false);
		}
		if (v instanceof ViewGroup) {
			ViewGroup g = (ViewGroup) v;
			for (int i = 0; i < g.getChildCount(); i++) {
				disattivaCampi(g.getChildAt(i));
			}
		}
	}

	/**
	 * Titolo mostrato nella testata standard (header_dettaglio_standard), da sovrascrivere
	 * nel modulo - tipicamente "Nuovo X"/"Modifica X" in base a getModalita(). Se il layout
	 * del modulo non include quella testata (headerTitolo assente) o il modulo non
	 * sovrascrive questo metodo (torna null), non succede nulla: l'adozione e' incrementale.
	 */
	protected String getTitoloDettaglio() {
		return null;
	}

	protected boolean nascondiTastiera() {
		// TODO Auto-generated method stub
		// if (getIDModifica() == 0) {
		return false;
		// }
		// return super.nascondiTastiera();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);

		View scroll = findViewById(R.id.scroll);
		if (scroll != null) {
			scroll.setBackgroundColor(Color.parseColor("#F5F5F5"));
		}
	}

	public void annulla(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}

	public void salva(View v) {
		System.out.println("Econ: EconDettaglioActivity salva ENTER");
		if (solaLettura) {
			Toast.makeText(this, getString(R.string.sola_lettura_licenza), Toast.LENGTH_LONG).show();
			return;
		}
		if (getMessaggioConfermaSalvataggio() != null) {
			mostraDialogSalvataggio();
		} else {
			String result = salvaDettaglio();

			if (result.equals(SALVATAGGIO_OK)) {
                if (isChiudiAlSalvataggio()) {
                    setResult(RESULT_OK);
                    finish();
                }
                else{
                    aggiornaDopoSalvataggio();
                }
			} else {

                if (result.startsWith("ERR")){
                    EconCrash(result);
                }
                else{
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                }

			}
		}
		System.out.println("Econ: EconDettaglioActivity salva EXIT");
	}


    protected void mostraDialogSalvataggio() {
		System.out.println("Econ: EconDettaglioActivity mostraDialogSalvataggio ENTER");
        Utility.mostraConfermaDialog(getString(R.string.attenzione), getMessaggioConfermaSalvataggio(), this, "OK",
                getString(R.string.annulla), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            String result = salvaDettaglio();

                            if (result.equals(SALVATAGGIO_OK)) {
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                if (result.startsWith("ERR")){
                                    EconCrash(result);
                                }
                                else{
                                    Toast.makeText(EconDettaglioActivity.this, result, Toast.LENGTH_LONG).show();
                                }

                            }
                        }
                    }


                });
		System.out.println("Econ: EconDettaglioActivity mostraDialogSalvataggio EXIT");
    }




    protected void aggiornaDopoSalvataggio() {

    }

    public String getMessaggioConfermaSalvataggio() {
		return null;
	}

	/**
	 * da sovrascrivere
	 */
	protected void inizializzaModifica() {

	}

	private String salvaDettaglio() {
		System.out.println("Econ: EconDettaglioActivity salvaDettaglio ENTER");
		String result = SALVATAGGIO_OK;
		DbInterno db = new DbInterno(this);
		db.getReadableDatabase().beginTransaction();
		try {
			if (getModalita() == INSERIMENTO) {
				result = eseguiInserimento(db);
			} else {
				result = eseguiAggiornamento(db);
			}
			db.getReadableDatabase().setTransactionSuccessful();
		} catch (Exception e) {
			// TODO: handle exception
			if (e.getMessage() == null) {
				result = "ERR_"+Log.getStackTraceString(e);
			} else {
				result = "ERR_"+e.getMessage();
			}

		} finally {
			db.getReadableDatabase().endTransaction();
			db.close();
		}
		System.out.println("Econ: EconDettaglioActivity salvaDettaglio EXIT");
		return result;
	}

	/**
	 * da sovrascrivere
	 * 
	 * @return
	 */
	protected String eseguiInserimento(DbInterno db) {
		return SALVATAGGIO_OK;
	}

	/**
	 * da sovrascrivere
	 * 
	 * @return
	 */
	protected String eseguiAggiornamento(DbInterno db) {
		return SALVATAGGIO_OK;
	}

	public int getModalita() {
		return modalita;
	}

	public void setModalita(int modalita) {
		this.modalita = modalita;
	}



	public String getIDModificaStringa() {
		return getIntent().getStringExtra("ID");
	}

	public int getIDModifica() {
		return getIntent().getIntExtra("ID", 0);
	}

    public boolean isChiudiAlSalvataggio() {
        return chiudiAlSalvataggio;
    }

    public void setChiudiAlSalvataggio(boolean chiudiAlSalvataggio) {
        this.chiudiAlSalvataggio = chiudiAlSalvataggio;
    }
}
