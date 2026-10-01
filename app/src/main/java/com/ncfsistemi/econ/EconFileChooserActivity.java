package com.ncfsistemi.econ;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import com.ncfsistemi.econ.adapters.EconListViewAdapter;
import com.ncfsistemi.econ.adapters.EconViewHolder;
import com.ncfsistemi.econ.db.table.PreventiviDettaglio;
import com.ncfsistemi.econ.utils.AsyncTaskExecutorService;
import com.ncfsistemi.econ.utils.Utility;

public class EconFileChooserActivity extends EconActivity implements OnItemClickListener, TextWatcher {

	protected class OggettoFile implements Comparable<OggettoFile> {
		private File f = null;
		private boolean cartellaSuperiore = false;
		private boolean daRicerca = false;

		public OggettoFile(File f) {
			this.f = f;

		}

		@Override
		public int compareTo(OggettoFile another) {
			// TODO Auto-generated method stub
			if (f.lastModified() < another.f.lastModified()) {
				return 1;
			}
			return -1;
		}

		public boolean isCartellaSuperiore() {
			return cartellaSuperiore;
		}

		public void setCartellaSuperiore(boolean cartellaSuperiore) {
			this.cartellaSuperiore = cartellaSuperiore;
		}

		public boolean isDaRicerca() {
			return daRicerca;
		}

		public void setDaRicerca(boolean daRicerca) {
			this.daRicerca = daRicerca;
		}

	}

	protected class FileAdapter extends EconListViewAdapter {

		private class FileHolder extends EconViewHolder {
			ImageView icona = null;
			TextView nomeFile = null;
			TextView dataModifica = null;

		}

		public FileAdapter(Context context, ArrayList<Object> dati, int layoutid) {
			super(context, dati, layoutid);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected EconViewHolder impostaViewHolder(View convertView, int position) {
			// TODO Auto-generated method stub
			FileHolder holder = new FileHolder();
			holder.icona = (ImageView) convertView.findViewById(R.id.icona);
			holder.nomeFile = (TextView) convertView.findViewById(R.id.nome_file);
			holder.dataModifica = (TextView) convertView.findViewById(R.id.data_ultima_modifica);
			return holder;
		}

		@Override
		protected void personalizzaView(int position, EconViewHolder viewholder) {
			// TODO Auto-generated method stub
			OggettoFile file = (OggettoFile) dati.get(position);

			if (file.f.isDirectory()) {
				((FileHolder) viewholder).icona.setVisibility(View.VISIBLE);
				((FileHolder) viewholder).dataModifica.setVisibility(View.INVISIBLE);
			} else {
				((FileHolder) viewholder).icona.setVisibility(View.INVISIBLE);
				((FileHolder) viewholder).dataModifica.setVisibility(View.VISIBLE);
			}
			((FileHolder) viewholder).icona.setTag(position);

			if (file.isCartellaSuperiore()) {
				((FileHolder) viewholder).nomeFile.setText("..");
			} else {
				if (file.isDaRicerca()) {
					((FileHolder) viewholder).nomeFile.setText(file.f.getPath());
				} else {
					((FileHolder) viewholder).nomeFile.setText(file.f.getName());
				}

			}

			((FileHolder) viewholder).nomeFile.setTag(position);

			long dataUltimaModifica = file.f.lastModified();

			Calendar dc = Calendar.getInstance();
			dc.setTimeInMillis(dataUltimaModifica);

			((FileHolder) viewholder).dataModifica.setText(context.getString(R.string.ultima_modifica) + ": " + Utility.dataToString(dc));
			((FileHolder) viewholder).dataModifica.setTag(position);

			super.personalizzaView(position, viewholder);
		}
	}

	private ListView lista = null;
	private String currentDir = "";
	private FileAdapter adapter = null;
	private ArrayList<Object> dati = null;
	private String estensione = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setVisualizzazionePopup();
		setContentView(R.layout.activity_econ_file_chooser);
		// per cercare i file da importare nella memoria del dispositivo serve la lettura (fino ad Android 12)
		if (android.os.Build.VERSION.SDK_INT <= 32 && androidx.core.content.ContextCompat.checkSelfPermission(this,
				android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
		}

		lista = (ListView) findViewById(R.id.listView1);
		currentDir = com.ncfsistemi.econ.utils.Utility.cartellaApp() + File.separator
				+ PreventiviDettaglio.PATH_EXPORT_PREVENTIVI;
		lista.setOnItemClickListener(this);
		if (getIntent().getExtras().containsKey("ESTENSIONE")) {
			estensione = getIntent().getStringExtra("ESTENSIONE");
		}
		((EditText) findViewById(R.id.editText_filtra)).addTextChangedListener(this);

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		ricerca();
	}

