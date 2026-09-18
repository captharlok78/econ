package pfa.app.econtab.utils;

import android.content.ContentValues;


/**
 * Classe wrapper per i campi di tipo AutocompleteEditText
 * Contiene un ContentValues con ID e DESCRIZIONE
 * L'id serve per sapere la chiave del record selezionato
 * il metodo toString ritorna la descrizione che è quella mostrata nella lista
 * @author Daniele
 *
 */
public class EConTabAutoCompleteContentValue
{
	private ContentValues contentValue;
	private String campoDescrizione;

	public EConTabAutoCompleteContentValue(ContentValues cv,String campodescrizione){
		this.contentValue = cv;
		this.campoDescrizione = campodescrizione;
	}
	
	public ContentValues getContentValue() {
		return contentValue;
	}

	public void setContentValue(ContentValues contentValue) {
		this.contentValue = contentValue;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return contentValue.getAsString(campoDescrizione);
	}	
}
