package com.seapip.thomas.wearify.Spotify;

import android.content.*;

import com.seapip.thomas.wearify.Wearify.Token;

import java.io.IOException;

import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Manager {
    final static public int CONNECT_CONTROLLER = 0;
    final static public int BLUETOOTH_CONTROLLER = 1;
    final static public int NATIVE_CONTROLLER = 2;
    static private Token mToken;
    static private Retrofit.Builder mBuilder;
    static private Dispatcher mDispatcher;
    static private Service mService;
    static private Controller mController;

    public static void getService(final Callback<Service> callback) {
        com.seapip.thomas.wearify.Wearify.Manager.getToken(new com.seapip.thomas.wearify.Wearify.Callback() {
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

    public static void setController(int controller, android.content.Context context) {
        switch (controller) {
            case CONNECT_CONTROLLER:
                mController = new ConnectController();
                break;
            case BLUETOOTH_CONTROLLER:
                break;
            case NATIVE_CONTROLLER:
                mController = new NativeController(context);
                break;
        }
    }

    public static void play(String uris, String contextUri, int position, Callback<Void> callback) {
        mController.play(uris, contextUri, position, callback);
    }

    public static void pause(Callback<Void> callback) {
        mController.pause(callback);
    }

    public static void resume(Callback<Void> callback) {
        mController.resume(callback);
    }

    public static void shuffle(boolean state, Callback<Void> callback) {
        mController.shuffle(state, callback);
    }

    public static void repeat(String state, Callback<Void> callback) {
        mController.repeat(state, callback);
    }

    public static void getPlayback(Callback<CurrentlyPlaying> callback) {
        mController.getPlayback(callback);
    }

    public static Runnable onPlayback(Callback<CurrentlyPlaying> callback) {
        return mController.onPlayback(callback);
    }

    public static void offPlayback(Runnable runnable) {
        mController.offPlayback(runnable);
    }

    public static void prev(Callback<Void> callback) {
        mController.prev(callback);
    }

    public static void next(Callback<Void> callback) {
        mController.next(callback);
    }

    public static void volume(int volume, Callback<Void> callback) {
        mController.volume(volume, callback);
    }
}
