package com.seapip.thomas.wearify.Spotify;

public interface Controller {

    void play(String uris, String contextUri, int position, Callback<Void> callback);

    void pause(Callback<Void> callback);

    void resume(Callback<Void> callback);

    void shuffle(boolean state, Callback<Void> callback);

    void repeat(String state, Callback<Void> callback);

    void getPlayback(Callback<CurrentlyPlaying> callback);

    void onPlayback(Callback<CurrentlyPlaying> callback);

    void offPlayback();

    void prev(Callback<Void> callback);

    void next(Callback<Void> callback);

    void volume(int volume, Callback<Void> callback);
}
