package pfa.app.econtab;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.Cantieri;
import pfa.app.econtab.db.table.CategorieComponenti;
import pfa.app.econtab.db.table.CategorieGenerali;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.Composizioni;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.Locali;
import pfa.app.econtab.db.table.Preventivi;
import pfa.app.econtab.fragments.ComponentiCategoriaFragment;
import pfa.app.econtab.fragments.ElementiCategoriaFragment;
import pfa.app.econtab.utils.Utility;
import pfa.app.econtab.views.EConTabElementoElettrico;
import pfa.app.econtab.views.EConTabItemMenu;

public class FinestraElementiActivity extends EConTabActivity implements OnClickListener {

    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    protected int numeroCategorie = 0;
    TabHost tabHost = null;
    HorizontalScrollView scroll_tab = null;
    private LinearLayout categorie = null;
    int selezionato = 0;

    int categoriaSelezionata = 0;
    private int idPreventivoSelezionato = 0;
    private int idCantiere = 0;
    private int idLocale = 0;

    private String descrizioneCantiere = "";
    private String descrizionePreventivo = "";

    ImageButton buttonChiudi = null;

    ArrayList<EConTabItemMenu> listaCategorie = null;

    private boolean mostraAvviso = true;

    protected TextView titolo = null;

    private boolean scrollAutomatico = true;
    private int precSelPerScroll = 0;
    private int precScrollX = 0;

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            EConTabItemMenu itemSel = listaCategorie.get(position);

            if (itemSel.getTipo() == EConTabItemMenu.CATEGORIA_ELEMENTI) {

                ElementiCategoriaFragment fragment = new ElementiCategoriaFragment();
                Bundle args = new Bundle();
                args.putInt(CategorieGenerali.ID_CATEGORIA_GENERALE, listaCategorie.get(position).getIdCategoria());
                fragment.setArguments(args);

                return fragment;
            }

            if (itemSel.getTipo() == EConTabItemMenu.CATEGORIA_COMPONENTI) {
                ComponentiCategoriaFragment fragment = new ComponentiCategoriaFragment();
                Bundle args = new Bundle();
                int idCategoria = listaCategorie.get(position).getIdCategoria();

                args.putInt(CategorieComponenti.ID_CATEGORIA_COMPONENTE, idCategoria);

                if (idCategoria == Componenti.COPRISCATOLA || idCategoria == Componenti.PORTAFRUTTI) {
                    args.putInt(Componenti.SPAZI_OSPITATI, getIntent().getIntExtra(Componenti.SPAZI_OSPITATI, 0));
                }

                fragment.setArguments(args);

                return fragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return numeroCategorie;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("EConTab: FinestraElementiActivity onCreate ENTER");
        super.onCreate(savedInstanceState);
        setVisualizzazionePopup();
        setContentView(R.layout.activity_finestra_elementi);


        titolo = (TextView) findViewById(R.id.textViewTitolo);
        scroll_tab = (HorizontalScrollView) findViewById(R.id.scroll_categorie);
        categorie = (LinearLayout) findViewById(R.id.categorie);

        categoriaSelezionata = getIntent().getIntExtra(CategorieGenerali.ID_CATEGORIA_GENERALE, 0);
        setIdCantiere(getIntent().getIntExtra(Cantieri.ID_CANTIERE, 0));
        setIdPreventivoSelezionato(getIntent().getIntExtra(Preventivi.ID_PREVENTIVO, 0));
        setIdLocale(getIntent().getIntExtra(Locali.ID_LOCALE, 0));
        setDescrizioneCantiere(getIntent().getStringExtra(Cantieri.NOME));
        setDescrizionePreventivo(getIntent().getStringExtra("DESC_PREVENTIVO"));

        creaMenuCategorie();

        ricaricaPageAdapter();


        buttonChiudi = (ImageButton) findViewById(R.id.imageButton_chiudi);
        buttonChiudi.setOnClickListener(this);

        setVisualizzazioneCategorie();
        System.out.println("EConTab: FinestraElementiActivity onCreate EXIT");
    }

