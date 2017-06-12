package com.seapip.thomas.wearify.Spotify.Controller;

import android.content.Context;
import android.os.Handler;

import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Objects.CurrentlyPlaying;
import com.seapip.thomas.wearify.Spotify.Objects.Device;
import com.seapip.thomas.wearify.Spotify.Objects.Offset;
import com.seapip.thomas.wearify.Spotify.Objects.Play;
import com.seapip.thomas.wearify.Spotify.Service;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.Spotify.Manager.getService;

public class ConnectController implements Controller {

    private String mDeviceId;
    private Context mContext;
    private com.seapip.thomas.wearify.Spotify.Controller.Service mService;
    private Handler mPlaybackHandler;
    private CurrentlyPlaying mCurrentlyPlaying;

    public ConnectController(Context context) {
        mContext = context;
        mService = (com.seapip.thomas.wearify.Spotify.Controller.Service) context;
        mPlaybackHandler = new Handler();
        mCurrentlyPlaying = new CurrentlyPlaying();
        mCurrentlyPlaying.context = new com.seapip.thomas.wearify.Spotify.Objects.Context();
        mCurrentlyPlaying.device = new Device();
    }

    @Override
    public void play(final String uris, final String contextUri, final int position) {
        getService(mContext, new Callback<Service>() {
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
                            mCurrentlyPlaying.is_playing = true;
                            mService.onPlaybackState(mCurrentlyPlaying);
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
    public void pause() {
        Manager.cancelAll();
        getService(mContext, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.pause(mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.is_playing = false;
                            mService.onPlaybackState(mCurrentlyPlaying);
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
        Manager.cancelAll();
        getService(mContext, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.resume(mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.is_playing = true;
                            mService.onPlaybackState(mCurrentlyPlaying);
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
    public void shuffle(final boolean state) {
        Manager.cancelAll();
        getService(mContext, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.shuffle(state, mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.shuffle_state = state;
                            mService.onPlaybackShuffle(mCurrentlyPlaying);
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
    public void repeat(final String state) {
        Manager.cancelAll();
        getService(mContext, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.repeat(state, mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.repeat_state = state;
                            mService.onPlaybackRepeat(mCurrentlyPlaying);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {

                    }
                });
            }
        });
    }

    public void getPlayback() {
        getService(mContext, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<CurrentlyPlaying> call = service.playback("from_token");
                call.enqueue(new retrofit2.Callback<CurrentlyPlaying>() {
                    @Override
                    public void onResponse(Call<CurrentlyPlaying> call, Response<CurrentlyPlaying> response) {
                        if (response.isSuccessful()) {
                            CurrentlyPlaying currentlyPlaying = response.body();
                            if (currentlyPlaying != null) {
                                mService.onPlaybackState(currentlyPlaying);
                                mService.onPlaybackShuffle(currentlyPlaying);
                                mService.onPlaybackRepeat(currentlyPlaying);
                                mService.onPlaybackDevice(currentlyPlaying);
                                if (currentlyPlaying.item == null || mCurrentlyPlaying.item == null
                                        || !currentlyPlaying.item.uri.equals(mCurrentlyPlaying.item.uri)) {
                                    mService.onPlaybackMetaData(currentlyPlaying);
                                }
                                mCurrentlyPlaying = currentlyPlaying;
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

    public void setInterval(final int interval) {
        mPlaybackHandler.removeCallbacksAndMessages(null);
        if (interval > 0) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    getPlayback();
                    mPlaybackHandler.postDelayed(this, interval);
                }
            };
            runnable.run();
        }
    }

    @Override
    public void previous() {
        Manager.cancelAll();
        getService(mContext, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.prev(mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mService.onPlaybackPrevious(mCurrentlyPlaying);
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
        Manager.cancelAll();
        getService(mContext, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.next(mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mService.onPlaybackNext(mCurrentlyPlaying);
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
        Manager.cancelAll();
        getService(mContext, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.volume(volume, mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.device.volume_percent = volume;
                            mService.onPlaybackVolume(mCurrentlyPlaying);
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
    public void seek(final int positionMs) {
        getService(mContext, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.seek(positionMs, mDeviceId);
                call.enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            mCurrentlyPlaying.progress_ms = positionMs;
                            mService.onPlaybackSeek(mCurrentlyPlaying);
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
        mService.onPlaybackBind(mCurrentlyPlaying);
    }
}
