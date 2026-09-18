package pfa.app.econtab;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.utils.Utility;

public abstract class EConTabDettaglioActivity extends EConTabActivity {
	public static final String SALVATAGGIO_OK = "OK";

	public static final int INSERIMENTO = 0;
	public static final int MODIFICA = 1;

	private int modalita = INSERIMENTO;

    private boolean chiudiAlSalvataggio = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: EConTabDettaglioActivity onCreate ENTER");
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
		System.out.println("EConTab: EConTabDettaglioActivity onCreate EXIT");
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
			scroll.setBackgroundColor(Color.parseColor("#eeeded"));
		}
	}

	public void annulla(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}

	public void salva(View v) {
		System.out.println("EConTab: EConTabDettaglioActivity salva ENTER");
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
                    EConTabCrash(result);
                }
                else{
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                }

			}
		}
		System.out.println("EConTab: EConTabDettaglioActivity salva EXIT");
	}


    protected void mostraDialogSalvataggio() {
		System.out.println("EConTab: EConTabDettaglioActivity mostraDialogSalvataggio ENTER");
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
                                    EConTabCrash(result);
                                }
                                else{
                                    Toast.makeText(EConTabDettaglioActivity.this, result, Toast.LENGTH_LONG).show();
                                }

                            }
                        }
                    }


                });
		System.out.println("EConTab: EConTabDettaglioActivity mostraDialogSalvataggio EXIT");
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
		System.out.println("EConTab: EConTabDettaglioActivity salvaDettaglio ENTER");
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
		System.out.println("EConTab: EConTabDettaglioActivity salvaDettaglio EXIT");
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
