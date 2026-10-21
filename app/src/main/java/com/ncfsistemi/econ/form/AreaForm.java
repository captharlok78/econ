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
}
