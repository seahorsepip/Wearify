package com.seapip.thomas.wearify.Spotify;

import com.seapip.thomas.wearify.SpotifyService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Manager {
    static private SpotifyService mService;

    public static SpotifyService getService() {
        if (mService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://wearify.seapip.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            mService = retrofit.create(SpotifyService.class);
        }
        return mService;
    }
}
