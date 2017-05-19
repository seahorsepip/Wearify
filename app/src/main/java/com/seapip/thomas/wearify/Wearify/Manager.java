package com.seapip.thomas.wearify.Wearify;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Manager {
    static private Token mToken;
    static private Service mService;
    static private SharedPreferences mPreferences;

    static public void init(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://wearify.seapip.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mService = retrofit.create(Service.class);
    }

    public static Service getService() {
        return mService;
    }

    static public String getToken() {
        return mToken.access_token;
        /*
        if (mToken == null) {
            mToken = new Token();
            mToken.refresh_token = mPreferences.getString("refresh_token", null);
        }
        if (mToken.date != null && Calendar.getInstance().before(mToken.date)) {
            return mToken.access_token;
        }
        refresh();
        return getToken();
        */
    }

    static public void setToken(Token token) {
        mToken = token;
        mToken.date = Calendar.getInstance();
        mToken.date.add(Calendar.SECOND, mToken.expires_in);
        mPreferences.edit().putString("refresh_token", mToken.refresh_token).apply();
    }

    static private void refresh() {
        Call<Token> call = mService.getToken(mToken.refresh_token);
        try {
            Response<Token> response = call.execute();
            if (response.isSuccessful()) {
                setToken(response.body());
            } else {
                //Wait 5 seconds before trying again
                Thread.sleep(5000);
                Log.d("WEARIFY", "Failed to refresh token!");
            }
        } catch (IOException | InterruptedException e) {
        }
    }
}
