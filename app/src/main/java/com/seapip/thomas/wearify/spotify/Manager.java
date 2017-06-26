package com.seapip.thomas.wearify.spotify;

import android.content.Context;

import com.seapip.thomas.wearify.wearify.Token;

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
    static private Token mToken;
    static private Retrofit.Builder mBuilder;
    static private Dispatcher mDispatcher;
    static private Service mService;

    public static void getService(Context context, final Callback<Service> callback) {
        com.seapip.thomas.wearify.wearify.Manager.getToken(context, new com.seapip.thomas.wearify.wearify.Callback() {
            @Override
            public void onSuccess(final Token token) {
                try {
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
                } catch (Exception e) {
                    callback.onError();
                }
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }

    public static void cancelAll() {
        //mDispatcher.cancelAll();
    }
}
