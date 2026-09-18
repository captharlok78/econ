package pfa.app.econtab.utils;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import androidx.core.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import pfa.app.econtab.R;
import pfa.app.econtab.db.DbInterno;
import pfa.app.econtab.db.table.AbstractTable;
import pfa.app.econtab.db.table.Componenti;
import pfa.app.econtab.db.table.Elementi;
import pfa.app.econtab.db.table.IconeModificate;
import pfa.app.econtab.db.table.Iva;
import pfa.app.econtab.views.EConTabElementoElettrico;

/**
 * Contiene funzioni di comodit� non di logica di business
 * 
 * @author daniele
 * 
 */
public class Utility {
	public static final String PDF = "application/pdf";
	public static final String XLS = "application/vnd.ms-excel";

	public static final String APP_NAME = "ECONTAB";

	public static Long dataToNumber(String data) {
		if (data != null && data.length() > 1) {
			SimpleDateFormat df = Sessione.getDateFormat();
			try {
				Date d = df.parse(data);
				Calendar c = Sessione.getCalendar();

				c.setTime(d);

				int anno = c.get(Calendar.YEAR);
				int mese = c.get(Calendar.MONTH) + 1;
				String meseDecine = "";
				if (mese < 10) {
					meseDecine = "0";
				}

				int giorno = c.get(Calendar.DAY_OF_MONTH);
				String giornoDecine = "";
				if (giorno < 10) {
					giornoDecine = "0";
				}

				return Long.parseLong("" + anno + meseDecine + mese + giornoDecine + giorno + "000000");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return 0l;

	}

	public static Long dataToNumber(Calendar data) {
		if (data != null) {
			int anno = data.get(Calendar.YEAR);
			int mese = data.get(Calendar.MONTH) + 1;
			String meseDecine = "";
			if (mese < 10) {
				meseDecine = "0";
			}

			int giorno = data.get(Calendar.DAY_OF_MONTH);
			String giornoDecine = "";
			if (giorno < 10) {
				giornoDecine = "0";
			}

			int ora = data.get(Calendar.HOUR_OF_DAY);
			String oraDecine = "";
			if (ora < 10) {
				oraDecine = "0";
			}
			int minuti = data.get(Calendar.MINUTE);
			String minutiDecine = "";
			if (minuti < 10) {
				minutiDecine = "0";
			}
			int secondi = data.get(Calendar.SECOND);
			String secondiDecine = "";
			if (secondi < 10) {
				secondiDecine = "0";
			}

			return Long.parseLong("" + anno + meseDecine + mese + giornoDecine + giorno + oraDecine + ora + minutiDecine + minuti
					+ secondiDecine + secondi);
		}
		return 0l;

	}

	public static Calendar numberToDataCalendar(long data) {
		String dataString = numberToData(data);
		Calendar c = Sessione.getCalendar();
		if (dataString != null && dataString.length() > 1) {
			SimpleDateFormat df = Sessione.getDateFormat();

			try {
				Date d = df.parse(dataString);

				c.setTime(d);

			} catch (Exception e) {

			}
		}
		return c;
	}

	public static String numberToData(long data) {
		String dataS = "" + data;
		if (dataS.length() == 14) {
			return dataS.substring(6, 8) + "/" + dataS.substring(4, 6) + "/" + dataS.substring(0, 4);
		}
		return dataS;

	}

	public static String numberToDataShort(long data) {
		String dataS = "" + data;
		if (dataS.length() == 14) {
			return dataS.substring(6, 8) + "/" + dataS.substring(4, 6) + "/" + dataS.substring(2, 4);
		}
		return dataS;

	}

	public static String dataToString() {
		return dataToString(Calendar.getInstance());
	}

	public static String dataToString(Calendar cal) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		return sdf.format(cal.getTime());
	}


    public static String dataToString(Calendar cal,String dateFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(cal.getTime());
    }

	public static String dataOraToString(Calendar cal) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
		return sdf.format(cal.getTime());
	}

