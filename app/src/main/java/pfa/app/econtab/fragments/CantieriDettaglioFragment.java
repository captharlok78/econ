package pfa.app.econtab.fragments;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.Join;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.utils.Utility;

public class CantieriDettaglioFragment extends EConTabFragment {
	private int cantiere = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_cantieri_dettaglio, container, false);
		cantiere = getArguments().getInt(Cantieri.ID_CANTIERE);
		v.findViewById(R.id.buttonMappa).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String indirizzo = getTesto(R.id.editText_indirizzo) + "," + getTesto(R.id.editText_citta);

				Utility.mostraMappa(getActivity(), indirizzo);
			}
		});

		return v;

	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		refresh();
	}

	public void refresh() {
		DbInterno db = new DbInterno(getActivity());

		Cantieri cant = new Cantieri();
		Anagrafica ana = new Anagrafica();
		Linee linee = new Linee();
		Placche placche = new Placche();
        Costruttori costruttori = new Costruttori();

		Join join1 = new Join(cant.getNomeTabella(), ana.getNomeTabella());
		join1.addCampiDiJoin(Cantieri.ID_ANAGRAFICA, Anagrafica.ID_ANAGRAFICA);

		Join join2 = new Join(cant.getNomeTabella(), linee.getNomeTabella(), Join.LEFT_JOIN);
		join2.addCampiDiJoin(Cantieri.ID_LINEA, Linee.ID_LINEA);

		Join join3 = new Join(cant.getNomeTabella(), placche.getNomeTabella(), Join.LEFT_JOIN);
		join3.addCampiDiJoin(Cantieri.ID_PLACCA, Placche.ID_PLACCA);

        Join join4 = new Join(Linee.NOME_TABELLA, Costruttori.NOME_TABELLA, Join.LEFT_JOIN);
        join4.addCampiDiJoin(Linee.ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);

		String SQL = "Select " + cant.getNomeCampoTabella("*") + "," + ana.getNomeCampoTabella(Anagrafica.RAGIONE_SOCIALE) + ","+costruttori.getNomeCampoTabella(Costruttori.RAGIONE_SOCIALE)+" as ragsoc_costruttore,"
				+ linee.getNomeCampoTabella(Linee.NOME_LINEA) + "," + placche.getNomeCampoTabella(Placche.NOME_PLACCA) + " from "
				+ cant.getNomeTabella() + join1.getSQLJoin() + join2.getSQLJoin() + join3.getSQLJoin() + join4.getSQLJoin()+ " where "
				+ cant.getNomeCampoTabella(Cantieri.ID_CANTIERE) + " = " + cantiere;

		ArrayList<Object> records = db.eseguiSelect(SQL, null);

		ContentValues val = null;

		db.close();

		if (records.size() > 0) {
			val = (ContentValues) records.get(0);
		}
		View v = getView();
		if (val != null) {
			setText(R.id.editText_ragionesociale, val.getAsString(Anagrafica.RAGIONE_SOCIALE), v);
			setText(R.id.editText_cantiere, val.getAsString(Cantieri.NOME), v);
			setText(R.id.editText_indirizzo, val.getAsString(Cantieri.INDIRIZZO), v);
			setText(R.id.editText_cap, val.getAsString(Cantieri.CAP), v);
			setText(R.id.editText_citta, val.getAsString(Cantieri.CITTA), v);
			setText(R.id.editText_provincia, val.getAsString(Cantieri.PROVINCIA), v);
            if (val.getAsString("ragsoc_costruttore")!=null){
                setText(R.id.editText_linea, val.getAsString("ragsoc_costruttore")+" - "+val.getAsString(Linee.NOME_LINEA), v);
            }

			setText(R.id.editText_placca, val.getAsString(Placche.NOME_PLACCA), v);
			setText(R.id.editText_note, val.getAsString(Cantieri.NOTE), v);
		}
	}

}
