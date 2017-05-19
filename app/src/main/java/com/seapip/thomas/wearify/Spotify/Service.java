package com.seapip.thomas.wearify.Spotify;

import retrofit2.Call;
import retrofit2.http.GET;

public interface Service {
    @GET("/v1/me/player/recently-played")
    Call<CursorPaging<PlayHistory>> getRecentPlayed();
}