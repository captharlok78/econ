package pfa.app.econtab.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import pfa.app.econtab.R;
import pfa.app.econtab.fragments.LocalePiantinaFragment;
import pfa.app.econtab.views.EConTabPiantinaGridView;
import pfa.app.econtab.views.EConTabPiantinaImageView;

public class PiantinaAdapter extends BaseAdapter {
	
	
	
	private Context context = null;
	private Boolean[] selezionati = null;
	private int numColonne = LocalePiantinaFragment.NUMERO_COLONNE;
	private int numRighe = LocalePiantinaFragment.NUMERO_RIGHE;

	public PiantinaAdapter(Context context) {
		// TODO Auto-generated constructor stub
		this.context = context;

	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return numColonne*numRighe;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		EConTabPiantinaImageView cella = null;
		if (convertView == null) {
			cella = new EConTabPiantinaImageView(context);

			// LayoutInflater infalInflater =
			// (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			// convertView = infalInflater.inflate(R.layout.grid_item_piantina,
			// null);

		} else {
			cella = (EConTabPiantinaImageView) convertView;
		}

		cella.setLayoutParams(new GridView.LayoutParams((int) parent.getWidth()/ numColonne, (int) parent.getWidth() / numColonne));
		cella.setScaleType(ImageView.ScaleType.FIT_XY);

		EConTabPiantinaGridView pgv = (EConTabPiantinaGridView) parent;
		if (!pgv.isInModifica()) {
			cella.setEnabled(false);
			if (selezionati[position] == true) {
				cella.setBackgroundColor(Color.GRAY);
				cella.setImageResource(R.drawable.piantina_si);
			} else {
				cella.setBackgroundColor(Color.TRANSPARENT);
				cella.setImageResource(R.drawable.piantina_no);
			}
			disegnaBordoSelezionati(cella, position);
		} else {

			if (selezionati[position] == true) {
				cella.setBackgroundColor(Color.parseColor("#CCCCCC"));
				cella.setImageResource(R.drawable.piantina_si);
			} else {
				cella.setBackgroundColor(Color.parseColor("#FFFFFF"));
				cella.setImageResource(R.drawable.piantina_no_edit);
			}
			disegnaBordo(cella, position);
		}

		return cella;
	}

	private void disegnaBordo(ImageView cella, int position) {
		// TODO Auto-generated method stub
		int padding = 1;
		cella.setPadding(padding, padding, padding, padding);

		
		if (isBordoSinistro(position)) {
			cella.setPadding(2*padding, cella.getPaddingTop(), cella.getPaddingRight(),
					cella.getPaddingBottom());
		}
		if (isBordoSopra(position)) {
			cella.setPadding(cella.getPaddingLeft(), 2*padding,
					cella.getPaddingRight(), cella.getPaddingBottom());
		}
		if (isBordoDestra(position)) {
			cella.setPadding(cella.getPaddingLeft(), cella.getPaddingTop(), 2*padding,
					cella.getPaddingBottom());
		}
		if (isBordoSotto(position)) {
			cella.setPadding(cella.getPaddingLeft(), cella.getPaddingTop(),
					cella.getPaddingRight(), 2*padding);
		}

	}

	private void disegnaBordoSelezionati(EConTabPiantinaImageView cella, int position) {
		// TODO Auto-generated method stub
		cella.bordoSinistra = false;
		cella.bordoSopra = false;
		cella.bordoDestra = false;
		cella.bordoSotto = false;
		cella.bordoPuntoSopraSinistra = false;
		cella.bordoPuntoSopraDestra = false;
		cella.bordoPuntoSottoSinistra = false;
		cella.bordoPuntoSottoDestra = false;
		
		int padding = 2 * context.getResources().getDimensionPixelSize(R.dimen.bordo_piantina);
		if (selezionati[position] == true) {
			if (isBordoSinistro(position)) {
				
				cella.bordoSinistra = true;
			}
			if (isBordoSopra(position)) {
				cella.bordoSopra = true;
			}
			if (isBordoDestra(position)) {
				cella.bordoDestra = true;
			}
			if (isBordoSotto(position)) {
				cella.bordoSotto = true;
			}

			// bordo sinistro
			if (position > 0) {
				if (selezionati[position - 1] == false) {
					cella.bordoSinistra = true;
				}
			}
			// bordo destro
			if (position < getCount() - 1) {
				if (selezionati[position + 1] == false) {
					cella.bordoDestra = true;
				}
			}
			// bordo sopra
			if (position >= numColonne) {
				if (selezionati[position
						- numColonne] == false) {
					cella.bordoSopra = true;
				}
					if (position>numColonne){
						if (selezionati[(position-1)  - numColonne] == false) {
										cella.bordoPuntoSopraSinistra = true;
								}
					}
					
					if (position>numColonne){
						if (selezionati[(position+1)  - numColonne] == false) {
										cella.bordoPuntoSopraDestra = true;
								}
					}
			}
			// bordo sotto
			if (position < getCount() - numColonne) {
				if (selezionati[position
						+ numColonne] == false) {
					cella.bordoSotto = true;
				}
				
				if ((position +1) < getCount() - numColonne) {
				if (selezionati[(position+1)  + numColonne] == false) {
									cella.bordoPuntoSottoDestra = true;
							}
				if (selezionati[(position-1)  + numColonne] == false) {
					cella.bordoPuntoSottoSinistra = true;
			}
				}
				
			}
		}

	}

	private boolean isBordoSinistro(int position) {
		if (position % numColonne == 0) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isBordoSopra(int position) {
		if (position < numColonne) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isBordoDestra(int position) {
		if ((position % numColonne == numColonne - 1)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isBordoSotto(int position) {
		if (position >= numColonne
				* (numRighe - 1)) {
			return true;
		} else {
			return false;
		}
	}

	public Boolean[] getSelezionati() {
		return selezionati;
	}

	public void setSelezionati(Boolean[] selezionati) {
		this.selezionati = selezionati;
		if (selezionati.length==36){
			numColonne = 6;
			numRighe = 6;
		}
		else {
			numColonne = LocalePiantinaFragment.NUMERO_COLONNE;
			numRighe = LocalePiantinaFragment.NUMERO_RIGHE;
		}
	}

}
