package pfa.app.econtab.fragments;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import pfa.app.econtab.CantiereSplitActivity;
import pfa.app.econtab.FinestraLocaliPreferitiActivity;
import pfa.app.econtab.PiantinaLocaleModActivity;
import pfa.app.econtab.R;
import pfa.app.econtab.adapters.EConTabListViewAdapter;
import pfa.app.econtab.adapters.EConTabViewHolder;
import pfa.app.econtab.adapters.PiantinaAdapter;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.ElementiCantiere;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabElementoElettrico;
import pfa.app.econtab.views.EConTabElementoPorta;
import pfa.app.econtab.views.EConTabLocaleViewPager;
import pfa.app.econtab.views.EConTabPiantinaGridView;

public class LocalePiantinaFragment extends EConTabFragment implements OnClickListener, OnTouchListener {

    private class ListaElementiViewHolder extends EConTabViewHolder {
        TextView nomeElemento = null;
        TextView numero = null;
        FrameLayout colore = null;

    }

    private class ListaElementiAdapter extends EConTabListViewAdapter{

        public ListaElementiAdapter(Context context, ArrayList<Object> dati, int layoutid){
            super(context,dati,layoutid);
        }


        @Override
        protected EConTabViewHolder impostaViewHolder(View convertView, int position) {
            ListaElementiViewHolder holder = new ListaElementiViewHolder();
            holder.colore = (FrameLayout)convertView.findViewById(R.id.colore_elemento);
            holder.nomeElemento = (TextView)convertView.findViewById(R.id.textViewElemento);
            holder.numero = (TextView)convertView.findViewById(R.id.textViewNumElemento);
            return holder;
        }


        @Override
        protected void personalizzaView(int position, EConTabViewHolder viewholder) {
            ListaElementiViewHolder holder = (ListaElementiViewHolder)viewholder;
            ContentValues val = (ContentValues)dati.get(position);
            holder.colore.setBackgroundColor(Color.parseColor(Utility.getColoreCategoria(val.getAsInteger(Elementi.ID_CATEGORIA_GENERALE))));
            holder.colore.setTag(position);

            holder.numero.setText(val.getAsString(ElementiCantiere.NUMERO_IDENTIFICATIVO));
            holder.numero.setTag(position);

            holder.nomeElemento.setText(val.getAsString(ElementiCantiere.NOME_ELEMENTO_CANT));
            holder.nomeElemento.setTag(position);

            super.personalizzaView(position,viewholder);
        }
    }


    private int locale = 0;
	private String nomeLocale = "";
	public static final int NUMERO_COLONNE = 8;
	public static final int NUMERO_RIGHE = 8;

	private EConTabPiantinaGridView pianta = null;
	private PiantinaAdapter adapter = null;
	private Boolean[] selezionati = null;
	private EConTabLocaleViewPager pager = null;

	private ImageButton drag_si_no = null;
	private ImageButton preferiti = null;

	private boolean tastiVisibili = false;

	private boolean animazioneEseguita = false;
	float downY = 0f;
	float upY = 0f;


    float yStartDragPopup = 0f;
    float xStartDragPopup = 0f;

    float xOriginalPopup = 0f;
    float yOriginalPopup = 0f;

