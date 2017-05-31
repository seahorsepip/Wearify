package com.seapip.thomas.wearify.Spotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.seapip.thomas.wearify.Wearify.Token;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

public class NativeController implements Controller, Player.NotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "59fb3493386b4a6f8db44f3df59e5a34";
    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback() {
        @Override
        public void onSuccess() {
            Log.e("WEARIFY", "OK!!!");
        }

        @Override
        public void onError(Error error) {
            Log.e("WEARIFY", "ERROR: " + error);
        }
    };
    private Context mContext;
    private SpotifyPlayer mPlayer;
    private PlaybackState mCurrentPlaybackState;
    private BroadcastReceiver mNetworkStateReceiver;


    public NativeController(Context context) {
        mContext = context;
        com.seapip.thomas.wearify.Wearify.Manager.getToken(new com.seapip.thomas.wearify.Wearify.Callback() {
            @Override
            public void onSuccess(Token token) {
                if (mPlayer == null) {
                    Config playerConfig = new Config(mContext, token.access_token, CLIENT_ID);
                    mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                        @Override
                        public void onInitialized(SpotifyPlayer player) {
                            mPlayer.setConnectivityStatus(mOperationCallback, getNetworkConnectivity());
                            mPlayer.addNotificationCallback(NativeController.this);
                            mPlayer.addConnectionStateCallback(NativeController.this);
                        }

                        @Override
                        public void onError(Throwable error) {
                            Log.e("WEARIFY", error.getMessage());
                        }
                    });
                } else {
                    mPlayer.login(token.access_token);
                }
            }
        });
    }

    private Connectivity getNetworkConnectivity() {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    @Override
    public void play(String uris, String contextUri, int position, Callback<Void> callback) {
        mPlayer.playUri(mOperationCallback, contextUri, position, 0);
    }

    @Override
    public void pause(Callback<Void> callback) {

    }

    @Override
    public void resume(Callback<Void> callback) {

    }

    @Override
    public void shuffle(boolean state, final Callback<Void> callback) {
        mPlayer.setShuffle(new Player.OperationCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess(null);
            }

            @Override
            public void onError(Error error) {
                callback.onSuccess(null);
                Log.e("WEARIFY", error.toString());
            }
        }, state);
    }

    @Override
    public void repeat(String state, Callback<Void> callback) {

    }

    @Override
    public void getPlayback(Callback<CurrentlyPlaying> callback) {

    }

    @Override
    public Runnable onPlayback(Callback<CurrentlyPlaying> callback) {
        return null;
    }

    @Override
    public void offPlayback(Runnable runnable) {

    }

    @Override
    public void prev(Callback<Void> callback) {

    }

    @Override
    public void next(Callback<Void> callback) {
        mPlayer.skipToNext(null);
    }

    @Override
    public void volume(int volume, Callback<Void> callback) {

    }

    @Override
    public void destroy() {
        Spotify.destroyPlayer(this);
    }

    @Override
    public void onLoggedIn() {

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

    }

    @Override
    public void onPlaybackError(Error error) {

    }
}
