package pfa.app.econtab;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;

import pfa.app.econtab.adapters.EConTabExpListViewAdapter;
import pfa.app.econtab.adapters.EConTabViewHolder;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Ditte;
import pfa.app.econtab.db.table.Iva;
import pfa.app.econtab.db.table.Manodopera;
import pfa.app.econtab.db.table.UnitaMisura;
import pfa.app.econtab.server.ConfigurazioneGenActivity;
import pfa.app.econtab.SincronizzazioneActivity;
import pfa.app.econtab.utils.Sessione;
import pfa.app.econtab.utils.Utility;

public class ConfigurazioneActivity extends EConTabActivity implements OnChildClickListener {

	@Override
	protected boolean isControllaRegistrazione() {
		return false;
	}

	@Override
	protected boolean isControllaLogin() {
		return false;
	}

	ExpandableListView lista = null;
	HashMap<String, ArrayList<Object>> listChildData = null;
	ArrayList<String> sezioni = null;

	ArrayList<Boolean> selezioniDitte = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("EConTab: ConfigurazioneActivity onCreate ENTER");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_configurazione);

		// if (!Sessione.isLicenzaBusiness(this)) {
		// findViewById(R.id.buttonConfigurazioniGenerali).setVisibility(View.GONE);
		// }

		setText(R.id.textViewVersione, getString(R.string.versione) + " " + Sessione.getLicenza(this).getDescrizioneLicenza());

		selezioniDitte = new ArrayList<Boolean>();

		sezioni = new ArrayList<String>();
		sezioni.add(getString(R.string.aziende));
		sezioni.add(getString(R.string.title_activity_iva_dettaglio));
		sezioni.add(getString(R.string.unita_misura));
		sezioni.add(getString(R.string.manodopera));
		//sezioni.add(getString(R.string.avanzate));

		listChildData = new HashMap<String, ArrayList<Object>>();

		DbInterno db = new DbInterno(this);
		ArrayList<Object> aziende = db.eseguiSelect(new Ditte());
		for (int i = 0; i < aziende.size(); i++) {
			ContentValues azienda = (ContentValues) aziende.get(i);
			if (azienda.getAsInteger(Ditte.ID_DITTA) == Sessione.getDittaSelezionata()) {
				selezioniDitte.add(true);
			} else {
				selezioniDitte.add(false);
			}

		}
		ArrayList<Object> iva = db.eseguiSelect(new Iva(), null, new String[] { Iva.CODICE_IVA });

		ArrayList<Object> udm = db.eseguiSelect(new UnitaMisura(), null, new String[] { UnitaMisura.UNITA_MISURA });

		ArrayList<Object> manodopera = db.eseguiSelect(new Manodopera(), null, new String[] { Manodopera.NOME });

		db.close();

		listChildData.put(getString(R.string.aziende), aziende);
		listChildData.put(getString(R.string.title_activity_iva_dettaglio), iva);
		listChildData.put(getString(R.string.unita_misura), udm);
		listChildData.put(getString(R.string.manodopera), manodopera);

		EConTabExpListViewAdapter adapter = new EConTabExpListViewAdapter(this, sezioni, listChildData, R.layout.list_group_configurazione,
				R.layout.list_item_configurazione) {
			@Override
			protected void personalizzaGroupView(final int groupPosition, boolean isExpanded, View convertView) {
				// TODO Auto-generated method stub
				setText(R.id.nomeCategoria, sezioni.get(groupPosition), convertView);
				Button buttonNuovo = (Button) convertView.findViewById(R.id.buttonNuovo);
				// le unita' di misura arrivano dal server: nessuna creazione dall'app
				buttonNuovo.setVisibility(groupPosition == 2 ? View.GONE : View.VISIBLE);

				buttonNuovo.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if (groupPosition == 0) {
							Intent intent = new Intent(ConfigurazioneActivity.this, AziendaDettaglioActivity.class);
							apriFinestraInserimento(intent, 1, new Ditte());
						}
						if (groupPosition == 1) {
							Intent intent = new Intent(ConfigurazioneActivity.this, IvaDettaglioActivity.class);
							apriFinestraInserimento(intent, 1, new Iva());
						}
						if (groupPosition == 3) {
							Intent intent = new Intent(ConfigurazioneActivity.this, ManodoperaDettaglioActivity.class);
							apriFinestraInserimento(intent, 1, new Manodopera());
						}
					}
				});

				super.personalizzaGroupView(groupPosition, isExpanded, convertView);
			}

			@Override
			protected void personalizzaChildView(final int groupPosition, final int childPosition, boolean isLastChild, EConTabViewHolder holder)
			{
				// TODO Auto-generated method stub

				final ContentValues val = (ContentValues) listChildData.get(sezioni.get(groupPosition)).get(childPosition);
				if (groupPosition == 0) {

					((MyViewHolder) holder).nome.setText(val.getAsString(Ditte.RAGIONE_SOCIALE));
					((MyViewHolder) holder).nome2.setVisibility(View.GONE);
					((MyViewHolder) holder).checkBoxSeleziona.setVisibility(View.VISIBLE);
					if (selezioniDitte.get(childPosition) == true) {
						((MyViewHolder) holder).checkBoxSeleziona.setChecked(true);
					} else {
						((MyViewHolder) holder).checkBoxSeleziona.setChecked(false);
					}
				}
				if (groupPosition == 1) {

					((MyViewHolder) holder).checkBoxSeleziona.setVisibility(View.GONE);
					((MyViewHolder) holder).nome2.setVisibility(View.GONE);
					((MyViewHolder) holder).nome.setText(val.getAsString(Iva.CODICE_IVA) + " - " + getString(R.string.aliquota) + ": "
							+ Utility.formatNumero(val.getAsFloat(Iva.ALIQUOTA)) + " %");

				}

				if (groupPosition == 2) {

					((MyViewHolder) holder).checkBoxSeleziona.setVisibility(View.GONE);
					((MyViewHolder) holder).nome2.setVisibility(View.GONE);

					((MyViewHolder) holder).nome.setText(val.getAsString(UnitaMisura.UNITA_MISURA) + " - "
							+ val.getAsString(UnitaMisura.NOME));

				}

				if (groupPosition == 3) {

					((MyViewHolder) holder).checkBoxSeleziona.setVisibility(View.GONE);

					((MyViewHolder) holder).nome.setText(val.getAsString(Manodopera.NOME));
					((MyViewHolder) holder).nome2.setVisibility(View.VISIBLE);
					((MyViewHolder) holder).nome2.setText(getString(R.string.costo_orario) + " "
							+ Utility.formatNumero(val.getAsFloat(Manodopera.COSTO_ORARIO), 2));

				}

				int[] pos = new int[2];
				pos[0] = childPosition;
				pos[1] = groupPosition;

				((MyViewHolder) holder).nome.setTag(pos);
				((MyViewHolder) holder).checkBoxSeleziona.setTag(pos);

				((MyViewHolder) holder).checkBoxSeleziona.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						CheckBox cb = (CheckBox) v;
						// TODO Auto-generated method stub
						for (int i = 0; i < selezioniDitte.size(); i++) {
							selezioniDitte.set(i, false);

						}
						Sessione.setDittaSelezionata(0);
						Sessione.setNomeDittaSelezionata("Nessuna ditta seleizonata");
						if (cb.isChecked()) {
							selezioniDitte.set(childPosition, true);
							Sessione.setDittaSelezionata(val.getAsInteger(Ditte.ID_DITTA));
							Sessione.setNomeDittaSelezionata(val.getAsString(Ditte.RAGIONE_SOCIALE));

						}

						notifyDataSetChanged();
					}
				});

				super.personalizzaChildView(groupPosition, childPosition, isLastChild, holder);
			}

			@Override
			protected EConTabViewHolder impostaViewHolder(View convertView, final int groupPosition, final int childPosition)
			{
				// TODO Auto-generated method stub
				final MyViewHolder holder = new MyViewHolder();
				holder.nome = (TextView) convertView.findViewById(R.id.nomefiglio);
				holder.nome2 = (TextView) convertView.findViewById(R.id.nomefiglio2);
				holder.checkBoxSeleziona = (CheckBox) convertView.findViewById(R.id.checkBoxSeleziona);

				return holder;
			}
		};

		lista = (ExpandableListView) findViewById(R.id.lista);
		lista.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
		lista.setAdapter(adapter);

		registerForContextMenu(lista);
		lista.setOnChildClickListener(this);
		System.out.println("EConTab: ConfigurazioneActivity onCreate EXIT");
	}

	int countClick = 0;
	public void apriSqlManager(View view) {
		if (countClick>=5){
			Intent intentSQL = new Intent(this,SqlManagerActivity.class);
			startActivity(intentSQL);
		}
		else{
			countClick ++;
		}
	}

	private class MyViewHolder extends EConTabViewHolder {
		TextView nome = null;
		TextView nome2 = null;
		CheckBox checkBoxSeleziona = null;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		System.out.println("EConTab: ConfigurazioneActivity onCreateContextMenu ENTER");
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);

		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);

		if (type == 1) {
			int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int child = ExpandableListView.getPackedPositionChild(info.packedPosition);

			ContentValues val = (ContentValues) listChildData.get(sezioni.get(group)).get(child);
			if (group == 0) {
				menu.setHeaderTitle(val.getAsString(Ditte.RAGIONE_SOCIALE));

				menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
				menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));

			}

			if (group == 1) {
				menu.setHeaderTitle(val.getAsString(Iva.CODICE_IVA));

				menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
				menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));

			}

			if (group == 2) {
				// unita' di misura gestite dal server: solo consultazione
				menu.setHeaderTitle(val.getAsString(UnitaMisura.UNITA_MISURA) + " - " + val.getAsString(UnitaMisura.NOME));
			}

			if (group == 3) {
				menu.setHeaderTitle(val.getAsString(Manodopera.NOME));
				menu.add(Menu.NONE, 1, Menu.NONE, getString(R.string.modifica));
				menu.add(Menu.NONE, 2, Menu.NONE, getString(R.string.elimina));

			}

		}
		System.out.println("EConTab: ConfigurazioneActivity onCreateContextMenu EXIT");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		System.out.println("EConTab: ConfigurazioneActivity onContextItemSelected ENTER");
		// TODO Auto-generated method stub
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == 1) {
			int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
			ContentValues val = (ContentValues) listChildData.get(sezioni.get(group)).get(child);
			if (group == 0) {
				if (item.getItemId() == 1) {
					Intent intent = new Intent(ConfigurazioneActivity.this, AziendaDettaglioActivity.class);
					intent.putExtra("ID", val.getAsInteger(Ditte.ID_DITTA));
					apriFinestraModifica(intent, 1);
				}
				if (item.getItemId() == 2) {

					confermaCancellazione(new Ditte(), val, true);

				}
			}
			if (group == 1) {
				if (item.getItemId() == 1) {
					Intent intent = new Intent(ConfigurazioneActivity.this, IvaDettaglioActivity.class);
					intent.putExtra("ID", val.getAsString(Iva.CODICE_IVA));
					apriFinestraModifica(intent, 1);
				}
				if (item.getItemId() == 2) {
					confermaCancellazione(new Iva(), val, true);
				}

			}

			if (group == 3) {
				if (item.getItemId() == 1) {
					Intent intent = new Intent(ConfigurazioneActivity.this, ManodoperaDettaglioActivity.class);
					intent.putExtra("ID", val.getAsInteger(Manodopera.ID_MANODOPERA));
					apriFinestraModifica(intent, 1);
				}
				if (item.getItemId() == 2) {
					confermaCancellazione(new Manodopera(), val, true);
				}

			}
		}
		System.out.println("EConTab: ConfigurazioneActivity onContextItemSelected EXIT");
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onChildClick(ExpandableListView arg0, View arg1, int arg2, int arg3, long arg4) {
		System.out.println("EConTab: ConfigurazioneActivity onChildClick");
		// TODO Auto-generated method stub
		arg0.showContextMenuForChild(arg1);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("EConTab: ConfigurazioneActivity onActivityResult ENTER");
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {

			Intent intent = new Intent(this, ConfigurazioneActivity.class);
			startActivity(intent);
			finish();
		}
		System.out.println("EConTab: ConfigurazioneActivity onActivityResult EXIT");
	}

	public void gestisciDb() {

		String[] items = new String[4];
		items[0] = "Backup db";
		items[1] = getString(R.string.ripristina_db);
		items[2] = getString(R.string.reset_categorie);
		items[3] = getString(R.string.reset_tutto);

		System.out.println("EConTab: ConfigurazioneActivity gestisciDb ENTER");

		Utility.mostraSelezioneDialog(getString(R.string.gestione_db), items, this, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

				if (which == 0)
				{
					final EditText input = new EditText(ConfigurazioneActivity.this);
						input.setHint("vuoto = nome default");
					AlertDialog.Builder ab = new AlertDialog.Builder(ConfigurazioneActivity.this);
					ab.setTitle(getString(R.string.nome_backup));
					ab.setIcon(android.R.drawable.ic_dialog_info);
					ab.setView(input);
					ab.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							try {
							String nome = input.getText().toString();
							Utility.backupDatabase(ConfigurazioneActivity.this,nome.trim());
							Toast.makeText(ConfigurazioneActivity.this, getString(R.string.messaggio_backup_ok), Toast.LENGTH_SHORT).show();
							} catch (IOException e) { // TODO Auto-generated catch block
								e.printStackTrace();
								Toast.makeText(ConfigurazioneActivity.this, getString(R.string.messaggio_backup_ko), Toast.LENGTH_SHORT).show();
							}
						}
					});

					AlertDialog di = ab.create();
					di.show();
					di.setCancelable(true);
					di.setCanceledOnTouchOutside(true);
				}

				if (which == 1)
				{
					Calendar dc = Calendar.getInstance();
                    ArrayList<String> filesBackup = new ArrayList<String>();
                    final ArrayList<File> listaFile = new ArrayList<File>();
					File f = new File(Utility.getPercorsoBackup());
					File[] files = f.listFiles();

                    if (files!=null) {
                        if (files.length > 1) {
                            Arrays.sort(files, new Comparator<File>() {
                                @Override
                                public int compare(File object1, File object2) {
                                    return (int) ((object1.lastModified() > object2.lastModified()) ? -1 : 1);
                                }
                            });
                        }


                        for (int i = 0; i < files.length; i++) {
                            if (!files[i].isDirectory() && files[i].getName().endsWith(".econtab.db")) {
                                dc.setTimeInMillis(files[i].lastModified());
                                filesBackup.add(files[i].getName().replace(".econtab.db", "") + "  (" + Utility.dataOraToString(dc) + ")");
                                listaFile.add(files[i]);
                                // prendo i primi 50 file (pi� che sufficenti)
                                if (listaFile.size() >= 50) {
                                    break;
                                }
                            }
                        }

                    }
					String[] backupDisponibili = filesBackup.toArray(new String[0]);
					if (backupDisponibili.length > 0) {
						Utility.mostraSelezioneDialog(getString(R.string.ripristina_db), backupDisponibili, ConfigurazioneActivity.this,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface arg0, int index) {
										// TODO Auto-generated method stub
										File back = listaFile.get(index);
										Utility.ripristinaDb(back, ConfigurazioneActivity.this);
									}
								});
					} else {
						Toast.makeText(ConfigurazioneActivity.this, getString(R.string.nessun_backup_presente), Toast.LENGTH_SHORT).show();
					}

				}

				if (which == 2 || which == 3) {
					Intent intentSync = new Intent(ConfigurazioneActivity.this, SincronizzazioneActivity.class);
					intentSync.putExtra(SincronizzazioneActivity.EXTRA_MODE, SincronizzazioneActivity.MODE_DOWNLOAD);
					startActivity(intentSync);
				}

			}
		});
		System.out.println("EConTab: ConfigurazioneActivity gestisciDb EXIT");
	}

	public void apriConfigurazioni(View v) {
		Intent intent = new Intent(this, ConfigurazioneGenActivity.class);

		startActivity(intent);
	}

	@Override
	protected int getMenuID() {
		// TODO Auto-generated method stub
		return R.menu.econtab_menu_configurazione;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == R.id.item_backup) {
			gestisciDb();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
