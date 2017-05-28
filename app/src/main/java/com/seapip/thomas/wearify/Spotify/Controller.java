package com.seapip.thomas.wearify.Spotify;

public interface Controller {

    void play(String deviceId, String uris, String contextUri, int position, Callback<Void> callback);

    void shuffle(String deviceId, boolean state, Callback<Void> callback);
}
