package com.seapip.thomas.wearify.wearify

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface Service2 {
    @GET("token")
    fun getToken(): Observable<Model.Token>

    @GET("token/{token}")
    fun getToken(@Path("token") token: String): Observable<Model.Token>

    @GET("token/{token}/{key}")
    fun getToken(
            @Path("token") token: String,
            @Path("key") key: String
    ): Observable<Model.Token>
}
