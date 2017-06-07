package com.seapip.thomas.wearify.Spotify.Controller;

public interface Controller {

    void play(String uris, String contextUri, int position);

    void pause();

    void resume();

    void shuffle(boolean state);

    void repeat(String state);

    void previous();

    void next();

    void volume(int volume);

    void seek(int positionMs);

    void destroy();

    void bind();
}