    private RelativeLayout popupListaElementi = null;
    private ListView listaElementi = null;
    private ListaElementiAdapter adapterLista = null;
    private ArrayList<Object> dati = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View v = super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.fragment_locale_piantina, container, false);
		locale = getArguments().getInt(Locali.ID_LOCALE);

		pianta = (EConTabPiantinaGridView) v.findViewById(R.id.gridView_piantina);
		pianta.setNumColumns(NUMERO_COLONNE);
		pianta.setInModifica(false);
		pianta.setRoot((FrameLayout) v.findViewById(R.id.root));
		pianta.setViewPager(pager);
		pianta.setCestino((ImageView) v.findViewById(R.id.button_cestino));
		pianta.setIdLocale(locale);
		pianta.setDragPorteFinestre(false);

		preferiti = (ImageButton) v.findViewById(R.id.button_preferiti);
		preferiti.setOnClickListener(this);

		View edit = v.findViewById(R.id.button_edit);
		edit.setOnClickListener(this);

		View porta = v.findViewById(R.id.button_porta);
		porta.setOnClickListener(this);

		View finestra = v.findViewById(R.id.button_finestra);
		finestra.setOnClickListener(this);

		drag_si_no = (ImageButton) v.findViewById(R.id.button_drag_si_no);
		drag_si_no.setOnClickListener(this);

		v.findViewById(R.id.imageButton_mostra).setOnClickListener(this);
		v.findViewById(R.id.imageButton_mostra).setOnTouchListener(this);
		v.findViewById(R.id.tasti).setClickable(true);
		v.findViewById(R.id.tasti).setOnTouchListener(this);
		v.findViewById(R.id.imageButton_mostra).setVisibility(View.INVISIBLE);


        popupListaElementi = (RelativeLayout)v.findViewById(R.id.popup_lista_elementi);
        popupListaElementi.setVisibility(View.INVISIBLE);
        ((ImageButton)v.findViewById(R.id.imageButton_chiudi_lista)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                popupListaElementi.setVisibility(View.INVISIBLE);
            }
        });
        ((ImageButton)v.findViewById(R.id.button_lista_elementi)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (popupListaElementi.getVisibility()==View.VISIBLE){
                    popupListaElementi.setVisibility(View.INVISIBLE);
                }
                else{
                    popupListaElementi.setVisibility(View.VISIBLE);
                    caricaListaElementi();
                    if (xOriginalPopup!=0) {
                        popupListaElementi.setY(yOriginalPopup);
                        popupListaElementi.setX(xOriginalPopup);
                    }
                }

            }
        });

        popupListaElementi.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // Get finger position on screen
                final float Y =  event.getY();
                final float X =  event.getX();

                if (xOriginalPopup==0){
                    xOriginalPopup = popupListaElementi.getX();
                    yOriginalPopup = popupListaElementi.getY();
                }
                // Switch on motion event type
                switch (event.getAction() & MotionEvent.ACTION_MASK) {

                    case MotionEvent.ACTION_DOWN:
                        // save default base layout height
                        yStartDragPopup = event.getY();
                        xStartDragPopup = event.getX();
                        break;

                    case MotionEvent.ACTION_UP:
                        // If user was doing a scroll up
                        yStartDragPopup = 0;
                        xStartDragPopup = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:

                        float currentYPosition =  popupListaElementi.getY();
                        float currentXPosition =  popupListaElementi.getX();

                        float ydiff = Y-yStartDragPopup;
                        float xdiff = X-xStartDragPopup;
                        popupListaElementi.setX(currentXPosition+xdiff);
                        popupListaElementi.setY(currentYPosition+ydiff);

                        break;
                }
                return true;
            }
        });


		// refresh();
		// aggiornaElementiPiantina(false);


        listaElementi = (ListView)v.findViewById(R.id.lista_elementi);
        listaElementi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ContentValues valSel = (ContentValues)dati.get(i);
                ArrayList<EConTabElementoElettrico> elems = pianta.getListaElementi();
                for (int e=0;e<elems.size();e++){
                    if (elems.get(e).getTag()==valSel){
                        elems.get(e).mostraPopup();
                        popupListaElementi.setVisibility(View.INVISIBLE);
                        break;
                    }
                }
            }
        });

		return v;

	}

    private void caricaListaElementi(){

		ArrayList<EConTabElementoElettrico> elems = pianta.getListaElementi();
        if (dati==null){
            dati = new ArrayList<Object>();
        }
        else{
            dati.clear();
        }


        for (int i=0;i<elems.size();i++){
            ContentValues val = new ContentValues();
            val.put(Elementi.ID_CATEGORIA_GENERALE,elems.get(i).getIdCategoria());
            val.put(ElementiCantiere.NUMERO_IDENTIFICATIVO,elems.get(i).getNumero());
            val.put(ElementiCantiere.NOME_ELEMENTO_CANT,elems.get(i).getDescrizione());
            elems.get(i).setTag(val);
            dati.add(val);
        }
        Collections.sort(dati,new Comparator<Object>() {
            @Override
            public int compare(Object o, Object o2) {
                try {
                    int num1 = ((ContentValues)o).getAsInteger(ElementiCantiere.NUMERO_IDENTIFICATIVO);
                    int num2 = ((ContentValues)o2).getAsInteger(ElementiCantiere.NUMERO_IDENTIFICATIVO);
                    if (num1>num2){
                        return 1;
                    }
                    if (num1<num2){
                        return -1;
                    }
                }
                catch (Exception e){

                }

                return 0;
            }
        });
        if (adapterLista==null){
            adapterLista = new ListaElementiAdapter(getActivity(),dati,R.layout.list_item_elemento_locale);
            listaElementi.setAdapter(adapterLista);
        }
        else{
            adapterLista.notifyDataSetChanged();
        }
    }


	public void aggiornaElementiPiantina(boolean attivaDrag) {
		pianta.aggiornaElementi(((CantiereSplitActivity) getEConTabActivity()).getIdPreventivoSelezionato());




		if (attivaDrag) {
			drag_si_no.setImageResource(R.drawable.drag_si);
			pianta.setDragElementi(true);
			pianta.setDragPorteFinestre(true);
		}

	}

	public void aggiornaElementiPortePiantina() {
		pianta.aggiornaElementiPorte();

	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub

		super.onResume();
		refresh();
		if (isRefreshElementi()) {
			setRefreshElementi(false);
			aggiornaElementiPiantina(false);
		}

		// pianta.mostraPopupElementoSelezionato();

		// PROSSIMO_ID c'� quando sto facendo gli screenshot delle piantine dei vari locali e serve per caricare il
		// prossimo locale
		if (!animazioneEseguita && !getArguments().containsKey("PROSSIMO_ID")) {
			animazioneEseguita = true;

			TranslateAnimation slideDown = new TranslateAnimation(0, 0, getResources().getDimensionPixelSize(R.dimen.altezza_campo_doppia)
					* -1, 0);
			slideDown.setDuration(600);

			final TranslateAnimation slideUp = new TranslateAnimation(0, 0, 0, getResources().getDimensionPixelSize(
					R.dimen.altezza_campo_doppia)
					* -1);
			slideUp.setDuration(500);

			// getView().findViewById(R.id.imageButton_mostra).startAnimation(slideUp);
			getView().findViewById(R.id.tasti).startAnimation(slideUp);

			/*
			 * slideDown.setAnimationListener(new Animation.AnimationListener() {
			 * 
			 * @Override public void onAnimationStart(Animation animation) { // TODO Auto-generated method stub
			 * 
			 * }
			 * 
			 * @Override public void onAnimationRepeat(Animation animation) { // TODO Auto-generated method stub
			 * 
			 * }
			 * 
			 * @Override public void onAnimationEnd(Animation animation) {
			 * getView().findViewById(R.id.imageButton_mostra).startAnimation(slideUp);
			 * getView().findViewById(R.id.tasti).startAnimation(slideUp); // TODO Auto-generated method stub //
			 * tastiVisibili = false; // getView().findViewById(R.id.tasti).setVisibility(View.GONE); //
			 * getView().findViewById(R.id.tasti).setVisibility(View.GONE); //
			 * ((ImageButton)getView().findViewById(R.id.
			 * imageButton_mostra)).setImageDrawable(getResources().getDrawable(R.drawable.slide_down)); } });
			 */

			slideUp.setAnimationListener(new Animation.AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationEnd(Animation animation) {

					// TODO Auto-generated method stub
					tastiVisibili = false;
					getView().findViewById(R.id.imageButton_mostra).setVisibility(View.VISIBLE);
					getView().findViewById(R.id.tasti).setVisibility(View.GONE);
					((ImageButton) getView().findViewById(R.id.imageButton_mostra)).setImageDrawable(getResources().getDrawable(
							R.drawable.slide_down));
				}
			});
		}

		if (getArguments().containsKey("PROSSIMO_ID")) {
			final int prossimoId = getArguments().getInt("PROSSIMO_ID");

			pianta.postDelayed(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					pianta.creaImmagine();
					if (getActivity() != null) {
						((CantiereSplitActivity) getActivity()).creaImmaginiPiantine(prossimoId, 0);
					}

				}
			}, 400);

		}
		if (getArguments().containsKey("MODIFICHE_BLOCCATE")) {
			boolean modificheBloccate = getArguments().getBoolean("MODIFICHE_BLOCCATE");
			setModificheBloccate(modificheBloccate);
		}
	}

	public void refresh() {

		pianta.invalidate();
		if (adapter == null) {
			adapter = new PiantinaAdapter(getActivity());
			pianta.setAdapter(adapter);

		}
		selezionati = new Boolean[NUMERO_COLONNE * NUMERO_RIGHE];
		DbInterno db = new DbInterno(getActivity());
		ContentValues where = new ContentValues();
		where.put(Locali.ID_LOCALE, locale);
		ContentValues val = db.getRecord(new Locali(), where);
		db.close();

		String piantina = "";
		if (val != null) {
			piantina = val.getAsString(Locali.PIANTA_LOGICA);
			nomeLocale = val.getAsString(Locali.NOME);
			android.widget.TextView tvTitolo = (android.widget.TextView) getActivity().findViewById(R.id.textView_titolo_cantiere);
			if (tvTitolo != null) tvTitolo.setText(nomeLocale);

		}

		pianta.setNumColumns(NUMERO_COLONNE);
		if (piantina.length()<NUMERO_COLONNE*NUMERO_RIGHE){
			if (piantina.length()==36){
				selezionati = new Boolean[6*6];
				pianta.setNumColumns(6);
			}
			else {
				while (piantina.length() < NUMERO_COLONNE * NUMERO_RIGHE) {
					piantina = piantina + "1";
				}
			}
		}



		for (int i = 0; i < selezionati.length; i++) {
			String sel = String.valueOf(piantina.charAt(i));
			if (sel.equals("1")) {
				selezionati[i] = true;
			} else {
				selezionati[i] = false;
			}

		}
		adapter.setSelezionati(selezionati);
		adapter.notifyDataSetChanged();

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		if (v.getId() == R.id.imageButton_mostra) {
			if (tastiVisibili) {
				mostraTasti(false);
			} else {
				mostraTasti(true);
			}

		}

		if (v.getId() == R.id.button_drag_si_no) {
			if (pianta.isDragPorteFinestre()) {
				pianta.setDragPorteFinestre(false);
				pianta.setDragElementi(false);
				drag_si_no.setImageResource(R.drawable.drag_no);
			} else {
				pianta.setDragPorteFinestre(true);
				pianta.setDragElementi(true);
				drag_si_no.setImageResource(R.drawable.drag_si);
			}
		}

		if (v.getId() == R.id.button_preferiti) {
			_mostraOpzioniPreferiti();
		}

		if (v.getId() == R.id.button_edit) {
			if (selezionati.length<NUMERO_RIGHE*NUMERO_COLONNE){
				String[] opzioni = new String[2];
				opzioni[0] = "Mantieni risoluzione originale";
				opzioni[1] = "Cambia risoluzione (Sarà necessario riposizionare gli elementi dopo il salvataggio)";
				Utility.mostraSelezioneDialog("Attenzione, la piantina è stata creata con una risoluzione diversa da quella attuale", opzioni, getActivity(), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						if (i==0){
							Intent intent = new Intent(getActivity(), PiantinaLocaleModActivity.class);
							intent.putExtra("ID", locale);
							intent.putExtra("NUMRIGHE", 6);
							intent.putExtra("NUMCOLONNE", 6);
							startActivityForResult(intent, 1);
						}
						if (i==1){
							Intent intent = new Intent(getActivity(), PiantinaLocaleModActivity.class);
							intent.putExtra("ID", locale);
							startActivityForResult(intent, 1);
						}
					}
				});
			}
			else {
				Intent intent = new Intent(getActivity(), PiantinaLocaleModActivity.class);
				intent.putExtra("ID", locale);
				startActivityForResult(intent, 1);
			}

		}

		if (v.getId() == R.id.button_finestra) {
			EConTabElementoPorta finestra = new EConTabElementoPorta(getActivity(), EConTabElementoPorta.FINESTRA);
			pianta.aggiungiElementoPorta(finestra);
			pianta.setDragPorteFinestre(true);
			drag_si_no.setImageResource(R.drawable.drag_si);
		}

		if (v.getId() == R.id.button_porta) {
			EConTabElementoPorta finestra = new EConTabElementoPorta(getActivity(), EConTabElementoPorta.PORTA);
			pianta.aggiungiElementoPorta(finestra);
			pianta.setDragPorteFinestre(true);
			drag_si_no.setImageResource(R.drawable.drag_si);
		}

	}

	public ViewPager getViewPager() {
		return pager;
	}

	public void setViewPager(ViewPager viewPager) {
		if (viewPager instanceof EConTabLocaleViewPager) {
			this.pager = (EConTabLocaleViewPager) viewPager;
		} else {
			this.pager = null;
		}

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {

			downY = event.getY();
			// return false;
		}
		case MotionEvent.ACTION_UP: {
			upY = event.getY();
			float deltaY = downY - upY;
			if (deltaY < 0) {// swipe verso il basso
				mostraTasti(true);
			}
			if (deltaY > 0) {
				mostraTasti(false);
			}
		}

		}
		return false;
	}

	private void mostraTasti(boolean mostra) {
		if (mostra == true) {
			getView().findViewById(R.id.tasti).setVisibility(View.VISIBLE);
			((ImageButton) getView().findViewById(R.id.imageButton_mostra)).setImageDrawable(getResources()
					.getDrawable(R.drawable.slide_up));

		} else {
			getView().findViewById(R.id.tasti).setVisibility(View.GONE);
			((ImageButton) getView().findViewById(R.id.imageButton_mostra)).setImageDrawable(getResources().getDrawable(
					R.drawable.slide_down));

		}
		tastiVisibili = mostra;
	}

	private void _mostraOpzioniPreferiti() {
		// TODO Auto-generated method stub
		String[] items = new String[2];
		items[0] = getString(R.string.salva_locale_preferito);
		items[1] = getString(R.string.importa_locale_preferito);
		Utility.mostraSelezioneDialog("", items, getActivity(), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if (which == 0) {
					_salvaPreferito();
				}
				if (which == 1) {
					Intent intent = new Intent(getActivity(), FinestraLocaliPreferitiActivity.class);
					intent.putExtra(Locali.ID_LOCALE, locale);
					intent.putExtra(Preventivi.ID_PREVENTIVO, ((CantiereSplitActivity) getEConTabActivity()).getIdPreventivoSelezionato());
					ArrayList<Integer> listaElementi = pianta.getListaIdElementi();

					int[] elementi = new int[listaElementi.size()];
					for (int i = 0; i < listaElementi.size(); i++) {
						elementi[i] = listaElementi.get(i);
					}
					intent.putExtra("ELEMENTI", elementi);

					getActivity().startActivityForResult(intent, 3);// 3 � il codice associato alla selezione del locale
																	// preferito (vedi onactivityresult di
																	// CantiereSplitActivity)
				}
			}
		});
	}

	protected void _salvaPreferito() {
		// TODO Auto-generated method stub
		((CantiereSplitActivity) getActivity()).setSplitterPositionPercent(0);
		final EditText nome = new EditText(getActivity());
		nome.setText(nomeLocale);
		Utility.mostraDialogPersonalizzato(getString(R.string.nome_locale_preferito), getActivity(), nome, getString(R.string.salva),
				getString(R.string.annulla), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if (which == DialogInterface.BUTTON_POSITIVE) {
							AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
							ab.setMessage(getString(R.string.messaggio_salvataggio_preferito_in_corso));
							final AlertDialog di = ab.create();
							di.show();

							pianta.postDelayed(new Runnable() {

								@Override
								public void run() {
									// TODO Auto-generated method stub
									pianta.creaImmagine();
									DbInterno db = new DbInterno(getActivity());
									db.getReadableDatabase().beginTransaction();
									Locali tabLocale = new Locali();
									try {
										tabLocale.salvaLocalePreferito(db, locale, nome.getText().toString(), pianta.getListaIdElementi());
										db.getReadableDatabase().setTransactionSuccessful();
									} catch (Exception e) {
										Toast.makeText(getActivity(), Log.getStackTraceString(e), Toast.LENGTH_SHORT).show();

									}
									db.getReadableDatabase().endTransaction();
									db.close();
									di.cancel();
									Toast.makeText(getActivity(), getString(R.string.messaggio_salvataggio_preferito), Toast.LENGTH_LONG)
											.show();

								}
							}, 200);
						}
					}
				});

	}

	public void setModificheBloccate(boolean modificheBloccate) {
		// TODO Auto-generated method stub

		if (modificheBloccate) {
			drag_si_no.setVisibility(View.INVISIBLE);
			preferiti.setVisibility(View.INVISIBLE);
			drag_si_no.setImageResource(R.drawable.drag_no);
			pianta.setDragElementi(false);

		} else {
			drag_si_no.setVisibility(View.VISIBLE);
			preferiti.setVisibility(View.VISIBLE);
		}
	}

	public boolean isRefreshElementi() {
		return pianta.isRefreshElementi();
	}

	public void setRefreshElementi(boolean refreshElementi) {
		pianta.setRefreshElementi(refreshElementi);
	}
}
