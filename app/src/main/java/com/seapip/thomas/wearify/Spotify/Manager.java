package com.seapip.thomas.wearify.Spotify;

import android.content.Context;

import com.seapip.thomas.wearify.Wearify.Token;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Manager {
    final static public int NATIVE_CONTROLLER = 0;
    final static public int CONNECT_CONTROLLER = 1;
    final static public int BLUETOOTH_CONTROLLER = 2;
    static private Token mToken;
    static private Retrofit.Builder mBuilder;
    static private Dispatcher mDispatcher;
    static private Service mService;
    static private NativeController mNativeController;
    static private ConnectController mConnectController;
    static private Controller mCurrentController;
    static private HashMap<Integer, Callback<Void>> mDeviceCallbacks;
    static private Runnable mConnectRunnable;
    static private long mTransferring;

    public static void getService(Context context, final Callback<Service> callback) {
        com.seapip.thomas.wearify.Wearify.Manager.getToken(context, new com.seapip.thomas.wearify.Wearify.Callback() {
            @Override
            public void onSuccess(final Token token) {
                if (token != mToken) {
                    mToken = token;
                    if (mBuilder == null) {
                        mBuilder = new Retrofit.Builder()
                                .baseUrl("https://api.spotify.com/v1/")
                                .addConverterFactory(GsonConverterFactory.create());
                    }
                    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
                    httpClient.addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Interceptor.Chain chain) throws IOException {

                            Request request = chain.request();
                            return chain.proceed(request.newBuilder()
                                    .header("Authorization", "Bearer " + token.access_token)
                                    .method(request.method(), request.body())
                                    .build());
                        }
                    });
                    HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
                    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    httpClient.addInterceptor(interceptor);
                    mDispatcher = new Dispatcher();
                    mDispatcher.setMaxRequests(10);
                    httpClient.dispatcher(mDispatcher);
                    mBuilder = mBuilder.client(httpClient.build());
                    mService = mBuilder.build().create(Service.class);
                }
                callback.onSuccess(mService);
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }

    public static void cancelAll() {
        mDispatcher.cancelAll();
    }

    public static Controller getController(Context context) {
        if (mNativeController == null) {
            mNativeController = new NativeController(context);
        }
        if (mConnectController == null) {
            mConnectController = new ConnectController(context);
        }
        if (mCurrentController == null) {
            mCurrentController = mNativeController;
        }
        return mCurrentController;
    }

    public static void transferController(final Context context, final int controller,
                                          final String deviceId) {
        if (mCurrentController != null) {
            if (controller == NATIVE_CONTROLLER && mCurrentController instanceof NativeController) {
                updateDevice();
                return;
            } else if (controller == CONNECT_CONTROLLER && mCurrentController instanceof ConnectController) {
                setDevice(context, deviceId, null);
                updateDevice();
                return;
            }
            mCurrentController.getPlayback(new Callback<CurrentlyPlaying>() {
                @Override
                public void onSuccess(final CurrentlyPlaying currentlyPlaying) {
                    mTransferring = System.currentTimeMillis() + 3000;
                    mCurrentController.pause(new Callback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Callback<Void> transferCallback = new Callback<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    if (currentlyPlaying.context == null) {
                                        //Workaround: https://github.com/spotify/web-api/issues/565
                                        if (currentlyPlaying.item != null) {
                                            playTransfer(currentlyPlaying.item.uri, null, 0,
                                                    currentlyPlaying.shuffle_state,
                                                    currentlyPlaying.repeat_state,
                                                    currentlyPlaying.progress_ms);
                                        }
                                    } else if (currentlyPlaying.context.uri.contains(":playlist:")) {
                                        getPlaylistTrackNumber(context, currentlyPlaying.context.uri,
                                                currentlyPlaying.item.uri, 50, 0,
                                                new Callback<Integer>() {
                                                    @Override
                                                    public void onSuccess(final Integer position) {
                                                        playTransfer(null,
                                                                currentlyPlaying.context.uri,
                                                                position,
                                                                currentlyPlaying.shuffle_state,
                                                                currentlyPlaying.repeat_state,
                                                                currentlyPlaying.progress_ms);
                                                    }
                                                });
                                    } else if (currentlyPlaying.context.uri.contains(":album:")) {
                                        getPAlbumTrackNumber(context, currentlyPlaying.context.uri,
                                                currentlyPlaying.item.uri, 50, 0,
                                                new Callback<Integer>() {
                                                    @Override
                                                    public void onSuccess(Integer position) {
                                                        playTransfer(null,
                                                                currentlyPlaying.context.uri,
                                                                position,
                                                                currentlyPlaying.shuffle_state,
                                                                currentlyPlaying.repeat_state,
                                                                currentlyPlaying.progress_ms);
                                                    }
                                                });
                                    }
                                }
                            };
                            switch (controller) {
                                case NATIVE_CONTROLLER:
                                    mCurrentController = mNativeController;
                                    break;
                                case CONNECT_CONTROLLER:
                                    mCurrentController = mConnectController;
                                    setDevice(context, deviceId, transferCallback);
                                    return;
                                case BLUETOOTH_CONTROLLER:
                                    break;
                            }
                            transferCallback.onSuccess(null);
                        }
                    });
                }
            });
        }
    }

    private static void playTransfer(final String uris, final String contextUri, final int position,
                                     boolean shuffleState, final String repeatState, final int positionMs) {
        //Workaround: https://github.com/spotify/web-api/issues/565
        if (contextUri == null && mCurrentController == mNativeController) {
            mCurrentController.resume(new Callback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    updateDevice();
                }
            });
            return;
        }
        mCurrentController.shuffle(shuffleState, new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mCurrentController.repeat(repeatState, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mCurrentController.play(uris, contextUri, position, new Callback<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                mCurrentController.seek(positionMs, new Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        updateDevice();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    public static Runnable onPlayback(Context context, Callback<CurrentlyPlaying> callback) {
        if (mConnectRunnable == null) {
            getController(context);
            mConnectRunnable = mConnectController.onPlayback(new Callback<CurrentlyPlaying>() {
                @Override
                public void onSuccess(final CurrentlyPlaying currentlyConnectPlaying) {
                    if (currentlyConnectPlaying.device.is_active
                            && currentlyConnectPlaying.is_playing
                            && mConnectController != mCurrentController
                            && mTransferring < System.currentTimeMillis()) {
                        mCurrentController = mConnectController;
                        updateDevice();
                    }
                }
            });
        }
        return getController(context).onPlayback(callback);
    }

    public static void offPlayback(Context context, Runnable runnable) {
        if (mConnectRunnable != null) {
            mConnectController.offPlayback(mConnectRunnable);
            mConnectRunnable = null;
        }
        getController(context).offPlayback(runnable);
    }

    public static Runnable onDevice(Callback<Void> callback) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

            }
        };
        if (mDeviceCallbacks == null) {
            mDeviceCallbacks = new HashMap<>();
        }
        mDeviceCallbacks.put(runnable.hashCode(), callback);
        return runnable;
    }

    public static void offDevice(Runnable runnable) {
        if (mDeviceCallbacks != null) {
            mDeviceCallbacks.remove(runnable.hashCode());
        }
    }

    private static void updateDevice() {
        if (mDeviceCallbacks != null) {
            for (Callback<Void> callback : mDeviceCallbacks.values()) {
                callback.onSuccess(null);
            }
        }
    }

    private static void setDevice(Context context, final String deviceId, final Callback<Void> callback) {
        if (deviceId == null) {
            if (callback != null) {
                callback.onSuccess(null);
            }
            return;
        }
        getService(context, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Transfer transfer = new Transfer();
                transfer.device_ids = new String[1];
                transfer.device_ids[0] = deviceId;
                Call<Void> call = service.transfer(transfer);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            }
        });
    }

    private static void getPlaylistTrackNumber(final Context context, final String contextUri,
                                               final String trackUri, final int limit,
                                               final int offset, final Callback<Integer> callback) {
        getService(context, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Paging<PlaylistTrack>> call = service.getPlaylistTracks(
                        contextUri.split(":")[2], contextUri.split(":")[4],
                        "items(track.uri),total,offset", limit, offset, "from_token");
                call.enqueue(new retrofit2.Callback<Paging<PlaylistTrack>>() {
                    @Override
                    public void onResponse(Call<Paging<PlaylistTrack>> call,
                                           retrofit2.Response<Paging<PlaylistTrack>> response) {
                        if (response.isSuccessful()) {
                            Paging<PlaylistTrack> playlistTracks = response.body();
                            int x = 0;
                            for (PlaylistTrack playlistTrack : playlistTracks.items) {
                                if (playlistTrack.track.uri.equals(trackUri)) {
                                    callback.onSuccess(playlistTracks.offset + x);
                                    return;
                                }
                                x++;
                            }
                            if (playlistTracks.total > playlistTracks.offset + 50) {
                                getPlaylistTrackNumber(context, contextUri, trackUri, limit,
                                        playlistTracks.offset + limit, callback);
                                return;
                            }
                            callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<PlaylistTrack>> call, Throwable t) {

                    }
                });
            }
        });
    }

    private static void getPAlbumTrackNumber(final Context context, final String contextUri,
                                             final String trackUri, final int limit,
                                             final int offset, final Callback<Integer> callback) {
        getService(context, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Paging<Track>> call = service.getAlbumTracks(contextUri.split(":")[2], limit,
                        offset, "from_token");
                call.enqueue(new retrofit2.Callback<Paging<Track>>() {
                    @Override
                    public void onResponse(Call<Paging<Track>> call,
                                           retrofit2.Response<Paging<Track>> response) {
                        if (response.isSuccessful()) {
                            Paging<Track> albumTracks = response.body();
                            int x = 0;
                            for (Track track : albumTracks.items) {
                                if (track.uri.equals(trackUri)) {
                                    callback.onSuccess(albumTracks.offset + x);
                                    return;
                                }
                                x++;
                            }
                            if (albumTracks.total > albumTracks.offset + 50) {
                                getPlaylistTrackNumber(context, contextUri, trackUri, limit,
                                        albumTracks.offset + limit, callback);
                                return;
                            }
                            callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<Track>> call, Throwable t) {

                    }
                });
            }
        });
    }
}
