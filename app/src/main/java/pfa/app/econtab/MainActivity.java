package pfa.app.econtab;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: MainActivity onCreate ENTER");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		System.out.println("EConTab: MainActivity onCreate EXIT");
	}

}
