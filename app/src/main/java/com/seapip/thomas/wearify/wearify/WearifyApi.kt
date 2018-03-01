package com.seapip.thomas.wearify.wearify

import android.content.Context
import android.preference.PreferenceManager.getDefaultSharedPreferences
import android.util.Log
import com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES
import com.google.gson.GsonBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit


class WearifyApi {

    private val mService: Service2
    private var mToken: Model.Token? = null
    private var mDate: Calendar? = null

    init {
        val httpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .apply {
                    interceptors().add(Interceptor {
                        val request = it.request()
                        var response: Response
                        var attempts = 0
                        do {
                            response = it.proceed(request)
                            attempts++
                        } while (!response.isSuccessful && attempts < 10)
                        response
                    })
                }
        val gson = GsonBuilder().setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES).create()
        mService = Retrofit.Builder()
                .baseUrl("https://wearify.seapip.com/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()
                .create(Service2::class.java)
    }

    fun getToken(context: Context, success: (Model.Token) -> Unit, failure: () -> Unit = {}) {
        Log.e("WEARIFY", "UHM? " + now().before(mDate).toString())
        if (now().after(mDate)) return refresh(context, success, failure)
    }

    private fun refresh(context: Context, success: (Model.Token) -> Unit, failure: () -> Unit = {}) {
        val refresh = mToken?.refreshToken
                ?: getDefaultSharedPreferences(context).getString("refresh_token", null)
                ?: return failure()

        mService.getToken(refresh)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->
                            mToken = result.apply {
                                refreshToken = refresh
                                mDate = now().apply {
                                    add(Calendar.SECOND, expiresIn!!)
                                }
                                success(this)
                            }
                        },
                        { failure() }
                )
    }

    private fun now() = Calendar.getInstance()
}