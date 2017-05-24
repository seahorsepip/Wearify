package com.seapip.thomas.wearify.Spotify;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface Service {
    @GET("me/player/recently-played")
    Call<CursorPaging<PlayHistory>> getRecentPlayed(@Query("limit") int limit);

    @GET("users/{user_id}/playlists/{playlist_id}")
    Call<Playlist> getPlaylist(@Path("user_id") String userId,
                               @Path("playlist_id") String playlistId,
                               @Query("fields") String fields);

    @GET("users/{user_id}")
    Call<User> getUser(@Path("user_id") String userId);

    @GET("albums/{id}")
    Call<Album> getAlbum(@Path("id") String id);
}