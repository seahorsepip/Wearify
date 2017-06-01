package com.seapip.thomas.wearify.Spotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.seapip.thomas.wearify.Wearify.Token;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackBitrate;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import retrofit2.Call;
import retrofit2.Response;

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
    private String mDeviceId;
    private Context mContext;
    private SpotifyPlayer mPlayer;
    private PlaybackState mCurrentPlaybackState;
    private Metadata mMetadata;
    private BroadcastReceiver mNetworkStateReceiver;
    private HashMap<Integer, Callback<CurrentlyPlaying>> mPlaybackCallbacks;
    private boolean mShuffle;
    private AudioManager mAudioManager;
    private int mVolume;


    public NativeController(Context context) {
        mContext = context;
        mPlaybackCallbacks = new HashMap<>();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mVolume = 100 * mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        com.seapip.thomas.wearify.Wearify.Manager.getToken(new com.seapip.thomas.wearify.Wearify.Callback() {
            @Override
            public void onSuccess(Token token) {
                if (mPlayer == null) {
                    Config playerConfig = new Config(mContext, token.access_token, CLIENT_ID);
                    Log.e("WEARIFY CLIENTID: ", playerConfig.clientId);
                    Log.e("WEARIFY UNIQUEID: ", playerConfig.uniqueId);

                    mDeviceId = playerConfig.uniqueId;
                    Log.e("CONFIG", playerConfig.brandName);
                    Log.e("CONFIG", playerConfig.cachePath);
                    Log.e("CONFIG", playerConfig.displayName);
                    Log.e("CONFIG", playerConfig.modelName);
                    Log.e("CONFIG", playerConfig.oauthToken);
                    Log.e("CONFIG", playerConfig.osVersion);

                    mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                        @Override
                        public void onInitialized(final SpotifyPlayer player) {
                            getNetworkConnectivity(new Callback<Connectivity>() {
                                @Override
                                public void onSuccess(Connectivity connectivity) {
                                    player.setConnectivityStatus(mOperationCallback, connectivity);
                                    player.addNotificationCallback(NativeController.this);
                                    player.addConnectionStateCallback(NativeController.this);
                                }
                            });
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

    private void getNetworkConnectivity(final Callback<Connectivity> callback) {
        int MIN_BANDWIDTH_KBPS = 320;

        final ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager.getActiveNetwork();

        final Runnable onConnected = new Runnable() {
            @Override
            public void run() {
                Network network = connectivityManager.getActiveNetwork();
                connectivityManager.bindProcessToNetwork(network);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    Log.e("WEARIFY", "CONNECTIVITY???");
                    Log.e("WEARIFY", String.valueOf(networkInfo.getType()));
                    Log.e("WEARIFY", String.valueOf(ConnectivityManager.TYPE_WIFI));
                    if (networkInfo.getType() == 16) {
                        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                        WifiManager.WifiLock lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LockTag");
                        lock.acquire();
                        Log.e("WEARIFY", "LOCKED!!!");
                    }
                    callback.onSuccess(Connectivity.MOBILE);
                } else {
                    callback.onSuccess(Connectivity.OFFLINE);
                }
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        int bandwidth = -1;
        if (activeNetwork != null) {
            bandwidth = connectivityManager.getNetworkCapabilities(activeNetwork).getLinkDownstreamBandwidthKbps();
            if (bandwidth < 3000) {
                final Handler handler = new Handler();

                connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        handler.removeCallbacksAndMessages(null);
                        onConnected.run();
                    }
                });

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mContext.startActivity(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"));
                    }
                }, 5000);
            } else {
                onConnected.run();
            }
        } else {
            mContext.startActivity(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"));
        }
    }

    @Override
    public String deviceId() {
        return mDeviceId;
    }

    private void play(String contextUri, int position, final Callback<Void> callback) {
        mPlayer.playUri(new Player.OperationCallback() {
            @Override
            public void onSuccess() {
                mPlayer.setShuffle(null, mShuffle);
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onError(Error error) {

            }
        }, contextUri, position, 0);
    }

    @Override
    public void play(final String uris, final String contextUri, final int position, final Callback<Void> callback) {
        mPlayer.setPlaybackBitrate(new Player.OperationCallback() {
            @Override
            public void onSuccess() {
                if (mShuffle) {
                    Manager.getService(new Callback<Service>() {
                        @Override
                        public void onSuccess(Service service) {
                            if (contextUri.split(":")[3].equals("playlist")) {
                                Call<Playlist> call = service.getPlaylist(contextUri.split(":")[2],
                                        contextUri.split(":")[4], "tracks.total", "from_token");
                                call.enqueue(new retrofit2.Callback<Playlist>() {
                                    @Override
                                    public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                                        if (response.isSuccessful()) {
                                            Playlist playlist = response.body();
                                            int position = ThreadLocalRandom.current().nextInt(0, playlist.tracks.total);
                                            play(contextUri, position, callback);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Playlist> call, Throwable t) {

                                    }
                                });
                            }
                        }
                    });
                    return;
                }
                play(contextUri, position, callback);
            }

            @Override
            public void onError(Error error) {

            }
        }, PlaybackBitrate.BITRATE_LOW);
    }

    @Override
    public void pause(Callback<Void> callback) {
        mPlayer.pause(null);
    }

    @Override
    public void resume(Callback<Void> callback) {
        mPlayer.resume(null);
    }

    @Override
    public void shuffle(boolean state, final Callback<Void> callback) {
        mShuffle = state;
        if (mCurrentPlaybackState != null && mCurrentPlaybackState.isPlaying) {
            mPlayer.setShuffle(new Player.OperationCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                }

                @Override
                public void onError(Error error) {

                }
            }, state);
        } else {
            callback.onSuccess(null);
        }
    }

    @Override
    public void repeat(String state, Callback<Void> callback) {

    }

    @Override
    public void getPlayback(Callback<CurrentlyPlaying> callback) {
        CurrentlyPlaying currentlyPlaying = new CurrentlyPlaying();
        if (mMetadata != null && mMetadata.currentTrack != null && mCurrentPlaybackState != null) {
            currentlyPlaying.item = new Track();
            currentlyPlaying.item.uri = mMetadata.currentTrack.uri;
            currentlyPlaying.item.name = mMetadata.currentTrack.name;
            currentlyPlaying.item.artists = new Artist[1];
            currentlyPlaying.item.artists[0] = new Artist();
            currentlyPlaying.item.artists[0].name = mMetadata.currentTrack.artistName;
            currentlyPlaying.item.album = new Album();
            currentlyPlaying.item.album.images = new Image[1];
            currentlyPlaying.item.album.images[0] = new Image();
            currentlyPlaying.item.album.images[0].url = mMetadata.currentTrack.albumCoverWebUrl;
            currentlyPlaying.device = new Device();
            currentlyPlaying.device.is_active = true;
            currentlyPlaying.device.id = mDeviceId;
            currentlyPlaying.device.name = Build.MODEL;
            currentlyPlaying.device.type = "Watch";
            currentlyPlaying.device.volume_percent = mVolume;
            currentlyPlaying.is_playing = mCurrentPlaybackState.isPlaying;
            currentlyPlaying.shuffle_state = mShuffle;
            Log.e("NOWPLAYING", String.valueOf(currentlyPlaying.device.volume_percent));
        }
        callback.onSuccess(currentlyPlaying);
    }

    @Override
    public Runnable onPlayback(Callback<CurrentlyPlaying> callback) {
        getPlayback(callback);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

            }
        };
        mPlaybackCallbacks.put(runnable.hashCode(), callback);
        return runnable;
    }

    @Override
    public void offPlayback(Runnable runnable) {
        mPlaybackCallbacks.remove(runnable.hashCode());
    }

    @Override
    public void prev(Callback<Void> callback) {
        mPlayer.skipToPrevious(null);
    }

    @Override
    public void next(Callback<Void> callback) {
        mPlayer.skipToNext(null);
    }

    @Override
    public void volume(int volume, Callback<Void> callback) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume > 0 ?
                volume * mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100 : 0, 0);
        mVolume = volume;
        updatePlayback();
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    @Override
    public void destroy() {
        Log.e("WEARIFY", "DESTROY!!!");
        mPlayer.logout();
        mPlayer.shutdown();
        mPlayer.destroy();
        Spotify.destroyPlayer(this);
    }

    @Override
    public void onLoggedIn() {
        Log.e("WEARIFY", "LOGGEDIN!!!");
    }

    @Override
    public void onLoggedOut() {
        Log.e("WEARIFY", "LOGOUT???");

    }

    @Override
    public void onLoginFailed(Error error) {
        Log.e("WEARIFY", error.toString());
    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {
        Log.e("WEARIFY", s);
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        mCurrentPlaybackState = mPlayer.getPlaybackState();
        mMetadata = mPlayer.getMetadata();
        updatePlayback();
    }

    private void updatePlayback() {
        for (Callback<CurrentlyPlaying> callback : mPlaybackCallbacks.values()) {
            getPlayback(callback);
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.e("WEARIFY", error.toString());
    }
}
