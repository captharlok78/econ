package pfa.app.econtab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageInfo;
import android.util.Base64;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import pfa.app.econtab.api.TokenManager;
import pfa.app.econtab.utils.Sessione;

public class StartActivity extends EConTabActivity {

    private static final int REQUEST_PERMISSIONS = 1234;
    private static Handler splashHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mostraVersioneReale();

        if (hasRequiredPermissions()) {
            scheduleStart();
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_PERMISSIONS);
        }
    }

    private void mostraVersioneReale() {
        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.textViewVersione)).setText("v " + pinfo.versionName);
        } catch (Exception ignored) {
        }
    }

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void scheduleStart() {
        if (splashHandler == null) {
            splashHandler = new Handler(Looper.getMainLooper());
            splashHandler.postDelayed(() -> {
                splashHandler = null;
                if (!isFinishing() && !isDestroyed()) inizia();
            }, 2500);
        } else {
            inizia();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code == REQUEST_PERMISSIONS
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            scheduleStart();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (splashHandler != null) {
                splashHandler.removeCallbacksAndMessages(null);
                splashHandler = null;
            }
            inizia();
        }
        return super.onTouchEvent(event);
    }

    private void inizia() {
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
            splashHandler = null;
        }

        TokenManager tm = TokenManager.getInstance(this);
        if (tm.hasToken() && isTokenValid() && tm.getIdDitta() > 0) {
            Sessione.setIdOperatore(tm.getUserId(), this);
            Sessione.setDittaSelezionata(tm.getIdDitta());
            Sessione.setNomeDittaSelezionata(tm.getNomeDitta());
            Intent intent = new Intent(this, MenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }

    private boolean isTokenValid() {
        String token = TokenManager.getInstance(this).getToken();
        if (token == null) return false;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            byte[] decoded = Base64.decode(
                    parts[1].replace('-', '+').replace('_', '/'),
                    Base64.DEFAULT);
            JSONObject payload = new JSONObject(new String(decoded, "UTF-8"));
            long exp = payload.optLong("exp", 0);
            return exp > System.currentTimeMillis() / 1000L;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected int getBackgroundID() {
        return NO_BACKGROUND;
    }

    @Override
    protected int getMenuID() {
        return NO_MENU;
    }

    @Override
    protected boolean isControllaRegistrazione() {
        return false;
    }

    @Override
    protected boolean isControllaLogin() {
        return false;
    }
}
