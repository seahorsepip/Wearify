package com.seapip.thomas.wearify.spotify.controller;

import com.seapip.thomas.wearify.spotify.Callback;
import com.seapip.thomas.wearify.spotify.objects.CurrentlyPlaying;

public interface Controller {

    void play(String[] uris, String contextUri, int position,
              boolean shuffleState, final String repeatState, final int positionMs);

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

    void getPlayback(Callback<CurrentlyPlaying> callback);

    interface Callbacks {

        void onPlaybackState(CurrentlyPlaying currentlyPlaying);

        void onPlaybackShuffle(CurrentlyPlaying currentlyPlaying);

        void onPlaybackRepeat(CurrentlyPlaying currentlyPlaying);

        void onPlaybackPrevious(CurrentlyPlaying currentlyPlaying);

        void onPlaybackNext(CurrentlyPlaying currentlyPlaying);

        void onPlaybackVolume(CurrentlyPlaying currentlyPlaying);

        void onPlaybackSeek(CurrentlyPlaying currentlyPlaying);

        void onPlaybackMetaData(CurrentlyPlaying currentlyPlaying);

        void onPlaybackDevice(CurrentlyPlaying currentlyPlaying);

        void onPlaybackBuffering();
    }
}
