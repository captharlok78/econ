package com.ncfsistemi.econ.form;

import com.ncfsistemi.econ.db.table.AbstractTable;
import com.ncfsistemi.econ.db.table.Cantieri;
import com.ncfsistemi.econ.db.table.Unita;
import com.ncfsistemi.econ.utils.FaIcone;

/** Unità del cantiere (vedi LivelloCantiereForm). */
public class UnitaForm extends LivelloCantiereForm {

	@Override
	public AbstractTable tabella() {
		return new Unita();
	}

	@Override
	protected AbstractTable tabellaPadre() {
		return new Cantieri();
	}

	@Override
	protected String colonnaPadre() {
		return Cantieri.ID_CANTIERE;
	}

	@Override
	protected String icona() {
		return FaIcone.UNITA;
	}

	@Override
	protected String etichetta() {
		return "unità";
	}

	@Override
	protected boolean femminile() {
		return true;
	}
}
