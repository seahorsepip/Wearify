package com.seapip.thomas.wearify.spotify.webapi

import android.content.Context
import android.content.Intent

import com.seapip.thomas.wearify.AddWifiActivity
import com.seapip.thomas.wearify.Callback
import com.seapip.thomas.wearify.wearify.Token

import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Manager {
    private var mToken: Token? = null
    private var mBuilder: Retrofit.Builder? = null
    private var mDispatcher: Dispatcher? = null
    private var mWebAPI: WebAPI? = null

    fun getWebAPI(context: Context, callback: Callback<WebAPI>) {
        com.seapip.thomas.wearify.wearify.Manager.getToken(context, object : com.seapip.thomas.wearify.wearify.Callback() {
            override fun onSuccess(token: Token) {
                try {
                    if (token !== mToken) {
                        mToken = token
                        if (mBuilder == null) {
                            mBuilder = Retrofit.Builder()
                                    .baseUrl("https://api.spotify.com/v1/")
                                    .addConverterFactory(GsonConverterFactory.create())
                        }
                        val httpClient = OkHttpClient.Builder()
                        httpClient.addInterceptor { chain ->
                            val request = chain.request()
                            chain.proceed(request.newBuilder()
                                    .header("Authorization", "Bearer " + token.access_token)
                                    .method(request.method(), request.body())
                                    .build())
                        }
                        val loggingInterceptor = HttpLoggingInterceptor()
                        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                        httpClient.addInterceptor(loggingInterceptor)
                        mDispatcher = Dispatcher()
                        mDispatcher!!.maxRequests = 10
                        httpClient.dispatcher(mDispatcher!!)
                        mBuilder = mBuilder!!.client(httpClient.build())
                        mWebAPI = mBuilder!!.build().create(WebAPI::class.java)
                    }
                    callback.onSuccess(mWebAPI)
                } catch (e: Exception) {
                    //context.startActivity(Intent(context, AddWifiActivity::class.java))
                    callback.onError()
                }

            }

            override fun onError() {
                //context.startActivity(Intent(context, AddWifiActivity::class.java))
                callback.onError()
            }
        })
    }

    fun cancelAll() {
        mDispatcher!!.cancelAll()
    }
}
