package com.seapip.thomas.wearify.spotify.controller;

import android.content.Context;
import android.os.Handler;

import com.seapip.thomas.wearify.spotify.Callback;
import com.seapip.thomas.wearify.spotify.Service;
import com.seapip.thomas.wearify.spotify.objects.CurrentlyPlaying;
import com.seapip.thomas.wearify.spotify.objects.Device;
import com.seapip.thomas.wearify.spotify.objects.Offset;
import com.seapip.thomas.wearify.spotify.objects.Play;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.spotify.Service.CONNECT_CONTROLLER;
import static com.seapip.thomas.wearify.spotify.Service.cancelAllWebAPICalls;
import static com.seapip.thomas.wearify.spotify.Service.getWebAPI;

public class ConnectController implements Controller {

    private String mDeviceId;
    private Context mContext;
    private Service.Callbacks mCallbacks;
    private Handler mPlaybackHandler;
    private CurrentlyPlaying mCurrentlyPlaying;

    public ConnectController(Context context, Service.Callbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;
        mPlaybackHandler = new Handler();
        mCurrentlyPlaying = new CurrentlyPlaying();
        mCurrentlyPlaying.context = new com.seapip.thomas.wearify.spotify.objects.Context();
        mCurrentlyPlaying.device = new Device();
    }

    @Override
    public void play(final String[] uris, final String contextUri, final int position,
                     boolean shuffleState, final String repeatState, final int positionMs) {
        shuffle(shuffleState, new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                repeat(repeatState, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        getWebAPI(mContext, new Callback<WebAPI>() {
                            @Override
                            public void onSuccess(WebAPI webAPI) {
                                Play play = new Play();
                                play.uris = uris;
                                play.context_uri = contextUri;
                                if (position > -1) {
                                    play.offset = new Offset();
                                    play.offset.position = position;
                                }
                                Call<Void> call = webAPI.play(mDeviceId, play);
                                call.enqueue(new retrofit2.Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                                        if (response.isSuccessful()) {
                                            mCurrentlyPlaying.is_playing = true;
                                            mCallbacks.onPlaybackState(mCurrentlyPlaying, CONNECT_CONTROLLER);
                                            seek(positionMs);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {

                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void pause() {
        cancelAllWebAPICalls();
        getWebAPI(mContext, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Void> call = webAPI.pause(mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.is_playing = false;
                            mCallbacks.onPlaybackState(mCurrentlyPlaying, CONNECT_CONTROLLER);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            }
        });
    }

    @Override
    public void resume() {
        cancelAllWebAPICalls();
        getWebAPI(mContext, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Void> call = webAPI.resume(mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.is_playing = true;
                            mCallbacks.onPlaybackState(mCurrentlyPlaying, CONNECT_CONTROLLER);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            }
        });
    }

    @Override
    public void shuffle(boolean state) {
        shuffle(state, null);
    }

    private void shuffle(final boolean state, final Callback<Void> callback) {
        cancelAllWebAPICalls();
        getWebAPI(mContext, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Void> call = webAPI.shuffle(state, mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.shuffle_state = state;
                            mCallbacks.onPlaybackShuffle(mCurrentlyPlaying, CONNECT_CONTROLLER);
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

    @Override
    public void repeat(String state) {
        repeat(state, null);
    }

    private void repeat(final String state, final Callback<Void> callback) {
        cancelAllWebAPICalls();
        getWebAPI(mContext, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Void> call = webAPI.repeat(state, mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.repeat_state = state;
                            mCallbacks.onPlaybackRepeat(mCurrentlyPlaying, CONNECT_CONTROLLER);
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (callback != null) {
                            callback.onError();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void getPlayback(final Callback<CurrentlyPlaying> callback) {
        getWebAPI(mContext, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<CurrentlyPlaying> call = webAPI.playback("from_token");
                call.enqueue(new retrofit2.Callback<CurrentlyPlaying>() {
                    @Override
                    public void onResponse(Call<CurrentlyPlaying> call, Response<CurrentlyPlaying> response) {
                        if (response.isSuccessful()) {
                            callback.onSuccess(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<CurrentlyPlaying> call, Throwable t) {
                        callback.onError();
                    }
                });
            }
        });
    }

    @Override
    public void requestPlayback() {
        getPlayback(new Callback<CurrentlyPlaying>() {
            @Override
            public void onSuccess(CurrentlyPlaying currentlyPlaying) {
                if (currentlyPlaying != null) {
                    mCallbacks.onPlaybackState(currentlyPlaying, CONNECT_CONTROLLER);
                    mCallbacks.onPlaybackShuffle(currentlyPlaying, CONNECT_CONTROLLER);
                    mCallbacks.onPlaybackRepeat(currentlyPlaying, CONNECT_CONTROLLER);
                    mCallbacks.onPlaybackDevice(currentlyPlaying, CONNECT_CONTROLLER);
                    if (currentlyPlaying.item == null || mCurrentlyPlaying.item == null
                            || !currentlyPlaying.item.uri.equals(mCurrentlyPlaying.item.uri)) {
                        mCallbacks.onPlaybackMetaData(currentlyPlaying, CONNECT_CONTROLLER);
                    }
                    mCurrentlyPlaying = currentlyPlaying;
                }
            }
        });
    }

    @Override
    public void setInterval(final int interval) {
        mPlaybackHandler.removeCallbacksAndMessages(null);
        if (interval > 0) {
            new Runnable() {
                @Override
                public void run() {
                    requestPlayback();
                    mPlaybackHandler.postDelayed(this, interval);
                }
            }.run();
        }
    }

    @Override
    public void previous() {
        cancelAllWebAPICalls();
        getWebAPI(mContext, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Void> call = webAPI.prev(mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCallbacks.onPlaybackPrevious(mCurrentlyPlaying, CONNECT_CONTROLLER);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            }
        });
    }

    @Override
    public void next() {
        cancelAllWebAPICalls();
        getWebAPI(mContext, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Void> call = webAPI.next(mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCallbacks.onPlaybackNext(mCurrentlyPlaying, CONNECT_CONTROLLER);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            }
        });
    }

    @Override
    public void volume(final int volume) {
        cancelAllWebAPICalls();
        getWebAPI(mContext, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Void> call = webAPI.volume(volume, mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.device.volume_percent = volume;
                            mCallbacks.onPlaybackVolume(mCurrentlyPlaying, CONNECT_CONTROLLER);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            }
        });
    }

    @Override
    public void seek(int positionMs) {
        seek(positionMs, null);
    }

    private void seek(final int positionMs, final Callback<Void> callback) {
        getWebAPI(mContext, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Void> call = webAPI.seek(positionMs, mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.progress_ms = positionMs;
                            mCallbacks.onPlaybackSeek(mCurrentlyPlaying, CONNECT_CONTROLLER);
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

    @Override
    public void destroy() {
        mPlaybackHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void bind() {
        mCallbacks.onPlaybackBind(mCurrentlyPlaying, CONNECT_CONTROLLER);
    }
}
