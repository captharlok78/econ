package pfa.app.econtab;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import pfa.app.econtab.adapters.SQLAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.utils.Utility;


/**
 * Created by matteo on 09/10/2017.
 */

public class SqlManagerActivity extends EConTabActivity {
    TextView selTabella = null;
    TextView selCampi = null;
    EditText editQuery = null;

    String campi = "";

    private ArrayList<Object> arrayListTabelle = new ArrayList<Object>();
    private ArrayList<Object> arrayListCampi = new ArrayList<Object>();

    ArrayList<Object> data = null;
    private SQLAdapter adapter;
    ListView listaSQL = null;

    private String selectedPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: SqlManagerActivity onCreate ENTER");
        super.onCreate(savedInstanceState);
        setVisualizzazionePopup(50, 20);
        setContentView(R.layout.activity_sql_manager);

        selTabella = (TextView) findViewById(R.id.selTabella);
        selCampi = (TextView) findViewById(R.id.selCampi);
        editQuery = (EditText) findViewById(R.id.editQuery);
        listaSQL = (ListView) findViewById(R.id.listaSQL);

        DbInterno db = new DbInterno(this);

        editQuery.setText("");

        arrayListTabelle.addAll(db.eseguiSelect("SELECT name FROM sqlite_master WHERE type='table' order by name asc",null));
        db.close();
        System.out.println("EConTab: SqlManagerActivity onCreate EXIT");
    }

    public void chiudi(View view) {
        finish();
    }

    public void selezionaTabella(View view) {
        System.out.println("EConTab: SqlManagerActivity selezionaTabella ENTER");
        final String[] tabelle = new String[arrayListTabelle.size()];
        for (int i = 0; i < arrayListTabelle.size(); i++) {
            ContentValues tabella = (ContentValues) arrayListTabelle.get(i);
            tabelle[i] = tabella.getAsString("name");
        }
        Utility.mostraSelezioneDialog("tabelle", tabelle, this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                selTabella.setText(tabelle[i]);
                editQuery.setText("select * from " + tabelle[i]);
                DbInterno db = new DbInterno(SqlManagerActivity.this);
                selCampi.setText("*");
                arrayListCampi.clear();
                campi = "";
                arrayListCampi.addAll(db.eseguiSelect("PRAGMA table_info('" + tabelle[i] + "')",null));

                db.close();

            }
        });
        System.out.println("EConTab: SqlManagerActivity selezionaTabella EXIT");
    }

    public void selezionaCampi(View view) {
        System.out.println("EConTab: SqlManagerActivity selezionaCampi ENTER");
        final String[] campi = new String[arrayListCampi.size()+1];
        campi[0] = "*";

        for (int i = 0; i < arrayListCampi.size(); i++) {
            ContentValues operatore = (ContentValues) arrayListCampi.get(i);
            campi[i + 1] = operatore.getAsString("name");
        }

        Arrays.sort(campi,String.CASE_INSENSITIVE_ORDER);

        Utility.mostraSelezioneDialog("campi", campi, this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i==0){
                    selCampi.setText("*");
                }
                else {
                    selCampi.setText(campi[i]);
                }
            }
        });
        System.out.println("EConTab: SqlManagerActivity selezionaCampi EXIT");
    }

    public void aggiungiCampo(View view) {
        System.out.println("EConTab: SqlManagerActivity aggiungiCampo ENTER");
        if(campi.equals(""))
        {
            campi = campi + selCampi.getText().toString();
        }
        else {
            campi = campi + "," + selCampi.getText().toString();
        }
        aggiornaEditQuery();
        System.out.println("EConTab: SqlManagerActivity aggiungiCampo EXIT");
    }

    private void aggiornaEditQuery() {
        editQuery.setText("select " + campi + " from " + selTabella.getText().toString());
    }

    public void reset(View view) {
        System.out.println("EConTab: SqlManagerActivity reset ENTER");
        Intent intent = getIntent();
        finish();
        startActivity(intent);
        System.out.println("EConTab: SqlManagerActivity reset EXIT");
    }

    public void esegui(View view) {
        System.out.println("EConTab: SqlManagerActivity esegui ENTER");
        String SQL = "";
        SQL = editQuery.getText().toString();

        if(!SQL.equals(""))
        {
            if (data!=null){
                data.clear();
            }else{
                data = new ArrayList<Object>();
            }

            DbInterno db = new DbInterno(this);
            try {
                data.addAll(db.eseguiSelect(SQL, null));
                Toast.makeText(this,"query eseguita",Toast.LENGTH_LONG).show();
            }
            catch (Exception e)
            {
                Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
            }
            db.close();

            if (adapter == null) {
                adapter = new SQLAdapter(this, data);
                listaSQL.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        }
        System.out.println("EConTab: SqlManagerActivity esegui EXIT");
    }


}