	public void ricerca() {

		String filtro = getTesto(R.id.editText_filtra);

		lista.invalidate();
		if (dati == null) {
			dati = new ArrayList<Object>();
		}
		dati.clear();

		if (filtro.length() > 1) {
			_ricercaAsync(filtro);

		} else {
			setText(R.id.textViewCurrentDir, currentDir);
			File f = new File(currentDir);
			File[] dirs = f.listFiles();

			ArrayList<OggettoFile> dir = new ArrayList<OggettoFile>();
			ArrayList<OggettoFile> fls = new ArrayList<OggettoFile>();

			try {

				for (File ff : dirs)

				{

					if (ff.isDirectory())

						dir.add(new OggettoFile(ff));

					else

					{

						if (estensione.equals("") || ff.getName().endsWith("." + estensione)) {
							fls.add(new OggettoFile(ff));
						}

					}

				}

			} catch (Exception e)

			{

			}

			Collections.sort(dir);
			Collections.sort(fls);

			dati.addAll(dir);
			dati.addAll(fls);

			if (f.getParent() != null) {
				OggettoFile fp = new OggettoFile(new File(f.getParent()));
				fp.setCartellaSuperiore(true);
				dati.add(0, fp);
			}

			if (adapter == null) {
				adapter = new FileAdapter(this, dati, R.layout.list_item_file_chooser);
				lista.setAdapter(adapter);
			} else {
				adapter.notifyDataSetChanged();
			}
		}
	}

	private void _ricercaAsync(final String filtro) {
		// TODO Auto-generated method stub
		setText(R.id.textViewCurrentDir, "");
		// new class for asynchronous task
		AsyncTaskExecutorService<Void, Integer, String> task = new AsyncTaskExecutorService<Void, Integer, String>() {

			@Override
			protected void onPreExecute() {
				// TODO Auto-generated method stub
				super.onPreExecute();
				findViewById(R.id.progressBar1).setVisibility(View.VISIBLE);
				lista.setVisibility(View.GONE);
			}

			@Override
			protected String doInBackground(Void unused) {
				// TODO Auto-generated method stub
				ArrayList<File> fileTrovati = new ArrayList<File>();
				Utility.ricercaFile(Environment.getExternalStorageDirectory(), filtro, estensione, fileTrovati);
				ArrayList<OggettoFile> listaFileTrovati = new ArrayList<OggettoFile>();
				for (int i = 0; i < fileTrovati.size(); i++) {
					OggettoFile of = new OggettoFile(fileTrovati.get(i));
					of.setDaRicerca(true);
					listaFileTrovati.add(of);
				}
				Collections.sort(listaFileTrovati);
				dati.addAll(listaFileTrovati);
				return "";
			}

			@Override
			protected void onPostExecute(String result) {
				// TODO Auto-generated method stub
				findViewById(R.id.progressBar1).setVisibility(View.GONE);
				lista.setVisibility(View.VISIBLE);
				if (adapter == null) {
					adapter = new FileAdapter(EconFileChooserActivity.this, dati, R.layout.list_item_file_chooser);
					lista.setAdapter(adapter);
				} else {
					adapter.notifyDataSetChanged();
				}
			}
		};
		task.execute();
	}

	public void chiudi(View v) {
		finish();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		// TODO Auto-generated method stub
		final OggettoFile file = (OggettoFile) dati.get(position);
		if (file.f.isDirectory()) {
			currentDir = file.f.getPath();
			ricerca();
		} else {
			String[] items = new String[2];
			items[0] = getString(R.string.importa);
			items[1] = getString(R.string.visualizza);

			Utility.mostraSelezioneDialog("", items, this, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					if (which == 0) {
						Intent data = new Intent();
						data.putExtra("PATH", file.f.getPath());
						setResult(Activity.RESULT_OK, data);
						finish();
					}
					if (which == 1) {
						File fileExport = new File(file.f.getPath());
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(com.ncfsistemi.econ.utils.Utility.uriCondivisibile(EconFileChooserActivity.this, fileExport), "application/vnd.ms-excel"); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
					}

				}
			});
		}

	}

	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub
		if (getTesto(R.id.editText_filtra).length() <= 1) {
			ricerca();
			findViewById(R.id.imageButtonCerca).setVisibility(View.INVISIBLE);
		} else {
			findViewById(R.id.imageButtonCerca).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	public void ricercaClick(View v) {
		ricerca();
	}

}
