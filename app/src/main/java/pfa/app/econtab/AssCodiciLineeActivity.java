package pfa.app.econtab;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import pfa.app.econtab.adapters.AssCodiciLineeAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AssCodiciLinee;
import pfa.app.econtab.db.table.Linee;
import pfa.app.econtab.db.table.Placche;


/**
 * Created by daniele on 15/01/2018.
 */

public class AssCodiciLineeActivity extends EConTabActivity implements AdapterView.OnItemClickListener {
    private ListView lista = null;


    private ArrayList<Object> dati = null;
    private AssCodiciLineeAdapter adapter = null;
    private String codicearticolo = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: AssCodiciLineeActivity onCreate ENTER");
        super.onCreate(savedInstanceState);
        setVisualizzazionePopup(7, 3);
        setContentView(R.layout.activity_ass_codici_linee);
        lista = (ListView) findViewById(R.id.lista);
        lista.setOnItemClickListener(this);

        codicearticolo = getIntent().getStringExtra(AssCodiciLinee.CODICE_ARTICOLO);
        System.out.println("EConTab: AssCodiciLineeActivity onCreate EXIT");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ricerca();
    }


    private void ricerca() {
        // TODO Auto-generated method stub
        lista.invalidate();
        if (dati == null) {
            dati = new ArrayList<Object>();
        }
        dati.clear();

        DbInterno db = new DbInterno(this);

        dati.addAll(db.eseguiSelect("Select ass_codici_linee.*,linee.nome_linea from ass_codici_linee inner join linee on ass_codici_linee.id_linea=linee.id_linea where codice_articolo=?", new String[]{codicearticolo}));

        db.close();

        if (adapter == null) {
            adapter = new AssCodiciLineeAdapter(this, dati, R.layout.list_item_ass_codici_linee);
            lista.setAdapter(adapter);
            lista.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            registerForContextMenu(lista);
        } else {
            adapter.notifyDataSetChanged();

        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        // TODO Auto-generated method stub
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ContentValues item = (ContentValues) lista.getItemAtPosition(info.position);
        menu.setHeaderTitle(item.getAsString(Linee.NOME_LINEA));
        menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.elimina));

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ContentValues linea = (ContentValues) lista.getItemAtPosition(info.position);

        if (item.getItemId() == 1) {
            confermaCancellazione(new AssCodiciLinee(), linea, true);
        }
        return super.onContextItemSelected(item);
    }


    public void chiudi(View v) {
        finish();
    }
}
