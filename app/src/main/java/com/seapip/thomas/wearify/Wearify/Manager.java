package com.seapip.thomas.wearify.Wearify;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Manager {
    static private Token mToken;
    static private Service mService;

    public static Service getService() {
        if(mService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://wearify.seapip.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            mService = retrofit.create(Service.class);
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
        Call<Token> call = getService().getToken(mToken.refresh_token);
        call.enqueue(new retrofit2.Callback<Token>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.isSuccessful()) {
                    setToken(context, response.body());
                    callback.onSuccess(mToken);
                } else {
                    callback.onError();
                }
            }

            @Override
            public void onFailure(Call<Token> call, Throwable t) {
                callback.onError();
            }
        });
    }
}
