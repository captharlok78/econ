package com.ncfsistemi.econ.form;

import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Aree;
import com.ncfsistemi.econ.db.table.Locali;
import com.ncfsistemi.econ.utils.FaIcone;

/** Locale del cantiere (vedi LivelloCantiereForm). */
public class LocaleForm extends LivelloCantiereForm {

	@Override
	public AbstractTable tabella() {
		return new Locali();
	}

	@Override
	protected AbstractTable tabellaPadre() {
		return new Aree();
	}

	@Override
	protected String colonnaPadre() {
		return Aree.ID_AREA;
	}

	@Override
	protected String icona() {
		return FaIcone.LOCALE;
	}

	@Override
	protected String etichetta() {
		return "locale";
	}

	@Override
	protected boolean femminile() {
		return false;
	}
}
