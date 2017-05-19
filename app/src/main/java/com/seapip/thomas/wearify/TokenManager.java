package com.seapip.thomas.wearify;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TokenManager {
    private SharedPreferences mPreferences;
    private WearifyService mService;

    public TokenManager(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://wearify.seapip.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mService = retrofit.create(WearifyService.class);
    }

    public String getToken() {
        Token token = new Token();
        token.access_token = mPreferences.getString("access_token", null);
        token.refresh_token = mPreferences.getString("refresh_token", null);
        long milliSeconds = mPreferences.getLong("date", 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        Calendar now = Calendar.getInstance();
        if (now.before(calendar)) {
            return token.access_token;
        }
        Call<Token> call = mService.getToken(token.refresh_token);
        try {
            Response<Token> response = call.execute();
            if(response.isSuccessful()) {
                setToken(response.body());
                return token.access_token;
            }
        } catch (IOException e) {
        }
        return null;
    }

    public void setToken(Token token) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, token.expires_in);
        mPreferences.edit()
                .putString("access_token", token.access_token)
                .putString("refresh_token", token.refresh_token)
                .putLong("date", calendar.getTime().getTime())
                .apply();
    }
}
