package com.seapip.thomas.wearify.Spotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;

import com.seapip.thomas.wearify.Wearify.Token;
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

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

public class NativeController implements Controller, Player.NotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "59fb3493386b4a6f8db44f3df59e5a34";
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);
    private static final int MIN_NETWORK_BANDWIDTH_KBPS = 3000;

    private Context mContext;
    private SpotifyPlayer mPlayer;
    private PlaybackState mCurrentPlaybackState;
    private Metadata mMetadata;
    private BroadcastReceiver mNetworkStateReceiver;
    private HashMap<Integer, Callback<CurrentlyPlaying>> mPlaybackCallbacks;
    private boolean mShuffle;
    private AudioManager mAudioManager;
    private int mVolume;
    private Handler mNetworkHandler;
    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;


    public NativeController(Context context) {
        mContext = context;
        mPlaybackCallbacks = new HashMap<>();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mVolume = 100 * mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkHandler = new Handler();
        com.seapip.thomas.wearify.Wearify.Manager.getToken(mContext, new com.seapip.thomas.wearify.Wearify.Callback() {
            @Override
            public void onSuccess(Token token) {
                if (mPlayer == null) {
                    Config playerConfig = new Config(mContext, token.access_token, CLIENT_ID);

                    mPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                        @Override
                        public void onInitialized(final SpotifyPlayer player) {
                            //Lets handle connection stuff later...
                            player.setConnectivityStatus(new Player.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                }

                                @Override
                                public void onError(Error error) {
                                }
                            }, Connectivity.MOBILE);
                            player.addNotificationCallback(NativeController.this);
                            player.addConnectionStateCallback(NativeController.this);
                        }

                        @Override
                        public void onError(Throwable error) {

                        }
                    });
                } else {
                    mPlayer.login(token.access_token);
                }
            }
        });
    }

    private void unregisterNetworkCallback() {
        if (mNetworkCallback != null) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
        }
    }

    private void getHighBandwidthNetwork(Callback<Void> callback) {
        mNetworkHandler.removeCallbacksAndMessages(null);
        if (isNetworkHighBandwidth()) {
            callback.onSuccess(null);
            return;
        }
        requestHighBandwidthNetwork(callback);
    }

    private boolean isNetworkHighBandwidth() {

        Network network = mConnectivityManager.getBoundNetworkForProcess();
        network = network == null ? mConnectivityManager.getActiveNetwork() : network;
        if (network == null) {
            return false;
        }

        int bandwidth = mConnectivityManager
                .getNetworkCapabilities(network).getLinkDownstreamBandwidthKbps();

        if (bandwidth >= MIN_NETWORK_BANDWIDTH_KBPS) {
            return true;
        }

        return false;
    }

    private void requestHighBandwidthNetwork(final Callback<Void> callback) {
        // Before requesting a high-bandwidth network, ensure prior requests are invalidated.
        unregisterNetworkCallback();

        // Requesting an unmetered network may prevent you from connecting to the cellular
        // network on the user's watch or phone; however, unless you explicitly ask for permission
        // to a access the user's cellular network, you should request an unmetered network.
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(final Network network) {
                mNetworkHandler.removeCallbacksAndMessages(null);
                if(callback != null) {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onLost(Network network) {
                requestHighBandwidthNetwork(null);
            }
        };

        // requires android.permission.CHANGE_NETWORK_STATE
        mConnectivityManager.requestNetwork(request, mNetworkCallback);

        mNetworkHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                addWifiNetwork();
                unregisterNetworkCallback();
            }
        }, NETWORK_CONNECTIVITY_TIMEOUT_MS);
    }

    private void releaseHighBandwidthNetwork() {
        mNetworkHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mConnectivityManager.bindProcessToNetwork(null);
                unregisterNetworkCallback();
            }
        }, NETWORK_CONNECTIVITY_TIMEOUT_MS);
    }

    private void addWifiNetwork() {
        mContext.startActivity(new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"));
    }


    private void play(final String contextUri, final int position, final Callback<Void> callback) {
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
        getHighBandwidthNetwork(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (mShuffle) {
                    Manager.getService(mContext, new Callback<Service>() {
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
        });
    }

    @Override
    public void pause(Callback<Void> callback) {
        releaseHighBandwidthNetwork();
        mPlayer.pause(null);
        if (callback != null) {
            callback.onSuccess(null);
        }
    }

    @Override
    public void resume(final Callback<Void> callback) {
        getHighBandwidthNetwork(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mPlayer.resume(null);
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }
        });
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
            currentlyPlaying.device.id = "native_playback";
            currentlyPlaying.device.name = Build.MODEL;
            currentlyPlaying.device.type = "Watch";
            currentlyPlaying.device.volume_percent = mVolume;
            currentlyPlaying.is_playing = mCurrentPlaybackState.isPlaying;
            currentlyPlaying.shuffle_state = mShuffle;
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
    public void prev(final Callback<Void> callback) {
        getHighBandwidthNetwork(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mPlayer.skipToPrevious(null);
                if(callback != null) {
                    callback.onSuccess(null);
                }
            }
        });
    }

    @Override
    public void next(final Callback<Void> callback) {
        getHighBandwidthNetwork(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mPlayer.skipToNext(null);
                if(callback != null) {
                    callback.onSuccess(null);
                }
            }
        });
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
        mPlayer.logout();
        mPlayer.shutdown();
        mPlayer.destroy();
        Spotify.destroyPlayer(this);
    }

    @Override
    public void onLoggedIn() {
    }

    @Override
    public void onLoggedOut() {
        destroy();
        //Manager.setController(Manager.NATIVE_CONTROLLER, mContext);
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
    }
}
