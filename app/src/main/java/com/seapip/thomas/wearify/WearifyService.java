package com.seapip.thomas.wearify;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface WearifyService {
    @GET("token")
    Call<Token> getToken();
    @GET("token/{token}")
    Call<Token> getToken(@Path("token") String token);
    @GET("token/{token}/{key}")
    Call<Token> getToken(@Path("token") String token, @Path("key") String key);
}
