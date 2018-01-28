package com.seapip.thomas.wearify.wearify;

import android.content.Context;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Manager {
    static private Token mToken;
    static private Service mService;

    private static void createService() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS);
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                okhttp3.Response response = chain.proceed(request);
                int tryCount = 0;
                while (!response.isSuccessful() && tryCount < 10) {
                    tryCount++;
                    response = chain.proceed(request);
                }
                return response;
            }
        });
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://wearify.seapip.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        mService = retrofit.create(Service.class);
    }

    public static Service getService() {
        if (mService == null) {
            createService();
        }
        return mService;
    }

    private static Service getService(Callback callback) {
        if (mService == null) {
            createService();
        }
        return mService;
    }

    static public void getToken(Context context, Callback callback) {
        if (mToken == null) {
            mToken = new Token();
            mToken.refresh_token = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("refresh_token", null);
            if (mToken.refresh_token == null) {
                callback.onError();
                return;
            }
        }
        if (mToken.date != null && Calendar.getInstance().before(mToken.date)) {
            callback.onSuccess(mToken);
            return;
        }
        refresh(context, callback);
    }

    static public void setToken(Context context, Token token) {
        if (token.refresh_token == null) {
            token.refresh_token = mToken.refresh_token;
        }
        mToken = token;
        mToken.date = Calendar.getInstance();
        mToken.date.add(Calendar.SECOND, mToken.expires_in);
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("refresh_token", mToken.refresh_token).apply();
    }

    static private void refresh(final Context context, final Callback callback) {
        Call<Token> call = getService(callback).getToken(mToken.refresh_token);
        call.enqueue(new retrofit2.Callback<Token>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.isSuccessful()) {
                    setToken(context, response.body());
                    callback.onSuccess(mToken);
                }
            }

            @Override
            public void onFailure(Call<Token> call, Throwable t) {
                callback.onError();
            }
        });
    }

    static public void removeToken(Context context) {
        mToken = null;
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("refresh_token").apply();
    }
}
