package pfa.app.econtab;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Cantieri;

public class CantieriActivity extends EConTabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: CantieriActivity onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cantieri);
	}

	public void nuovoCantiere(View v){
		System.out.println("EConTab: CantieriActivity nuovoCantiere");
		Intent intent = new Intent(this,CantieriDettaglioModActivity.class);
		apriFinestraInserimento(intent, 2,new Cantieri());
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("EConTab: CantieriActivity onActivityResult");
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==2){
			if (resultCode==RESULT_OK){
				DbInterno db = new DbInterno(this);
				int ultimoCantiere = db.getUltimoId(new Cantieri());
				db.close();
				
				Intent intent = new Intent(this,CantiereSplitActivity.class);
				intent.putExtra(Cantieri.ID_CANTIERE, ultimoCantiere);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
			}
		}
	}

}
