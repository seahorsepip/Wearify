package com.seapip.thomas.wearify.Spotify.Controller;

import com.seapip.thomas.wearify.Spotify.Objects.CurrentlyPlaying;

public interface Callbacks {

    void onPlaybackState(CurrentlyPlaying currentlyPlaying);

    void onPlaybackShuffle(CurrentlyPlaying currentlyPlaying);

    void onPlaybackRepeat(CurrentlyPlaying currentlyPlaying);

    void onPlaybackPrevious(CurrentlyPlaying currentlyPlaying);

    void onPlaybackNext(CurrentlyPlaying currentlyPlaying);

    void onPlaybackVolume(CurrentlyPlaying currentlyPlaying);

    void onPlaybackSeek(CurrentlyPlaying currentlyPlaying);

    void onPlaybackMetaData(CurrentlyPlaying currentlyPlaying);

    void onPlaybackDevice(CurrentlyPlaying currentlyPlaying);
}