    public void ricaricaPageAdapter() {
        System.out.println("EConTab: FinestraElementiActivity ricaricaPageAdapter ENTER");

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {

                categorie.getChildAt(selezionato).setSelected(false);
                categorie.getChildAt(position).setSelected(true);

                selezionato = position;

                EConTabItemMenu viewSel = (EConTabItemMenu) categorie.getChildAt(position);
                titolo.setText(viewSel.getTesto());

                if (scrollAutomatico) {
                    scroll_tab.smoothScrollTo(precScrollX, 0);
                    // scroll_tab.smoothScrollTo(viewSel.getLeft() - viewSel.getWidth(), 0);
                    Rect scrollBounds = new Rect();
                    scroll_tab.getHitRect(scrollBounds);

                    Rect viewBounds = new Rect();
                    if (!viewSel.getGlobalVisibleRect(viewBounds, new Point(0, 0))) {
                        if (precSelPerScroll < position) {
                            scroll_tab.smoothScrollTo(precScrollX + viewSel.getWidth() * 2, 0);
                        } else {
                            scroll_tab.smoothScrollTo(precScrollX - viewSel.getWidth() * 2, 0);
                        }
                    } else {
                        if (viewSel.getWidth() > viewBounds.width()) {
                            if (precSelPerScroll < position) {
                                scroll_tab.smoothScrollTo(precScrollX + viewSel.getWidth() * 2, 0);
                            } else {
                                scroll_tab.smoothScrollTo(precScrollX - viewSel.getWidth() * 2, 0);
                            }
                        }
                    }

                }
                precScrollX = scroll_tab.getScrollX();
                precSelPerScroll = position;

            }
        });

        mViewPager.setCurrentItem(selezionato);
        System.out.println("EConTab: FinestraElementiActivity ricaricaPageAdapter EXIT");
    }

    protected void setVisualizzazioneCategorie() {
        findViewById(R.id.textView_categorie_comp).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        if (selezionato > 1) {

            scroll_tab.smoothScrollTo(categorie.getChildAt(selezionato).getLeft() - categorie.getChildAt(selezionato).getWidth(), 0);
        }
    }

    protected void creaMenuCategorie() {
        System.out.println("EConTab: FinestraElementiActivity creaMenuCategorie ENTER");
        // TODO Auto-generated method stub
        DbInterno db = new DbInterno(this);
        CategorieGenerali tabCat = new CategorieGenerali();
        ContentValues where = new ContentValues();
        where.put(CategorieGenerali.POSIZIONABILE_SN, 1);
        ArrayList<Object> lista = db.eseguiSelect(tabCat, where, null);
        db.close();
        numeroCategorie = lista.size();

        for (int i = 0; i < lista.size(); i++) {
            ContentValues val = (ContentValues) lista.get(i);
            EConTabItemMenu item = new EConTabItemMenu(this);
            item.setTesto(val.getAsString(CategorieGenerali.NOME));
            item.setIcona(CategorieGenerali.PATH_ICONE, val.getAsString(CategorieGenerali.ICONA));

            item.setOnClickListener(this);
            item.setTag("" + i);
            item.setIdCategoria(val.getAsInteger(CategorieGenerali.ID_CATEGORIA_GENERALE));
            if (listaCategorie == null) {
                listaCategorie = new ArrayList<EConTabItemMenu>();
            }
            listaCategorie.add(item);

            if (categoriaSelezionata == item.getIdCategoria()) {
                selezionato = i;
                item.setSelected(true);
                titolo.setText(item.getTesto());
            }

            categorie.addView(item);

        }
        System.out.println("EConTab: FinestraElementiActivity creaMenuCategorie EXIT");
    }

    @Override
    public void onClick(View v) {
        System.out.println("EConTab: FinestraElementiActivity onClick");
        // TODO Auto-generated method stub
        if (v.getId() == R.id.imageButton_chiudi) {
            setResult(RESULT_CANCELED);
            finish();
        }

        if (v instanceof EConTabItemMenu) {
            scrollAutomatico = false;
            mViewPager.setCurrentItem(Integer.parseInt(v.getTag().toString()));
            scrollAutomatico = true;
        }
    }

    public String getDescrizioneCantiere() {
        return descrizioneCantiere;
    }

    public void setDescrizioneCantiere(String descrizioneCantiere) {
        this.descrizioneCantiere = descrizioneCantiere;
    }

    public String getDescrizionePreventivo() {
        return descrizionePreventivo;
    }

    public void setDescrizionePreventivo(String descrizionePreventivo) {
        this.descrizionePreventivo = descrizionePreventivo;
    }

    public int getIdPreventivoSelezionato() {
        return idPreventivoSelezionato;
    }

    public void setIdPreventivoSelezionato(int idPreventivoSelezionato) {
        this.idPreventivoSelezionato = idPreventivoSelezionato;
    }

    public int getIdCantiere() {
        return idCantiere;
    }

    public void setIdCantiere(int idCantiere) {
        this.idCantiere = idCantiere;
    }

    /**
     * Aggiunge l'elemento cliccato al cantiere/preventivo
     */
    public void aggiungiElemento(final ContentValues val) {
        System.out.println("EConTab: FinestraElementiActivity aggiungiElemento ENTER");
        String nomeElemento = val.getAsString(Elementi.NOME_ELEMENTO);

        boolean erroreComposizioniMancanti = false;

        //controllo che esiata almeno un componente
        int idElemento = val.getAsInteger(Elementi.ID_ELEMENTO);

        if (val.containsKey(Elementi.ID_CATEGORIA_GENERALE)) {

            DbInterno db = new DbInterno(this);
            int idCategoriaGenerale = val.getAsInteger(Elementi.ID_CATEGORIA_GENERALE);
            ContentValues where = new ContentValues();
            where.put(CategorieGenerali.ID_CATEGORIA_GENERALE,idCategoriaGenerale);
            ContentValues valCat =  db.getRecord(new CategorieGenerali(), where);
            if (valCat!=null && valCat.getAsInteger(CategorieGenerali.CONFIGURABILE_SN)==1){
                Composizioni tabComposizioni = new Composizioni();
                ArrayList<Object> composizioni = tabComposizioni.getComposizioneElemento(db, idElemento);
                if (composizioni.size() == 0) {
                    erroreComposizioniMancanti = true;
                }
            }

            db.close();
        }

        if (erroreComposizioniMancanti == true) {
            Utility.mostraDialog(getString(R.string.attenzione),getString(R.string.messaggio_composizoni_mancanti),this,"OK");
        } else {


            // String messaggio =
            // getString(R.string.elemento).toUpperCase(Locale.getDefault())
            // + System.getProperty("line.separator") + nomeElemento;
            // messaggio = messaggio + System.getProperty("line.separator") +
            // System.getProperty("line.separator");
            // messaggio = messaggio +
            // getString(R.string.cantiere).toUpperCase(Locale.getDefault())
            // + System.getProperty("line.separator") + getDescrizioneCantiere();
            // messaggio = messaggio + System.getProperty("line.separator") +
            // System.getProperty("line.separator");
            // messaggio = messaggio +
            // getString(R.string.preventivo_ordine).toUpperCase(Locale.getDefault())
            // + System.getProperty("line.separator") + getDescrizionePreventivo();
            if (mostraAvviso) {
                View checkView = View.inflate(this, R.layout.dialog_checkbox_layout, null);
                final CheckBox checkRicorda = (CheckBox) checkView.findViewById(R.id.checkbox_ricorda);
                final EditText editQta = (EditText) checkView.findViewById(R.id.editText_qta);

                TextView txtGiaPresenti = (TextView) checkView.findViewById(R.id.txtGiaPresenti);
                DbInterno db = new DbInterno(this);

                ArrayList<Object> recsGiaPresenti =  db.eseguiSelect("Select count(*) as giaPresenti from elementi_cantiere where id_elemento=" + val.getAsInteger(Elementi.ID_ELEMENTO)+" and id_locale=" + getIdLocale()+" and id_preventivo=" + getIdPreventivoSelezionato(),null);
                db.close();
                if (recsGiaPresenti!=null && recsGiaPresenti.size()>0){
                    ContentValues valGiaPresenti = (ContentValues) recsGiaPresenti.get(0);
                    txtGiaPresenti.setText(" (Già presenti in questo locale: " + valGiaPresenti.getAsString("giaPresenti")+")");
                }

                String messaggio = getDescrizionePreventivo();

                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        if (which == Dialog.BUTTON_POSITIVE) {
                            if (checkRicorda.isChecked()) {
                                mostraAvviso = false;
                            }
                            int qta;
                            try{
                                qta = Integer.parseInt(editQta.getText().toString());
                            }
                            catch (Exception ex){
                                qta = 1;
                            }
                            eseguiAggiunta(val,qta);
                        }
                    }
                };

                AlertDialog.Builder ab = new AlertDialog.Builder(this);
                ab.setMessage(messaggio);
                ab.setView(checkView);
                ab.setTitle(getString(R.string.conferma_aggiunta_elemento));
                ab.setIcon(android.R.drawable.ic_dialog_alert);
                ab.setPositiveButton(getString(R.string.conferma), listener);
                ab.setNegativeButton(getString(R.string.annulla), listener);

                AlertDialog di = ab.create();

                di.show();
            } else {
                eseguiAggiunta(val,1);
            }
        }
        System.out.println("EConTab: FinestraElementiActivity aggiungiElemento EXIT");
    }

    private void eseguiAggiunta(final ContentValues val,int qta) {
        System.out.println("EConTab: FinestraElementiActivity eseguiAggiunta ENTER");
        for (int i=0;i<qta;i++){
            EConTabElementoElettrico elem = new EConTabElementoElettrico(FinestraElementiActivity.this);
            elem.setIdCantiere(getIdCantiere());
            elem.setIdElementoListino(val.getAsInteger(Elementi.ID_ELEMENTO));
            elem.setIdPreventivo(getIdPreventivoSelezionato());
            elem.aggiungiInDb(FinestraElementiActivity.this, getIdLocale());


        }
        Toast t = Toast.makeText(FinestraElementiActivity.this,
                getString(R.string.elemento_aggiunto) + ": " + val.getAsString(Elementi.NOME_ELEMENTO)+", "+getString(R.string.quantita)+": " + qta, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
        System.out.println("EConTab: FinestraElementiActivity eseguiAggiunta EXIT");
    }

    public int getIdLocale() {
        return idLocale;
    }

    public void setIdLocale(int idLocale) {
        this.idLocale = idLocale;
    }

    public void aggiungiCategoria(EConTabItemMenu item) {
        categorie.addView(item);
    }

    public void resetCategorie() {
        System.out.println("EConTab: FinestraElementiActivity resetCategorie ENTER");
        if (categorie != null) {
            categorie.removeAllViews();
        }
        if (listaCategorie != null) {
            listaCategorie.clear();
        }
        System.out.println("EConTab: FinestraElementiActivity resetCategorie EXIT");
    }


    protected void notifyPageAdapter() {
        if (mSectionsPagerAdapter != null) {
            mSectionsPagerAdapter.notifyDataSetChanged();
        }

    }
}
