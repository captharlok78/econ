package com.ncfsistemi.econ.form;

import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Unita;
import com.ncfsistemi.econ.db.table.Aree;
import com.ncfsistemi.econ.utils.FaIcone;

/** Area del cantiere (vedi LivelloCantiereForm). */
public class AreaForm extends LivelloCantiereForm {

	@Override
	public AbstractTable tabella() {
		return new Aree();
	}

	@Override
	protected AbstractTable tabellaPadre() {
		return new Unita();
	}

	@Override
	protected String colonnaPadre() {
		return Unita.ID_UNITA;
	}

	@Override
	protected String icona() {
		return FaIcone.AREA;
	}

	@Override
	protected String etichetta() {
		return "area";
	}

	@Override
	protected boolean femminile() {
		return true;
	}

	/** Livelli proposti: quelli attivi del catalogo del server (§20; senza catalogo da -3 a 10). */
	public static String[][] vociLivello() {
		return com.ncfsistemi.econ.utils.LivelliAree.voci(0);
	}

	/** Livelli proposti più quello attuale dell'area (anche se disattivato). */
	public static String[][] vociLivello(int attuale) {
		return com.ncfsistemi.econ.utils.LivelliAree.voci(attuale);
	}

	/** Nome del livello dal catalogo: -1 Scantinato, 0 Piano terra, 1 Piano 1... */
	public static String nomeLivello(int n) {
		return com.ncfsistemi.econ.utils.LivelliAree.nome(n);
	}

	@Override
	protected void campiAggiuntivi(Sezione s) {
		s.scelta(Aree.LIVELLO, "Livello (piano)", vociLivello()).obbligatorio();
	}

	@Override
	public void dopoCaricamento(Form f, android.content.ContentValues record, boolean nuovo) {
		super.dopoCaricamento(f, record, nuovo);
		if (nuovo || f.valore(Aree.LIVELLO).isEmpty()) f.imposta(Aree.LIVELLO, "0");
	}
}
