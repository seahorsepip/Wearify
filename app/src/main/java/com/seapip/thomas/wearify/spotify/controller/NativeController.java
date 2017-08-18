package com.seapip.thomas.wearify.spotify.controller;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import com.seapip.thomas.wearify.AddWifiActivity;
import com.seapip.thomas.wearify.spotify.Callback;
import com.seapip.thomas.wearify.spotify.Service;
import com.seapip.thomas.wearify.spotify.objects.Album;
import com.seapip.thomas.wearify.spotify.objects.Artist;
import com.seapip.thomas.wearify.spotify.objects.CurrentlyPlaying;
import com.seapip.thomas.wearify.spotify.objects.Device;
import com.seapip.thomas.wearify.spotify.objects.Image;
import com.seapip.thomas.wearify.spotify.objects.Playlist;
import com.seapip.thomas.wearify.spotify.objects.Track;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;
import com.seapip.thomas.wearify.wearify.Token;
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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.spotify.Service.NATIVE_CONTROLLER;
import static com.seapip.thomas.wearify.spotify.Service.getWebAPI;

public class NativeController implements Controller, Player.NotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "59fb3493386b4a6f8db44f3df59e5a34";
    private static final long NETWORK_CONNECTIVITY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long NETWORK_CONNECTIVITY_RELEASE_MS = TimeUnit.SECONDS.toMillis(100);
    private static final int MIN_NETWORK_BANDWIDTH_KBPS = 300;

    private Context mContext;
    private Service.Callbacks mCallbacks;
    private SpotifyPlayer mPlayer;
    private PlaybackState mCurrentPlaybackState;
    private Metadata mMetadata;
    private boolean mShuffle;
    private String mRepeat = "off";
    private AudioManager mAudioManager;
    private int mVolume;
    private Handler mNetworkHandler;
    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private CurrentlyPlaying mCurrentlyPlaying;


    public NativeController(Context context, Service.Callbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mVolume = 100 * mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkHandler = new Handler();
        mCurrentlyPlaying = new CurrentlyPlaying();
        mCurrentlyPlaying.context = new com.seapip.thomas.wearify.spotify.objects.Context();
        mCurrentlyPlaying.device = new Device();
        com.seapip.thomas.wearify.wearify.Manager.getToken(mContext, new com.seapip.thomas.wearify.wearify.Callback() {
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
                            player.setPlaybackBitrate(null, PlaybackBitrate.BITRATE_LOW);
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

        return bandwidth >= MIN_NETWORK_BANDWIDTH_KBPS;
    }

    private void requestHighBandwidthNetwork(final Callback<Void> callback) {
        // Before requesting a high-bandwidth network, ensure prior requests are invalidated.
        unregisterNetworkCallback();

        // Requesting an unmetered network may prevent you from connecting to the cellular
        // network on the user's watch or phone; however, unless you explicitly ask for permission
        // to a access the user's cellular network, you should request an unmetered network.
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(final Network network) {
                mNetworkHandler.removeCallbacksAndMessages(null);
                if (callback != null) {
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onLost(Network network) {
                Callback<Void> resumeCallback = null;
                if (mCurrentPlaybackState != null && mCurrentPlaybackState.isPlaying) {
                    resumeCallback = new Callback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mPlayer.resume(null);
                        }
                    };
                    mPlayer.pause(null);
                } else {
                    Toast.makeText(mContext, "Reconnecting...", Toast.LENGTH_LONG).show();
                }
                requestHighBandwidthNetwork(resumeCallback);
            }
        };

        mConnectivityManager.requestNetwork(request, mNetworkCallback);

        mNetworkHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mNetworkHandler.removeCallbacksAndMessages(null);
                mPlayer.pause(null);
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
        }, NETWORK_CONNECTIVITY_RELEASE_MS);
    }

    private void addWifiNetwork() {
        mContext.startActivity(new Intent(mContext, AddWifiActivity.class));
    }

    public void updateCurrentlyPlaying() {
        mCurrentPlaybackState = mPlayer.getPlaybackState();
        if (mMetadata != null && mMetadata.currentTrack != null) {
            mCurrentlyPlaying.item = new Track();
            mCurrentlyPlaying.item.uri = mMetadata.currentTrack.uri;
            mCurrentlyPlaying.item.name = mMetadata.currentTrack.name;
            mCurrentlyPlaying.item.artists = new Artist[1];
            mCurrentlyPlaying.item.artists[0] = new Artist();
            mCurrentlyPlaying.item.artists[0].name = mMetadata.currentTrack.artistName;
            mCurrentlyPlaying.item.album = new Album();
            mCurrentlyPlaying.item.album.images = new Image[1];
            mCurrentlyPlaying.item.album.images[0] = new Image();
            mCurrentlyPlaying.item.album.images[0].url = mMetadata.currentTrack.albumCoverWebUrl;
            mCurrentlyPlaying.item.duration_ms = (int) mMetadata.currentTrack.durationMs;
            mCurrentlyPlaying.context.uri = mMetadata.contextUri;
        }
        if (mCurrentPlaybackState != null) {
            mCurrentlyPlaying.device.is_active = true;
            mCurrentlyPlaying.device.id = "native_playback";
            mCurrentlyPlaying.device.name = Build.MODEL;
            mCurrentlyPlaying.device.type = "Native";
            mCurrentlyPlaying.device.volume_percent = mVolume;
            mCurrentlyPlaying.is_playing = mCurrentPlaybackState.isPlaying;
            mCurrentlyPlaying.shuffle_state = mShuffle;
            mCurrentlyPlaying.repeat_state = mRepeat;
            mCurrentlyPlaying.progress_ms = (int) mCurrentPlaybackState.positionMs;
        }
    }

    @Override
    public void getPlayback(Callback<CurrentlyPlaying> callback) {
        updateCurrentlyPlaying();
        callback.onSuccess(mCurrentlyPlaying);
    }

    private void play(final String[] uris, final String contextUri,
                      final int position, final int positionMs) {
        mPlayer.playUri(new Player.OperationCallback() {
            @Override
            public void onSuccess() {
                if (uris != null) {
                    queue(uris, 1);
                    /*
                    Handler handler = new Handler();
                    for (int i = 1; i < uris.length; i++) {
                        final int offset = i;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mPlayer.queue(null, uris[offset]);
                            }
                        }, 500 * i);
                    }*/
                }
                mPlayer.setShuffle(null, mShuffle);
                mPlayer.setRepeat(null, !mRepeat.equals("off"));
                mPlayer.seekToPosition(null, positionMs);
            }

            @Override
            public void onError(Error error) {

            }
        }, contextUri != null ? contextUri : uris[0], contextUri != null ? position : 0, 0);
    }

    private void queue(final String[] uris, final int offset) {
        if (offset > uris.length - 1) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPlayer.queue(null, uris[offset]);
                    queue(uris, offset + 1);
                }
            }, 500);
        }
    }

    @Override
    public void play(final String[] uris, final String contextUri, final int position,
                     boolean shuffleState, String repeatState, final int positionMs) {
        shuffle(shuffleState);
        repeat(repeatState);
        getHighBandwidthNetwork(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (mShuffle) {
                    getWebAPI(mContext, new Callback<WebAPI>() {
                        @Override
                        public void onSuccess(WebAPI webAPI) {
                            if (contextUri == null) {
                                int position = ThreadLocalRandom.current().nextInt(0, uris.length);
                                play(uris, null, position, positionMs);
                                return;
                            } else if (contextUri.contains(":playlist:")) {
                                Call<Playlist> call = webAPI.getPlaylist(contextUri.split(":")[2],
                                        contextUri.split(":")[4], "tracks.total", "from_token");
                                call.enqueue(new retrofit2.Callback<Playlist>() {
                                    @Override
                                    public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                                        if (response.isSuccessful()) {
                                            Playlist playlist = response.body();
                                            int position = ThreadLocalRandom.current().nextInt(0, playlist.tracks.total);
                                            play(null, contextUri, position, positionMs);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Playlist> call, Throwable t) {

                                    }
                                });
                            } else if (contextUri.contains(":album:")) {
                                Call<Album> call = webAPI.getAlbum(contextUri.split(":")[2], "from_token");
                                call.enqueue(new retrofit2.Callback<Album>() {
                                    @Override
                                    public void onResponse(Call<Album> call, Response<Album> response) {
                                        if (response.isSuccessful()) {
                                            Album album = response.body();
                                            int position = ThreadLocalRandom.current().nextInt(0, album.tracks.total);
                                            play(null, contextUri, position, positionMs);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Album> call, Throwable t) {

                                    }
                                });
                            }
                        }
                    });
                    return;
                }
                play(uris, contextUri, position, positionMs);
            }
        });
    }

    @Override
    public void pause() {
        releaseHighBandwidthNetwork();
        mPlayer.pause(null);
    }

    @Override
    public void resume() {
        getHighBandwidthNetwork(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mPlayer.resume(null);
            }
        });
    }

    @Override
    public void shuffle(boolean state) {
        mShuffle = state;
        if (mCurrentPlaybackState != null && mCurrentPlaybackState.isPlaying) {
            mPlayer.setShuffle(null, state);
        }
    }

    @Override
    public void repeat(String state) {
        mRepeat = state;
        if (mCurrentPlaybackState != null && mCurrentPlaybackState.isPlaying) {
            mPlayer.setRepeat(null, !state.equals("off"));
        }
    }

    @Override
    public void previous() {
        getHighBandwidthNetwork(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mPlayer.skipToPrevious(null);
            }
        });
    }

    @Override
    public void next() {
        getHighBandwidthNetwork(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mPlayer.skipToNext(null);
            }
        });
    }

    @Override
    public void volume(int volume) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume > 0 ?
                volume * mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 100 : 0, 0);
        mVolume = volume;
        updateCurrentlyPlaying();
        mCallbacks.onPlaybackVolume(mCurrentlyPlaying, NATIVE_CONTROLLER);
    }

    @Override
    public void seek(int positionMs) {
        mPlayer.seekToPosition(null, positionMs);
    }

    @Override
    public void destroy() {
        Spotify.destroyPlayer(this);
    }

    @Override
    public void bind() {
        mCurrentPlaybackState = mPlayer.getPlaybackState();
        mMetadata = mPlayer.getMetadata();
        updateCurrentlyPlaying();
        mCallbacks.onPlaybackBind(mCurrentlyPlaying, NATIVE_CONTROLLER);
    }

    @Override
    public void onLoggedIn() {
    }

    @Override
    public void onLoggedOut() {
        destroy();
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
        updateCurrentlyPlaying();
        switch (playerEvent) {
            case kSpPlaybackNotifyPlay:
            case kSpPlaybackNotifyPause:
                mCallbacks.onPlaybackState(mCurrentlyPlaying, NATIVE_CONTROLLER);
                mCallbacks.onPlaybackDevice(mCurrentlyPlaying, NATIVE_CONTROLLER);
                break;
            case kSpPlaybackNotifyNext:
                mCallbacks.onPlaybackNext(mCurrentlyPlaying, NATIVE_CONTROLLER);
                break;
            case kSpPlaybackNotifyPrev:
                mCallbacks.onPlaybackPrevious(mCurrentlyPlaying, NATIVE_CONTROLLER);
                break;
            case kSpPlaybackNotifyShuffleOn:
            case kSpPlaybackNotifyShuffleOff:
                mCallbacks.onPlaybackShuffle(mCurrentlyPlaying, NATIVE_CONTROLLER);
                break;
            case kSpPlaybackNotifyRepeatOn:
            case kSpPlaybackNotifyRepeatOff:
                mCallbacks.onPlaybackRepeat(mCurrentlyPlaying, NATIVE_CONTROLLER);
                break;
            case kSpPlaybackNotifyTrackChanged:
            case kSpPlaybackNotifyMetadataChanged:
                mCallbacks.onPlaybackMetaData(mCurrentlyPlaying, NATIVE_CONTROLLER);
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {

    }
}
