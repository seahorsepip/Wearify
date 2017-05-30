package com.seapip.thomas.wearify.Spotify;

import android.os.Handler;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.Spotify.Manager.getService;

public class ConnectController implements Controller {

    private String mDeviceId;
    private Handler mPlaybackHandler;
    private Callback<CurrentlyPlaying> mOnPlaybackCallback;
    private CurrentlyPlaying mCurrentlyPlaying;

    public ConnectController() {
        mPlaybackHandler = new Handler();
    }

    @Override
    public void play(final String uris, final String contextUri,
                     final int position, final Callback<Void> callback) {
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Play play = new Play();
                play.uris = uris;
                play.context_uri = contextUri;
                if (position > -1) {
                    play.offset = new Offset();
                    play.offset.position = position;
                }
                Call<Void> call = service.play(mDeviceId, play);
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

    @Override
    public void pause(final Callback<Void> callback) {
        Manager.cancelAll();
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.pause(mDeviceId);
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

    @Override
    public void resume(final Callback<Void> callback) {
        Manager.cancelAll();
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.resume(mDeviceId);
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

    @Override
    public void shuffle(final boolean state, final Callback<Void> callback) {
        Manager.cancelAll();
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.shuffle(state, mDeviceId);
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

    @Override
    public void repeat(final String state, final Callback<Void> callback) {
        Manager.cancelAll();
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.repeat(state, mDeviceId);
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

    @Override
    public void getPlayback(final Callback<CurrentlyPlaying> callback) {
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<CurrentlyPlaying> call = service.playback("from_token");
                call.enqueue(new retrofit2.Callback<CurrentlyPlaying>() {
                    @Override
                    public void onResponse(Call<CurrentlyPlaying> call, Response<CurrentlyPlaying> response) {
                        if (response.isSuccessful()) {
                            CurrentlyPlaying currentlyPlaying = response.body();
                            if (callback != null && currentlyPlaying != null) {
                                mCurrentlyPlaying = currentlyPlaying;
                                callback.onSuccess(currentlyPlaying);
                            } else {
                                callback.onError();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CurrentlyPlaying> call, Throwable t) {
                    }
                });
            }
        });
    }

    @Override
    public Runnable onPlayback(final Callback<CurrentlyPlaying> callback) {
        mPlaybackHandler.removeCallbacksAndMessages(null);
        if(mCurrentlyPlaying != null) {
            callback.onSuccess(mCurrentlyPlaying);
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                getPlayback(callback);
                mPlaybackHandler.postDelayed(this, 3000);
            }
        };
        runnable.run();
        return runnable;
    }

    @Override
    public void offPlayback(Runnable runnable) {
        mPlaybackHandler.removeCallbacks(runnable);
    }

    @Override
    public void prev(final Callback<Void> callback) {
        Manager.cancelAll();
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.prev(mDeviceId);
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

    @Override
    public void next(final Callback<Void> callback) {
        Manager.cancelAll();
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.next(mDeviceId);
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

    public void volume(final int volume, final Callback<Void> callback) {
        Manager.cancelAll();
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.volume(volume, mDeviceId);
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
}
