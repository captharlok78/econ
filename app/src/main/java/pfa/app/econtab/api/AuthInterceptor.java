package pfa.app.econtab.api;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Aggiunge il JWT Bearer token ad ogni richiesta verso Mercury. */
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public AuthInterceptor(Context context) {
        this.tokenManager = TokenManager.getInstance(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = tokenManager.getToken();

        Request original = chain.request();
        Request.Builder builder = original.newBuilder();

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        builder.header("Accept", "application/json");
        builder.header("Content-Type", "application/json");

        return chain.proceed(builder.build());
    }
}
