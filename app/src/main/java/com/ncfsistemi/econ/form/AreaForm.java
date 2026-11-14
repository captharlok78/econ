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

	/** Livelli proposti: da -3 a 10 (STUDIO_DESIGN_GRAFICA.md §11). */
	public static String[][] vociLivello() {
		String[][] voci = new String[14][];
		for (int n = -3; n <= 10; n++) {
			voci[n + 3] = new String[]{String.valueOf(n), n + " · " + nomeLivello(n)};
		}
		return voci;
	}

	/** -1 Scantinato, 0 Piano terra, 1 Piano 1... */
	public static String nomeLivello(int n) {
		String s = com.ncfsistemi.econ.export.ConformitaXLS.nomeLivello(n);
		return s.substring(0, 1).toUpperCase() + s.substring(1);
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
