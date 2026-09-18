package pfa.app.econtab;

import android.os.Bundle;

public class OrdiniActivity extends EConTabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: OrdiniActivity onCreate ENTER");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ordini);
		System.out.println("EConTab: OrdiniActivity onCreate EXIT");
	}

}
