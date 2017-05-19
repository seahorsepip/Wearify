package com.seapip.thomas.wearify;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import com.seapip.thomas.wearify.Wearify.Manager;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

public class PlayNowActivity extends Activity implements
        Player.NotificationCallback, ConnectionStateCallback {

    private static final String TEST_SONG_URI = "spotify:track:2habSXqcJGExM6JJyskY7O";
    private static final String CLIENT_ID = "59fb3493386b4a6f8db44f3df59e5a34";
    private SpotifyPlayer mPlayer;
    private PlaybackState mCurrentPlaybackState;
    private BroadcastReceiver mNetworkStateReceiver;
    private Metadata mMetadata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String token = Manager.getToken();
        onAuthenticationComplete(token);
    }

    private void onAuthenticationComplete(String token) {
        // Once we have obtained an authorization token, we can proceed with creating a Player.
        if (mPlayer == null) {
            Log.d("WEARIFY", token);
            Config playerConfig = new Config(getApplicationContext(), token, CLIENT_ID);
            // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
            // the second argument in order to refcount it properly. Note that the method
            // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
            // one passed in here. If you pass different instances to Spotify.getPlayer() and
            // Spotify.destroyPlayer(), that will definitely result in resource leaks.
            mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    player.setConnectivityStatus(null, getNetworkConnectivity(PlayNowActivity.this));
                    player.addNotificationCallback(PlayNowActivity.this);
                    player.addConnectionStateCallback(PlayNowActivity.this);
                    //Log.d("WEARIFY", mPlayer.getMetadata().toString());
                    // Trigger UI refresh
                }

                @Override
                public void onError(Throwable error) {
                    //Error
                }
            });
        } else {
            mPlayer.login(token);
        }
    }

    private Connectivity getNetworkConnectivity(Context context) {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    @Override
    public void onLoggedIn() {
        mPlayer.playUri(null, TEST_SONG_URI, 0, 0);
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 3, 0);
    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Error error) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {

    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("WEARIFY", playerEvent.toString());
    }

    @Override
    public void onPlaybackError(Error error) {

    }
}
