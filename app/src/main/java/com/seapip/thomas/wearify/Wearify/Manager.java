package com.seapip.thomas.wearify.Wearify;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
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
    }

    static public void onToken(Runnable runnable) {
        if (mToken == null) {
            mToken = new Token();
            mToken.refresh_token = mPreferences.getString("refresh_token", null);
            if (mToken.refresh_token == null) {
                Log.d("WEARIFY", "Uhh ohh??!");
                return;
            }
        }
        if (mToken.date != null && Calendar.getInstance().before(mToken.date)) {
            runnable.run();
            return;
        }
        refresh(runnable);
    }

    static public void setToken(Token token) {
        mToken = token;
        mToken.date = Calendar.getInstance();
        mToken.date.add(Calendar.SECOND, mToken.expires_in);
        mPreferences.edit().putString("refresh_token", mToken.refresh_token).commit();
    }

    static private void refresh(final Runnable runnable) {
        Call<Token> call = mService.getToken(mToken.refresh_token);
        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.isSuccessful()) {
                    Log.d("WEARIFY", "Maybe?");
                    setToken(response.body());
                    onToken(runnable);
                } else {
                    //Wait 5 seconds before trying again
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    Log.d("WEARIFY", "Failed to refresh token!");
                }
            }

            @Override
            public void onFailure(Call<Token> call, Throwable t) {

            }
        });
    }
}
