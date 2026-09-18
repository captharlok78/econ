package pfa.app.econtab.fragments;

import android.content.ContentValues;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Anagrafica;
import pfa.app.econtab.utils.Utility;

public class ClientiDettaglioFragment extends EConTabFragment {
	private int cliente = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_clienti_dettaglio, container, false);
		cliente = getArguments().getInt(Anagrafica.ID_ANAGRAFICA);
		DbInterno db = new DbInterno(getActivity());
		ContentValues where = new ContentValues();
		where.put(Anagrafica.ID_ANAGRAFICA, cliente);
		ContentValues val = db.getRecord(new Anagrafica(), where);

		db.close();

		v.findViewById(R.id.buttonMappa).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String indirizzo = getTesto(R.id.editText_indirizzo) + "," + getTesto(R.id.editText_citta);

				Utility.mostraMappa(getActivity(), indirizzo);
			}
		});

		if (val != null) {
			TextView headerTitolo = getActivity().findViewById(R.id.headerTitolo);
			if (headerTitolo != null) {
				headerTitolo.setText(val.getAsString(Anagrafica.RAGIONE_SOCIALE));
			}

			setText(R.id.editText_ragionesociale, val.getAsString(Anagrafica.RAGIONE_SOCIALE), v);
			setText(R.id.editText_codice, val.getAsString(Anagrafica.CODICE_ESTERNO), v);
			setText(R.id.editText_indirizzo, val.getAsString(Anagrafica.INDIRIZZO), v);
			setText(R.id.editText_cap, val.getAsString(Anagrafica.CAP), v);
			setText(R.id.editText_citta, val.getAsString(Anagrafica.CITTA), v);
			setText(R.id.editText_provincia, val.getAsString(Anagrafica.PROVINCIA), v);
			setText(R.id.editText_cf, val.getAsString(Anagrafica.CODICE_FISCALE), v);
			setText(R.id.editText_partitaiva, val.getAsString(Anagrafica.PARTITA_IVA), v);
			setText(R.id.editText_cellulare1, val.getAsString(Anagrafica.CELLULARE), v);
			setText(R.id.editText_cellulare2, val.getAsString(Anagrafica.CELLULARE1), v);
			setText(R.id.editText_telefono, val.getAsString(Anagrafica.TELEFONO), v);
			setText(R.id.spinner_iva, val.getAsString(Anagrafica.CODICE_IVA), v);
			setText(R.id.editText_mail, val.getAsString(Anagrafica.MAIL), v);
			setText(R.id.editText_note, val.getAsString(Anagrafica.NOTE), v);
		}

		return v;

	}
}