	public static boolean isOnline(Context ctx) {
		ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) return false;

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			android.net.Network network = cm.getActiveNetwork();
			if (network == null) return false;
			android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
			return caps != null && (
				caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
				caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
				caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) ||
				caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
			);
		}
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnected();
	}

	public static String formatPrezzo(float prezzo) {
		NumberFormat format = NumberFormat.getCurrencyInstance();

		return format.format(prezzo);
	}

	public static String formatPrezzo(double prezzo) {
		NumberFormat format = NumberFormat.getCurrencyInstance();

		return format.format(prezzo);
	}

	/**
	 * Trasforma il numero nel formato testo
	 * 
	 * @param numero
	 * @return il numero in formato testo formattato
	 */
	public static String formatNumero(double numero) {
		return formatNumero(numero, -1);
	}

	/**
	 * Trasforma il numero nel formato testo
	 * 
	 * @param numero
	 * @return il numero in formato testo formattato
	 */
	public static String formatNumero(double numero, int decimali) {
		NumberFormat form = NumberFormat.getInstance(Locale.ITALY);

		if (decimali >= 0) {
			form.setMinimumFractionDigits(decimali);
			form.setMaximumFractionDigits(decimali);
		}
		String formattato = form.format(numero);

		formattato = replaceAll(formattato, ".", "");
		return formattato;
	}

	/**
	 * Trasforma la stringa in un numero decimnale per essere inserito nel db
	 * 
	 * @param numero
	 * @return il numero in formato db
	 */
	public static double formatNumeroDB(String numero) {
		numero = numero.replaceAll(",", ".");
		if (numero.trim().equals("")) {
			numero = "0";
		}
		return Double.parseDouble(numero);
	}

	/**
	 * Arrotonda il numero passato alle cifre decimali passate
	 * 
	 * @param numero
	 * @param decimali
	 * @return
	 */
	public static double arrotonda(double numero, int decimali) {
		String moltiplicatore = "1";
		for (int i = 0; i < decimali; i++) {
			moltiplicatore = moltiplicatore + "0";
		}
		int moltiplicatoreInt = Integer.parseInt(moltiplicatore);
		numero = Math.round(numero * moltiplicatoreInt);
		numero = numero / moltiplicatoreInt;
		return numero;

	}

	public static final void mostraDialog(String titolo, String messaggio, Context ctx, String testoOK) {
		AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
		ab.setMessage(messaggio);
		ab.setTitle(titolo);
		ab.setIcon(android.R.drawable.ic_dialog_alert);
		ab.setPositiveButton(testoOK, new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				arg0.cancel();
			}
		});

		AlertDialog di = ab.create();
		di.show();
	}

	/**
	 * Mostra un popup di selezione di oggetti semplici (stringhe)
	 * 
	 * @param titolo
	 * @param items
	 * @param ctx
	 * @param listener
	 */
	public static final void mostraSelezioneDialog(String titolo, CharSequence[] items, Context ctx, OnClickListener listener) {
		mostraSelezioneDialog(titolo, items, ctx, listener, true);
	}

	public static final void mostraSelezioneDialog(String titolo, CharSequence[] items, Context ctx, OnClickListener listener,
			boolean cancellabile) {
		AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
		ab.setTitle(titolo);
		ab.setItems(items, listener);
		ab.setIcon(android.R.drawable.ic_dialog_info);
		AlertDialog di = ab.create();
		di.show();
		di.setCancelable(cancellabile);
		di.setCanceledOnTouchOutside(cancellabile);
	}

	public static void mostraConfermaDialog(String titolo, String messaggio, Context ctx, String testoOK, String testoNO,
			OnClickListener listener) {

		AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
		ab.setMessage(messaggio);
		ab.setTitle(titolo);
		ab.setIcon(android.R.drawable.ic_dialog_alert);
		ab.setPositiveButton(testoOK, listener);
		ab.setNegativeButton(testoNO, listener);

		AlertDialog di = ab.create();

		di.show();

	}


    public static final void mostraSelezioneMultiplaDialog(String titolo, CharSequence[] items, final boolean[] selezionati, Context ctx,
                                                           String testoOK, String testoNO,OnClickListener listener) {
        AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
        ab.setTitle(titolo);
        ab.setMultiChoiceItems(items, selezionati, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i, boolean b) {
				selezionati[i] = b;
			}
		});
        ab.setIcon(android.R.drawable.ic_dialog_info);
        ab.setPositiveButton(testoOK, listener);
        ab.setNegativeButton(testoNO, listener);
        AlertDialog di = ab.create();
        di.show();


    }

	public static void mostraConfermaCancellazioneDialog(Context ctx, OnClickListener listener) {

		AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
		ab.setMessage(ctx.getResources().getString(R.string.conferma_cancellazione));
		ab.setTitle(ctx.getResources().getString(R.string.attenzione));
		ab.setIcon(android.R.drawable.ic_dialog_alert);
		ab.setPositiveButton(ctx.getResources().getString(R.string.conferma), listener);
		ab.setNegativeButton(ctx.getResources().getString(R.string.annulla), listener);

		AlertDialog di = ab.create();

		di.show();

	}

	public static void mostraConfermaSalvataggioDialog(Context ctx, OnClickListener listener) {

		AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
		ab.setMessage(ctx.getResources().getString(R.string.conferma_salvataggio));
		ab.setTitle(ctx.getResources().getString(R.string.attenzione));
		ab.setIcon(android.R.drawable.ic_dialog_alert);
		ab.setPositiveButton(ctx.getResources().getString(R.string.conferma), listener);
		ab.setNegativeButton(ctx.getResources().getString(R.string.annulla), listener);

		AlertDialog di = ab.create();

		di.show();

	}

	public static AlertDialog mostraDialogPersonalizzato(String titolo, Context ctx, View viewPersonalizzata, String testoOK,
			String testoNO, OnClickListener listener) {
		return mostraDialogPersonalizzato(titolo, null, ctx, viewPersonalizzata, testoOK, testoNO, listener);
	}

	public static AlertDialog mostraDialogPersonalizzato(String titolo, BitmapDrawable icona, Context ctx, View viewPersonalizzata,
			String testoOK, String testoNO, OnClickListener listener) {
		return mostraDialogPersonalizzato(titolo, icona, ctx, viewPersonalizzata, null, testoOK, testoNO, listener);
	}

	public static AlertDialog mostraDialogPersonalizzato(String titolo, BitmapDrawable icona, Context ctx, View viewPersonalizzata,
			View viewPersonalizzataTitolo, String testoOK, String testoNO, OnClickListener listener) {
		AlertDialog.Builder ab = new AlertDialog.Builder(ctx);

		ab.setPositiveButton(testoOK, listener);
		ab.setNegativeButton(testoNO, listener);

		ab.setView(viewPersonalizzata);

		if (viewPersonalizzataTitolo == null) {
			ab.setTitle(titolo);
		} else {
			ab.setCustomTitle(viewPersonalizzataTitolo);
		}

		if (icona != null) {
			ab.setIcon(icona);
		}

		AlertDialog di = ab.create();

		di.show();

		di.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		return di;
	}

	public static void playSound(final Context ctx, int suono) {
		MediaPlayer mediaPlayer = null;
		mediaPlayer = MediaPlayer.create(ctx, suono);
		mediaPlayer.setVolume(1.0f, 1.0f);
		mediaPlayer.start();

	}

	public static void sendNotification(Context ctx, String titolo, String contenuto, PendingIntent intent) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle(titolo);
		mBuilder.setContentText(contenuto);
		mBuilder.setContentIntent(intent);
		mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		mBuilder.setVibrate(new long[] { 500, 500, 200, 500 });
		mBuilder.setAutoCancel(true);

		NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, mBuilder.build());
	}

	public static String decomprimiFile(String str) throws IOException {
		System.out.println("EConTab: Utility decomprimiFile ENTER");
		if (str == null || str.length() == 0) {
			return str;
		}

		File f = new File(Environment.getExternalStorageDirectory() + File.separator + "result_sinc_gol.txt");
		if (f.exists()) {
			f.delete();
		}
		f.createNewFile();
		GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(Base64.decode(str, Base64.DEFAULT)));
		BufferedReader bf = new BufferedReader(new InputStreamReader(gis));
		String outStr = "";
		String line;
		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		int cont = 0;
		for (int length = 0; (length = bf.read(buffer)) > 0; ) {
			//System.out.println("" + new String(buffer));
			writer.write(buffer, 0, length);
			cont = cont + 1024;
			if (cont > 1024000) {
				cont = 0;
				writer.flush();

				FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory() + File.separator + "result_sinc_gol.txt", true);
				fw.append(writer.toString());
				fw.flush();
				fw.close();

				writer.close();

				writer = new StringWriter();
			}
		}

		bf.close();
		gis.close();
		FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory() + File.separator + "result_sinc_gol.txt", true);
		fw.append(writer.toString());
		fw.flush();
		fw.close();
		writer.close();

		System.out.println("EConTab: Utility decomprimiFile EXIT");

		return Environment.getExternalStorageDirectory() + File.separator + "result_sinc_gol.txt";
	}


	public static String decomprimi(String str) throws IOException {
		System.out.println("EConTab: Utility decomprimi ENTER");

		if (str == null || str.length() == 0) {
			return str;
		}

		System.out.println("EConTab: Utility decomprimi str.length() " + str.length());

		GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(Base64.decode(str, Base64.DEFAULT)));
		BufferedReader bf = new BufferedReader(new InputStreamReader(gis));
		String outStr = "";
		String line;
		while ((line = bf.readLine()) != null) {
			outStr = outStr + line;
		}
		bf.close();
		gis.close();

		System.out.println("EConTab: Utility decomprimi EXIT");

		return outStr;
	}

	public static String comprimi(String str) throws IOException {
		String ret = "";
		if (str == null || str.length() == 0) {
			return str;
		}
		System.out.println("EConTab: Utility comprimi -------------------------------- str: " + str);
		ByteArrayOutputStream os = new ByteArrayOutputStream(str.length());
		GZIPOutputStream gos = new GZIPOutputStream(os);
		gos.write(str.getBytes());
		gos.close();
		byte[] compressed = os.toByteArray();
		os.close();
		ret = Base64.encodeToString(compressed, Base64.DEFAULT);
		System.out.println("EConTab: Utility comprimi -------------------------------- length: " + ret.length());
		System.out.println("EConTab: Utility comprimi -------------------------------- ret: " + ret);
		return ret;
	}

	/**
	 * Questo � un metodo creato per velocizzare le operazioni di replace nelle stringhe: infatti la funzione replace o
	 * replaceAll della classe String non � molto performante e quando viene richiamata migliaia di volte si nota un
	 * certo rallentamento
	 * 
	 * @param text
	 * @param searchString
	 * @param replacement
	 * @return
	 */
	public static String replaceAll(final String text, final String searchString, final String replacement) {
		if (replacement == null || text == null || text.length() == 0 || searchString == null || searchString.length() == 0) {
			return text;
		}
		int start = 0;
		int end = text.indexOf(searchString, start);
		if (end == -1) {
			return text;
		}
		final int replLength = searchString.length();
		int increase = replacement.length() - replLength;
		increase = increase < 0 ? 0 : increase;
		increase *= 16;
		final StringBuilder buf = new StringBuilder(text.length() + increase);
		while (end != -1) {
			buf.append(text.substring(start, end)).append(replacement);
			start = end + replLength;

			end = text.indexOf(searchString, start);
		}
		buf.append(text.substring(start));
		return buf.toString();
	}

	public static void scaleContents(View rootView, View container, boolean ridimensionaXY) {
		// Compute the scaling ratio

		float xScale = (float) container.getWidth() / rootView.getWidth();
		float yScale = (float) container.getHeight() / rootView.getHeight();
		float scale = Math.min(xScale, yScale);
		// Scale our contents
		if (ridimensionaXY) {
			scaleViewAndChildren(rootView, xScale, yScale);
		} else {
			scaleViewAndChildren(rootView, scale, scale);
		}
	}

	// Scale the given view, its contents, and all of its children by the given
	// factor.
	public static void scaleViewAndChildren(View root, float scaleX, float scaleY) {
		// Retrieve the view's layout information

		ViewGroup.LayoutParams layoutParams = root.getLayoutParams();
		// Scale the view itself
		if (layoutParams.width != ViewGroup.LayoutParams.FILL_PARENT && layoutParams.width != ViewGroup.LayoutParams.WRAP_CONTENT) {
			layoutParams.width *= scaleX;
		}
		if (layoutParams.height != ViewGroup.LayoutParams.FILL_PARENT && layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
			layoutParams.height *= scaleY;

		}
		// If this view has margins, scale those too
		if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
			marginParams.leftMargin *= scaleX;
			marginParams.rightMargin *= scaleX;
			marginParams.topMargin *= scaleY;
			marginParams.bottomMargin *= scaleY;
		}
		// Set the layout information back into the view
		root.setLayoutParams(layoutParams);
		// Scale the view's padding
		root.setPadding((int) (root.getPaddingLeft() * scaleX), (int) (root.getPaddingTop() * scaleY),
				(int) (root.getPaddingRight() * scaleX), (int) (root.getPaddingBottom() * scaleY));
		// If the root view is a TextView, scale the size of its text
		if (root instanceof TextView) {

			TextView textView = (TextView) root;
			// float newsize = textView.getTextSize() * scaleX;

			// textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newsize);
		}

		// If the root view is a ViewGroup, scale all of its children
		// recursively

		if (root instanceof ViewGroup) {
			ViewGroup groupView = (ViewGroup) root;
			for (int cnt = 0; cnt < groupView.getChildCount(); ++cnt)
				scaleViewAndChildren(groupView.getChildAt(cnt), scaleX, scaleY);
		}
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		try {
			return BitmapFactory.decodeResource(res, resId, options);
		} catch (OutOfMemoryError ex) {
			return BitmapFactory.decodeResource(res, R.drawable.ic_launcher, options);
		}

	}

	public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		try {
			return BitmapFactory.decodeFile(path, options);
		} catch (OutOfMemoryError ex) {
			return null;
		}

	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	public static void mostraMappa(Context ctx, String indirizzo) {

		try {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:https://www.google.it/maps?q=" + indirizzo));
			ctx.startActivity(intent);
		} catch (ActivityNotFoundException activityException) {

		}

	}


    public static void chiama(Context ctx, String numero) {
            chiama(ctx,numero,true);
    }


	public static void chiama(Context ctx, String numero,boolean ancheMessaggi) {
		try {
            if (ancheMessaggi){
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                callIntent.setData(Uri.parse("tel:" + numero));
                smsIntent.setData(Uri.parse("sms:" + numero));
                Intent chooserIntent = Intent.createChooser(smsIntent, ctx.getString(R.string.completa_azione_con));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { callIntent });
                ctx.startActivity(chooserIntent);
            }
            else{
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + numero));
                ctx.startActivity(callIntent);
            }




		} catch (ActivityNotFoundException activityException) {

		}
	}

	public static void inviaMail(Context ctx, String indirizzo) {
		inviaMail(ctx, indirizzo, "", "");
	}

	public static void inviaMail(Context ctx, String indirizzo, String oggetto, String testo) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL, new String[] { indirizzo });
		i.putExtra(Intent.EXTRA_SUBJECT, oggetto);
		i.putExtra(Intent.EXTRA_TEXT, testo);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			ctx.startActivity(Intent.createChooser(i, ctx.getString(R.string.completa_azione_con)));
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(ctx, "Nessun client di posta installato", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Costruisce e ritorna l'adapter per la tabella da usare nei campi di autocompletamento Il nome campo serve per
	 * dire qual � il campo da mostrare nella lista
	 * 
	 * @param ctx
	 * @param nomecampo
	 * @return
	 */
	public static ArrayAdapter<Object> getArrayAdapterTabella(Context ctx, ArrayList<Object> lista, final String nomecampo) {
		final ArrayList<EConTabAutoCompleteContentValue> listaAdapter = new ArrayList<EConTabAutoCompleteContentValue>();
		for (int i = 0; i < lista.size(); i++) {
			ContentValues val = (ContentValues) lista.get(i);
			listaAdapter.add(new EConTabAutoCompleteContentValue(val, nomecampo));
		}

		lista.clear();
		lista = null;

        final ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(ctx, R.layout.autocomplete_list_item) {

			ArrayList<Object> suggestions = new ArrayList<Object>();
			int maxResultSize = 20;



			@Override
			public Filter getFilter() {
				// TODO Auto-generated method stub
				Filter filter = new Filter() {


					@Override
					protected FilterResults performFiltering(CharSequence constraint) {
						// TODO Auto-generated method stub

						if (constraint != null) {
							suggestions.clear();

							for (int i = 0; i < listaAdapter.size(); i++) {
								EConTabAutoCompleteContentValue val = listaAdapter.get(i);
								if (val.getContentValue().getAsString(nomecampo).toLowerCase()
										.contains(constraint.toString().toLowerCase())) {
									suggestions.add(val);
									if (suggestions.size() == maxResultSize) {

										ContentValues cvLimite = new ContentValues();
										cvLimite.put(nomecampo, "... Visualizzati i primi " + maxResultSize
												+ " risultati per la chiave di ricerca inserita");
										EConTabAutoCompleteContentValue valLimite = new EConTabAutoCompleteContentValue(cvLimite, nomecampo);

										suggestions.add(valLimite);
										break;
									}
								}
							}
							FilterResults filterResults = new FilterResults();
							filterResults.values = suggestions;
							filterResults.count = suggestions.size();
							return filterResults;
						} else {
							return new FilterResults();
						}

					}

					@Override
					protected void publishResults(CharSequence constraints, FilterResults results) {
                        // TODO Auto-generated method stub

                           ArrayList<Object> filteredList = new ArrayList<Object>();
                            if (results.values!=null) {
                                filteredList.addAll((ArrayList<Object>) results.values);
                                if (results.count > 0) {

                                    clear();
                                    for (Object c : filteredList) {

                                        add(c);
                                    }

                                    notifyDataSetChanged();
                                }
                            }
                       }
				};
				return filter;
			}

			@Override
			public boolean isEnabled(int position) {
				// TODO Auto-generated method stub
				if (position >= maxResultSize) {
					return false;
				}
				return super.isEnabled(position);
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// TODO Auto-generated method stub

				View v = super.getView(position, convertView, parent);

				if (position >= maxResultSize) {

					try {
						((TextView) v.findViewById(android.R.id.text1)).setTextColor(Color.RED);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						((TextView) v.findViewById(android.R.id.text1)).setTextColor(Color.BLACK);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return v;
			}

		};

		return adapter;
	}

	/**
	 * Costruisce e ritorna l'adapter per la tabella da usare nei campi di autocompletamento Il nome campo serve per
	 * dire qual � il campo da mostrare nella lista
	 * 
	 * @param ctx
	 * @param SQL
	 * @param nomecampo
	 * @return
	 */
	public static ArrayAdapter<Object> getArrayAdapterTabella(Context ctx, String SQL, final String nomecampo) {

		DbInterno db = new DbInterno(ctx);
		ArrayList<Object> lista = db.eseguiSelect(SQL, null);
		db.close();

		return getArrayAdapterTabella(ctx, lista, nomecampo);
	}

	/**
	 * Ritorna l'icona associata data la directori e il nome Se non trovata ritorna l'icona di default
	 * 
	 * @param cont
	 * @param dir
	 * @param icona
	 * @return
	 */
	public static Bitmap getIcona(Context cont, String dir, String icona) {
		return _getIcona(cont, dir, icona, false);
	}

	public static Bitmap getIconaThumb(Context cont, String dir, String icona) {
		return _getIcona(cont, dir, icona, true);
	}



	private static Bitmap _getIcona(Context cont, String dir, String icona, boolean thumb) {
		try {

			File dirApp = cont.getDir(dir, Context.MODE_PRIVATE);
			if (thumb) {
				dirApp = new File(dirApp.getAbsolutePath() + File.separator + "thumb");
			}
			File fileIco = new File(dirApp, icona);

			if (!icona.trim().equals("") && fileIco.exists()) {

				Bitmap bitmap = BitmapFactory.decodeFile(dirApp.getAbsolutePath() + File.separator + icona);
				return bitmap;
			} else {
				return BitmapFactory.decodeResource(cont.getResources(), R.drawable.no_image);

			}

		} catch (Exception e) {
			return BitmapFactory.decodeResource(cont.getResources(), R.drawable.ic_launcher);
		}
	}

	public static Bitmap getIconaScalata(Context cont, String dir, String icona) {
        int altezza = cont.getResources().getDimensionPixelSize(R.dimen.altezza_menu_categorie);
        return getIconaScalata(cont,dir,icona,altezza,altezza);

	}

    public static Bitmap getIconaScalata(Context cont, String dir, String icona,int larghezza,int altezza) {
        try {

            File dirApp = cont.getDir(dir, Context.MODE_PRIVATE);
            File fileIco = new File(dirApp, icona);
            if (!icona.trim().equals("") && fileIco.exists()) {
                Bitmap bitmap = ThumbnailUtils.extractThumbnail(
                        BitmapFactory.decodeFile(dirApp.getAbsolutePath() + File.separator + icona), larghezza, altezza);
                // Bitmap bitmap = Bitmap.createScaledBitmap(
                // BitmapFactory.decodeFile(dirApp.getAbsolutePath() +
                // File.separator + icona), altezza, altezza,
                // true);



                return bitmap;
            } else {
                return BitmapFactory.decodeResource(cont.getResources(), R.drawable.no_image);

            }

        } catch (Exception e) {
            return BitmapFactory.decodeResource(cont.getResources(), R.drawable.ic_launcher);
        }
    }

	public static int autoScaleTextViewTextToHeight(TextView tv, int dimensioneContenitore, int maxPixelSize, float minSizeText) {

		final int maxViewHeight = dimensioneContenitore - 10;
		final int maxViewWidth = dimensioneContenitore - 8;

		final String s = tv.getText().toString();

		Rect currentBounds = new Rect();
		float resultingSize = minSizeText;
		tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, resultingSize);
		tv.getPaint().getTextBounds(s, 0, s.length(), currentBounds);

		while (currentBounds.height() < maxViewHeight && currentBounds.width() < maxViewWidth && resultingSize <= maxPixelSize) {
			resultingSize++;
			tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, resultingSize);
			tv.getPaint().getTextBounds(s, 0, s.length(), currentBounds);

		}
		if (currentBounds.height() >= maxViewHeight || currentBounds.width() >= maxViewWidth) {

			tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, resultingSize - 1);
		}

		tv.getPaint().getTextBounds(s, 0, s.length(), currentBounds);

		if (currentBounds.height() > currentBounds.width()) {
			return currentBounds.height();
		} else {
			return currentBounds.width();
		}

	}

	public static boolean salvaIcona(Context cont, String dir, String icona, Bitmap bmp) {
		return salvaIcona(cont,dir,icona,bmp,true,null);
	}

    public static boolean salvaIcona(Context cont, String dir, String icona, Bitmap bmp,boolean quadrata,DbInterno db) {
		System.out.println("EConTab: Utility salvaIcona ENTER");
		System.out.println("EConTab: Utility salvaIcona dir " + dir);
		System.out.println("EConTab: Utility salvaIcona icona " + icona);
		System.out.println("EConTab: Utility salvaIcona quadrata " + quadrata);
        FileOutputStream out = null;
        FileOutputStream outThumb = null;

        File dirApp = cont.getDir(dir, Context.MODE_PRIVATE);
		if(dirApp.exists() && dirApp.isDirectory()) {
			System.out.println("EConTab: Utility salvaIcona dirApp EXISTS");
		}
		else
		{
			System.out.println("EConTab: Utility salvaIcona dirApp DOESNT EXIST");
			dirApp.mkdir();
		}
		/*
		File dirAppThumb = cont.getDir(dir + File.separator + "thumb", Context.MODE_PRIVATE);
		if(dirAppThumb.exists() && dirAppThumb.isDirectory()) {
			System.out.println("EConTab: Utility salvaIcona dirAppThumb EXISTS");
		}
		else
		{
			System.out.println("EConTab: Utility salvaIcona dirAppThumb DOESNT EXIST");
			dirAppThumb.mkdir();
		}
		*/

        File fileIco = new File(dirApp, icona);
        if (fileIco.exists()) {
			System.out.println("EConTab: Utility salvaIcona fileIco EXISTS");
            fileIco.delete();
        }

		File fileIcoThumb = new File(dirApp + File.separator + "thumb", icona);
        if (fileIcoThumb.exists()) {
			System.out.println("EConTab: Utility salvaIcona fileIcoThumb EXISTS");
            fileIcoThumb.delete();
        }
		else
		{
			System.out.println("EConTab: Utility salvaIcona fileIcoThumb DOESNT EXIST");
		}

        try {
            out = new FileOutputStream(fileIco);
            int larghezza = bmp.getWidth();
            int altezza = bmp.getHeight();
            int larghezzaThumb = larghezza/4;
            int altezzaThumb = altezza/4;
            if (quadrata){
                larghezza = 400;
                altezza = 400;
                larghezzaThumb = 100;
                altezzaThumb = 100;
            }
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, larghezza, altezza, true);
            scaled.compress(Bitmap.CompressFormat.PNG, 90, out);

            outThumb = new FileOutputStream(fileIcoThumb);
            Bitmap scaledThumb = Bitmap.createScaledBitmap(bmp, larghezzaThumb, altezzaThumb, true);
            scaledThumb.compress(Bitmap.CompressFormat.PNG, 90, outThumb);

            //salvo le icone da fare l'upload sul db in modo da non aver problemi con la data
			IconeModificate tabiconeMod = new IconeModificate();
			boolean chiudiDB = false;
			if (db==null){
				db = new DbInterno(cont);
				chiudiDB = true;
			}
			ContentValues valIns = tabiconeMod.getValoriLogInserimento(db);
            valIns.put(IconeModificate.PERCORSO_FILE,fileIco.getPath());
			tabiconeMod.inserisciRecord(db, valIns);
			if (chiudiDB){
				db.close();
			}
			System.out.println("EConTab: Utility salvaIcona EXIT");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
			System.out.println("EConTab: Utility salvaIcona EXCEPTION");
            return false;
        } finally {
            try {
                out.close();
                outThumb.close();
            } catch (Throwable ignore) {

            }
        }
    }

	public static String getPercorsoBackup() {
		return Environment.getExternalStorageDirectory() + File.separator + "ECONTAB" + File.separator + "backup";
	}

	public static void backupDatabase(Context ctx,String nomeBackup) throws IOException {
		// comapatto il db che male non fa
		DbInterno db = new DbInterno(ctx);
		db.getReadableDatabase().execSQL("VACUUM");
		db.close();

		Calendar cal = Calendar.getInstance();
        if (nomeBackup.equals("")){
            nomeBackup = ""+dataToNumber(cal);
        }

		String percorsoBackup = getPercorsoBackup();
		File dirBackup = new File(percorsoBackup);
		dirBackup.mkdirs();

		File backupDB = new File(percorsoBackup, dataToNumber(cal) + "_" + DbInterno.DATABASE_NAME); // for
		File backupDBZip = new File(percorsoBackup, nomeBackup + DbInterno.DATABASE_NAME_ZIP); // for

		// example
		// "my_data_backup.db"
		File currentDB = ctx.getDatabasePath(DbInterno.DATABASE_NAME); // databaseName=your current
																		// application database name, for
																		// example "my_data.db"
		if (currentDB.exists()) {
			FileChannel src = new FileInputStream(currentDB).getChannel();
			FileChannel dst = new FileOutputStream(backupDB).getChannel();
			dst.transferFrom(src, 0, src.size());
			src.close();
			dst.close();
			FileOutputStream os = new FileOutputStream(backupDBZip);
			GZIPOutputStream zos = new GZIPOutputStream(os);
			FileInputStream in = new FileInputStream(backupDB);
			try {

				byte[] buf = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(buf)) > 0) {
					zos.write(buf, 0, bytesRead);
				}
			} catch (Exception e) {

			} finally {

				zos.close();
				in.close();
				// cancello il file di backup non zippato
				backupDB.delete();

			}

		}

        //backup delle immagini
        File dirImmagini = new File(percorsoBackup+File.separator+nomeBackup);
        dirImmagini.mkdirs();
        File dirComp = ctx.getDir(Componenti.PATH_ICONE, Context.MODE_PRIVATE);
        File dirElem = ctx.getDir(Elementi.PATH_ICONE, Context.MODE_PRIVATE);
        copiaDirectory(dirComp, new File(dirImmagini.getPath()+File.separator+dirComp.getName()));
        copiaDirectory(dirElem,  new File(dirImmagini.getPath()+File.separator+dirElem.getName()));
	}

	public static void ripristinaDb(final File back, final Context ctx) {
		// TODO Auto-generated method stub
		mostraConfermaDialog(ctx.getResources().getString(R.string.attenzione),
				ctx.getResources().getString(R.string.conferma_ripristino_db), ctx, "OK", ctx.getString(R.string.annulla),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						if (which == DialogInterface.BUTTON_POSITIVE) {
							try {
								File currentDB = ctx.getDatabasePath(DbInterno.DATABASE_NAME);
								if (back.exists()) {
									// sconpatto il backup
									FileInputStream in = new FileInputStream(back);

									GZIPInputStream zin = new GZIPInputStream(in);
									FileOutputStream fout = new FileOutputStream(currentDB);
									byte[] buf = new byte[1024];
									int bytesRead;
									while ((bytesRead = zin.read(buf)) > 0) {
										fout.write(buf, 0, bytesRead);
									}
									zin.close();
									fout.close();


                                    //ripristino le immagini se esiste la cartella
                                    String cartelleaImmagini = back.getName().replace(".econtab.db","");
                                    File dirImmagini = new File(getPercorsoBackup()+File.separator+cartelleaImmagini);
                                    if (dirImmagini.exists()&&dirImmagini.isDirectory()){
                                        File dirComp = new File(dirImmagini+File.separator+ctx.getDir(Componenti.PATH_ICONE,Context.MODE_PRIVATE).getName());
                                        File dirElem = new File(dirImmagini+File.separator+ctx.getDir(Elementi.PATH_ICONE,Context.MODE_PRIVATE).getName());
                                        copiaDirectory(dirComp, ctx.getDir(Componenti.PATH_ICONE,Context.MODE_PRIVATE));
                                        copiaDirectory(dirElem, ctx.getDir(Elementi.PATH_ICONE,Context.MODE_PRIVATE));
                                    }

									Toast.makeText(ctx, ctx.getResources().getString(R.string.messaggio_ripristino_ok), Toast.LENGTH_SHORT)
											.show();
								}

							} catch (Exception e) {
								Toast.makeText(ctx, ctx.getResources().getString(R.string.messaggio_ripristino_ko), Toast.LENGTH_SHORT)
										.show();
							}

						}
					}
				});
	}

	/**
	 * ritona la mappa dei codici iva : chiave = codice, valore = aliquota
	 * 
	 * @param context
	 * @return
	 */
	public static HashMap<String, Double> getMappaCodiciIva(Context context) {
		// TODO Auto-generated method stub

		DbInterno db = new DbInterno(context);
		HashMap<String, Double> mappaIva = getMappaCodiciIva(db);
		db.close();
		return mappaIva;
	}

	/**
	 * ritona la mappa dei codici iva : chiave = codice, valore = aliquota
	 * 
	 * @return
	 */
	public static HashMap<String, Double> getMappaCodiciIva(DbInterno db) {
		// TODO Auto-generated method stub
		HashMap<String, Double> mappaIva = new HashMap<String, Double>();

		ArrayList<Object> records = db.eseguiSelect(new Iva());
		for (int i = 0; i < records.size(); i++) {
			ContentValues val = (ContentValues) records.get(i);
			mappaIva.put(val.getAsString(Iva.CODICE_IVA), val.getAsDouble(Iva.ALIQUOTA));
		}

		return mappaIva;
	}

	public static File getFileImmaginePiantina(int idLocale) {
		// TODO Auto-generated method stub
		File dirExport = new File(getPercorsoPiantine());

		if (!dirExport.exists()) {
			dirExport.mkdirs();
		}

		File filePiantina = new File(dirExport, "LOC" + idLocale + ".png");
		return filePiantina;
	}

	public static File getFileImmaginePiantinaThumb(int idLocale) {
		// TODO Auto-generated method stub
		File dirExport = new File(getPercorsoPiantine() + File.separator + "thumb");

		if (!dirExport.exists()) {
			dirExport.mkdirs();
		}

		File filePiantina = new File(dirExport, "LOC" + idLocale + ".png");
		return filePiantina;
	}

	public static Bitmap getImmaginePiantina(int idLocale, boolean thumb, Context cont) {
		try {
			File dirExport = null;
			if (thumb) {
				dirExport = new File(getPercorsoPiantine() + File.separator + "thumb");

			} else {
				dirExport = new File(getPercorsoPiantine());
			}
			File fileIco = new File(dirExport, "LOC" + idLocale + ".png");

			if (fileIco.exists()) {

				Bitmap bitmap = BitmapFactory.decodeFile(fileIco.getAbsolutePath());

				return bitmap;
			} else {
				return BitmapFactory.decodeResource(cont.getResources(), R.drawable.no_image);

			}

		} catch (Exception e) {
			return BitmapFactory.decodeResource(cont.getResources(), R.drawable.no_image);
		}
	}

	public static String getPercorsoPiantine() {
		return Environment.getExternalStorageDirectory() + File.separator + "ECONTAB" + File.separator + "piantine";
	}

	public static void copiaFile(File source, File dest) throws IOException {
		InputStream input = null;
		OutputStream output = null;

		try {
			input = new FileInputStream(source);
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}

		} finally {

			input.close();
			output.close();

		}

	}



    public static void copiaDirectory(File source,File dest) throws IOException{
        if (!dest.exists()){
            dest.mkdirs();
        }
        //controllo che dest sia una cartella altrimenti chiudo
        if (dest.isDirectory() && source.isDirectory())
        {

            File [] lista = source.listFiles();
            for(int i = 0; i < lista.length; i++)
            {
                if (lista[i].isFile())
                {
                    copiaFile(lista[i], new File(dest.getPath() + File.separator + lista[i].getName()));
                }
                else if (lista[i].isDirectory())
                {
                    //ricorsione
                    copiaDirectory(lista[i], new File(dest.getPath() + File.separator + lista[i].getName()));
                }
            }

        }

    }

	/**
	 * Visualizza un file dalla cartella asset
	 * 
	 * @param ctx
	 * @param fileName
	 * @param tipoFile
	 */
	public static void visualizzaFileAsset(Context ctx, String fileName, String tipoFile) {
		AssetManager assetManager = ctx.getAssets();

		InputStream in = null;
		OutputStream out = null;
		File file = new File(ctx.getFilesDir(), fileName);
		try {
			in = assetManager.open(fileName);
			out = ctx.openFileOutput(file.getName(), Context.MODE_WORLD_READABLE);

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}

			out.flush();

		} catch (Exception e) {
			Log.e("tag", e.getMessage());
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			in = null;
			try {
				out.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			out = null;
		}

		Intent intent = new Intent(Intent.ACTION_VIEW);

		intent.setDataAndType(Uri.parse("file://" + ctx.getFilesDir() + File.separator + fileName), tipoFile);

		ctx.startActivity(intent);
	}

	public static String getColoreCategoria(int idCategoria) {
		// TODO Auto-generated method stub
		if (idCategoria == 1) {
			return EConTabElementoElettrico.COLORE_PRESE;
		}
		if (idCategoria == 2) {
			return EConTabElementoElettrico.COLORE_INTERRUTTORI;

		}
		if (idCategoria == 3) {
			return EConTabElementoElettrico.COLORE_UTILIZZATORI;

		}
		if (idCategoria == 4) {
			return EConTabElementoElettrico.COLORE_QUADRI;

		}
		if (idCategoria == 5) {
			return EConTabElementoElettrico.COLORE_SCATOLE;

		}
		if (idCategoria == 6) {
			return EConTabElementoElettrico.COLORE_ALLARMI;

		}
		if (idCategoria == 9) {
			return EConTabElementoElettrico.COLORE_AUTOMAZIONI;
		}
		if (idCategoria == 10) {
			return EConTabElementoElettrico.COLORE_CITOFONIA;
		}
		if (idCategoria == 11) {
			return EConTabElementoElettrico.COLORE_TELEFONIA;
		}
		if (idCategoria == 12) {
			return EConTabElementoElettrico.COLORE_VARIO;
		}
		return EConTabElementoElettrico.COLORE_CANTIERE;
	}

	public static int getIdSfondoDaCategoria(int idCategoria) {
		if (idCategoria == 1) {
			// return EConTabElementoElettrico.COLORE_PRESE;
			return R.drawable.elemento_cat_prese;
		}
		if (idCategoria == 2) {
			return R.drawable.elemento_cat_interruttori;
		}
		if (idCategoria == 3) {

			return R.drawable.elemento_cat_utilizzatori;
		}
		if (idCategoria == 4) {

			return R.drawable.elemento_cat_quadri;
		}
		if (idCategoria == 5) {
			return R.drawable.elemento_cat_scatole;
		}
		if (idCategoria == 6) {
			return R.drawable.elemento_cat_allarmi;
		}
		if (idCategoria == 9) {
			return R.drawable.elemento_cat_automazioni;
		}
		if (idCategoria == 10) {
			return R.drawable.elemento_cat_citofonia;
		}
		if (idCategoria == 11) {
			return R.drawable.elemento_cat_telefonia;
		}
		if (idCategoria == 12) {
			return R.drawable.elemento_cat_vario;
		}
		return R.drawable.elemento_cantiere;
	}

	public static Boolean isTablet(Context context) {

		if ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE) {

			return true;
		}
		return false;
	}

	public static void ricercaFile(File dir, String filtro, String estensione, ArrayList<File> result) {
		// TODO Auto-generated method stub
		File listFile[] = dir.listFiles();

		if (listFile != null) {
			for (int i = 0; i < listFile.length; i++) {

				if (listFile[i].isDirectory()) {
					ricercaFile(listFile[i], filtro, estensione, result);
				} else {
					if (estensione.equals("") || listFile[i].getName().endsWith("." + estensione)) {

						if (listFile[i].getName().toLowerCase(Locale.getDefault()).contains(filtro.toLowerCase(Locale.getDefault()))) {

							result.add(listFile[i]);
						}
					}

				}
			}
		}

	}

	public static String getURLServer(Context ctx) {
		// TODO Auto-generated method stub
        return ctx.getSharedPreferences(APP_NAME, ctx.MODE_PRIVATE).getString("URL", "http://127.0.0.1:8000/");
	}

	public static String getStringaDaPaginaWeb(String url) {
		System.out.println("EConTab: Utility getStringaDaPaginaWeb ENTER url=" + url);
		OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
				.readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
				.build();
		Request request = new Request.Builder().url(url).build();
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				Log.w("MyApp", "Download Error: " + response.code() + " | for URL: " + url);
				return null;
			}
			ResponseBody body = response.body();
			String result = body != null ? body.string() : null;
			System.out.println("EConTab: Utility getStringaDaPaginaWeb response=" + result);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ArrayList<JSONObject> toJSONArrayList(ArrayList<Object> records) throws JSONException {
		// TODO Auto-generated method stub
		ArrayList<JSONObject> jsonArrayList = new ArrayList<JSONObject>();
		for (int i = 0; i < records.size(); i++) {
			ContentValues curr = (ContentValues) records.get(i);
			JSONObject objRec = new JSONObject();
			Iterator<String> iter = curr.keySet().iterator();
			String nomeCampo = "";
			while (iter.hasNext()) {
				nomeCampo = iter.next();
				objRec.accumulate(nomeCampo, curr.get(nomeCampo));
			}
			jsonArrayList.add(objRec);
		}

		return jsonArrayList;
	}

	public static byte[] getBytes(File file) {

		ByteArrayOutputStream ous = null;
		InputStream ios = null;
		try {
			byte[] buffer = new byte[1024];
			ous = new ByteArrayOutputStream();
			ios = new FileInputStream(file);
			int read = 0;
			while ((read = ios.read(buffer)) != -1) {
				ous.write(buffer, 0, read);
			}
			return ous.toByteArray();
		} catch (IOException ex) {
			Log.e("getBytes " + file.getName(), Log.getStackTraceString(ex));
		} finally {
			try {
				if (ous != null)
					ous.close();
			} catch (IOException e) {
			}

			try {
				if (ios != null)
					ios.close();
			} catch (IOException e) {
			}
		}
		return null;
	}


    public static Bitmap getBitmapFromView(ViewGroup view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        view.draw(canvas);
        return returnedBitmap;
    }


    public static void copyStream(InputStream input, OutputStream output)
            throws IOException {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }


    /**
     *
     * Metodo da usare in debug per avere la stringa json del db corrente
     * @param ctx
     * @return
     * @throws Exception
     */
    public static String getStringaJsonDb(Context ctx) throws Exception {
        JSONObject obj = new JSONObject();
        DbInterno db = new DbInterno(ctx);
        // prendo i nuovi dati (id<0) di tutte le tabelle
        JSONObject tabelle = new JSONObject();
        ArrayList<String> nomiTabelle = db.getNomiTabelle();
        for (int i = 0; i < nomiTabelle.size(); i++) {
            AbstractTable tab = AbstractTable.getIstanzaTabella(nomiTabelle.get(i));
            if (tab != null) {
                ArrayList<JSONObject> nuoviValori = tab.getAllRecord(db);
                tabelle.accumulate(tab.getNomeTabella(), new JSONArray(nuoviValori));
            }
        }
        obj.accumulate("INSERT", tabelle);
        db.close();

        return obj.toString();
    }

    /**
     * Ritorna la stringa dai caratteri non ammessi dal FileSystem di Windows per i nomi file e cartelle
     * @param fileName
     * @return il file pulito
     */
    public static String formattaStringaPerNomeFile(String fileName) {
        String newFileName = fileName.replaceAll("\\\\", "");
        newFileName = newFileName.replaceAll("/", "");
        newFileName = newFileName.replaceAll(":", "");
        newFileName = newFileName.replaceAll("\\*", "");
        newFileName = newFileName.replaceAll("\\?", "");
        newFileName = newFileName.replaceAll("\"", "");
        newFileName = newFileName.replaceAll("<", "");
        newFileName = newFileName.replaceAll(">", "");
        newFileName = newFileName.replaceAll("\\|", "");
        if (newFileName.equals("")){
            newFileName = "temp";
        }
        return  fileName;
    }

    public static void apriSito(Context ctx, String url) {

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        ctx.startActivity(i);
    }

	public static void aggiornaApp(final Context ctx) {
		// DL: TODO
		/*
		AsyncTask<Void, Integer, String> task = new AsyncTask<Void, Integer, String>(){
			ProgressDialog pd = null;
			@Override
			protected String doInBackground(Void... params) {
				// TODO Auto-generated method stub
				String result = "";
				try{
					DefaultHttpClient httpclient = new DefaultHttpClient();

					HttpParams httpParameters = new BasicHttpParams();
					// set the timeout in milliseconds until a connection is established
					// the default value is zero, that means the timeout is not used
					int timeoutConnection = 3000;
					HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
					// set the default socket timeout (SO_TIMEOUT) in milliseconds
					// which is the timeout for waiting for data
					int timeoutSocket = 5000;
					HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

					httpclient.setParams(httpParameters);

					HttpGet get = new HttpGet("http://econtab.mobi/download/econtab.apk");
					HttpEntity entity = httpclient.execute( get ).getEntity();

					if( entity.getContentType().getValue().equalsIgnoreCase("application/vnd.android.package-archive")) {
						String fname = "econtab.apk";
						FileOutputStream fos = ctx.getApplicationContext().openFileOutput( fname, Context.MODE_WORLD_READABLE);
						entity.writeTo(fos);
						fos.close();
						Intent notificationIntent = new Intent(Intent.ACTION_VIEW );
						notificationIntent.setDataAndType(
								Uri.parse("file://" + ctx.getFilesDir().getAbsolutePath() + "/" + fname),
								"application/vnd.android.package-archive");
						notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						ctx.startActivity(notificationIntent);
					}

				}
				catch(Exception e){
					e.printStackTrace();
					result = Log.getStackTraceString(e);
				}
				return result;
			}

			@Override
			protected void onPreExecute() {
				pd = new ProgressDialog(ctx);
				pd.setMax(100);
				pd.setTitle("Download...");
				pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				pd.setCancelable(false);
				pd.setIndeterminate(true);
				pd.show();
			}

			@Override
			protected void onPostExecute(String s) {
				super.onPostExecute(s);
				pd.cancel();
				if (!s.equals("")){
					Utility.mostraDialog("",s,ctx,"OK");
				}
			}
		};
		task.execute();
		*/
	}

	public static String readStringFromAPrivateFile(Context ctx, String filename)
	{
		String s = "";

		try
		{
			FileInputStream fIn = ctx.openFileInput(filename);
			InputStreamReader isr = new InputStreamReader(fIn);
			char[] inputBuffer = new char[pfa.app.econtab.Globals.READ_BLOCK_SIZE];
			int charRead;
			while((charRead = isr.read(inputBuffer))>0)
			{
				// convert the chars to a String
				String readString = String.copyValueOf(inputBuffer, 0, charRead);
				s += readString;
				inputBuffer = new char[pfa.app.econtab.Globals.READ_BLOCK_SIZE];
			}

		}
		catch(IOException ioe)
		{
			s = "";
			ioe.printStackTrace();
		}

		return s;
	}

	public static void writeStringToAPrivateFile(Context ctx, String filename, String string_to_be_saved)
	{
		/*
		boolean boolean_temp = false;
		File data_file = new File(filename);

		if(data_file.exists() == false)
		{
			System.out.println("Utility: writeStringToAPrivateFile FILE NOT EXIST");
			try
			{
				boolean_temp = data_file.createNewFile();
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
				boolean_temp = false;
			}
		}
		else
		{
			boolean_temp = true;
		}

		if(boolean_temp == true)
		{
			try
			{
				FileOutputStream fOut =	new FileOutputStream(data_file);
				OutputStreamWriter osw = new OutputStreamWriter(fOut);
				// write the string to the file
				osw.write(string_to_be_saved);
				osw.flush();
				osw.close();
			}
			catch (IOException ioe)
			{
				System.out.println("Utility: writeStringToAPrivateFile WRITE EXCEPTION");
				ioe.printStackTrace();
			}
		}
		*/
		try {
			OutputStreamWriter out = new OutputStreamWriter(ctx.openFileOutput(filename, 0));
			out.write(string_to_be_saved);
			out.close();
		} catch (Throwable t) {
			System.out.println("Utility: writeStringToAPrivateFile WRITE EXCEPTION");
			t.printStackTrace();
		}
	}
}
