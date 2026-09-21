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
import pfa.app.econtab.db.table.Costruttori;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Placche;
import pfa.app.econtab.db.table.Unita;

public  class LocaleDettaglioFragment extends EConTabFragment {
	private int locale = 0;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_locale_dettaglio, container,false);
		locale = getArguments().getInt(Locali.ID_LOCALE);
		
		return v;
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		refresh();
	}
	
	public void refresh(){
DbInterno db = new DbInterno(getActivity());
		
		Locali uni = new Locali();
		
		Linee linee = new Linee();
		Placche placche = new Placche();
        Costruttori costruttori = new Costruttori();
	
		
		Join join1 = new Join(uni.getNomeTabella(), linee.getNomeTabella(),Join.LEFT_JOIN);
		join1.addCampiDiJoin(Unita.ID_LINEA, Linee.ID_LINEA);
		
		Join join2 = new Join(uni.getNomeTabella(), placche.getNomeTabella(),Join.LEFT_JOIN);
		join2.addCampiDiJoin(Unita.ID_PLACCA, Placche.ID_PLACCA);

        Join join4 = new Join(Linee.NOME_TABELLA, Costruttori.NOME_TABELLA, Join.LEFT_JOIN);
        join4.addCampiDiJoin(Linee.ID_COSTRUTTORE, Costruttori.ID_COSTRUTTORE);
		
		String SQL = "Select " + uni.getNomeCampoTabella("*") +","+linee.getNomeCampoTabella(Linee.NOME_LINEA)+","+placche.getNomeCampoTabella(Placche.NOME_PLACCA)+","+costruttori.getNomeCampoTabella(Costruttori.RAGIONE_SOCIALE)+" as ragsoc_costruttore"+
				" from " + uni.getNomeTabella()+ join1.getSQLJoin()+join2.getSQLJoin()+ join4.getSQLJoin()+
        " where " + uni.getNomeCampoTabella(Locali.ID_LOCALE)+" = " + locale;
		
		ArrayList<Object> records = db.eseguiSelect(SQL, null);
		
		ContentValues val = null;
		
		db.close();
		
		if (records.size()>0){
			val = (ContentValues)records.get(0);
		}
		View v = getView();
		if (val!=null){
			setText(R.id.editText_locale, val.getAsString(Locali.NOME), v);
			String nomeLocale = val.getAsString(Locali.NOME);
			android.widget.TextView tvTitolo = (android.widget.TextView) getActivity().findViewById(R.id.headerTitolo);
			if (tvTitolo != null) tvTitolo.setText(nomeLocale);

            if (val.getAsString("ragsoc_costruttore")!=null){
                setText(R.id.editText_linea,  val.getAsString("ragsoc_costruttore")+" - "+val.getAsString(Linee.NOME_LINEA), v);
            }

			setText(R.id.editText_placca, val.getAsString(Placche.NOME_PLACCA), v);
			setText(R.id.editText_note, val.getAsString(Locali.NOTE),v);
		}
	}
	
	
	
}
