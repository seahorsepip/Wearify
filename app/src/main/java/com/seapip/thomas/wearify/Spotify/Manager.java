package com.seapip.thomas.wearify.Spotify;

import com.seapip.thomas.wearify.Wearify.Token;

import java.io.IOException;

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
    static private Service mService;

    public static void getService(final Callback callback) {
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
}
