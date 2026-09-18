package pfa.app.econtab.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by daniele on 07/02/2015.
 */
public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
    Context cont = null;
    String dir = "";
    ImageView view = null;
    boolean thumb = false;
    int altezza = 0;
    int larghezza = 0;



    public BitmapWorkerTask(Context cont,String dir,ImageView view,boolean thumb){
        this.cont = cont;
        this.dir = dir;
        this.view = view;
        this.thumb = thumb;

    }


    public void setDimensioni(int larghezza,int altezza){
        this.altezza = altezza;
        this.larghezza = larghezza;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        Bitmap btm = null;
        if (altezza!=0 && larghezza!=0){

           btm =  Utility.getIconaScalata(cont,dir,params[0],larghezza,altezza);

        }
        else{
            if (thumb){
                btm = Utility.getIconaThumb(cont,dir,params[0]);
            }
            else{
                btm = Utility.getIcona(cont,dir,params[0]);
            }
        }
        if (Sessione.getCacheIcone().get(params[0])==null && btm!=null){
            Sessione.getCacheIcone().put(params[0],btm);
        }
        return btm;

    }


    @Override
    protected void onPreExecute() {
        view.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {

        view.setImageBitmap(bitmap);
        view.setVisibility(View.VISIBLE);
    }


    public void caricaIcona(String icona){
        if (Sessione.getCacheIcone().get(icona)!=null){
            view.setImageBitmap(Sessione.getCacheIcone().get(icona));
            view.setVisibility(View.VISIBLE);
        }
        else{
            execute(icona);
        }

    }
}