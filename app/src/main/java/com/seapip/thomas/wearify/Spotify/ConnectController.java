package com.seapip.thomas.wearify.Spotify;

import retrofit2.Call;

import static com.seapip.thomas.wearify.Spotify.Manager.getService;

public class ConnectController implements Controller {

    @Override
    public void play(final String deviceId, final String uris, final String contextUri,
                     final int position, final Callback<Void> callback) {
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Play play = new Play();
                play.device_id = deviceId;
                play.uris = uris;
                play.context_uri = contextUri;
                if (position > -1) {
                    play.offset = new Offset();
                    play.offset.position = position;
                }
                Call<Void> call = service.play(play);
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
    public void shuffle(final String deviceId, final boolean state, final Callback<Void> callback) {
        getService(new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Void> call = service.shuffle(state, deviceId);
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
