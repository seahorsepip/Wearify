package com.seapip.thomas.wearify;

import com.seapip.thomas.wearify.Spotify.CursorPaging;
import com.seapip.thomas.wearify.Spotify.PlayHistory;

import retrofit2.Call;
import retrofit2.http.GET;

public interface SpotifyService {
    @GET("/v1/me/player/recently-played")
    Call<CursorPaging<PlayHistory>> getToken();
}