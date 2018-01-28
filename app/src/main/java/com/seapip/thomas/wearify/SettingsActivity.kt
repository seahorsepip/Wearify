package com.seapip.thomas.wearify

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.wearable.activity.WearableActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.seapip.thomas.wearify.spotify.Service
import com.seapip.thomas.wearify.spotify.Util
import com.seapip.thomas.wearify.spotify.objects.User
import com.seapip.thomas.wearify.spotify.webapi.Manager
import com.seapip.thomas.wearify.spotify.webapi.WebAPI
import kotlinx.android.synthetic.main.activity_settings.*
import retrofit2.Call
import retrofit2.Response


class SettingsActivity : WearableActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        button_sign_out.setOnClickListener {
            //Clear token
            com.seapip.thomas.wearify.wearify.Manager.removeToken(this)

            //Stop service
            stopService(Intent(this, Service::class.java))

            //Restart application
            startActivity(Intent(this, LaunchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_TASK_ON_HOME
            })
        }

        getUser {
            if (it.images.isNotEmpty()) {
                Glide.with(applicationContext)
                        .load(Util.smallestImageUrl(it.images))
                        .asBitmap()
                        .fitCenter()
                        .into(object : BitmapImageViewTarget(account_avatar) {
                            override fun setResource(resource: Bitmap) {
                                super.setResource(resource)
                                RoundedBitmapDrawableFactory.create(resources, resource).apply {
                                    isCircular = true
                                    account_avatar.setImageDrawable(this)
                                }
                            }
                        })
            }
            account_id.text = it.id
            account_product.text = "Spotify ${if (it.product == "premium") "Premium" else "Free"}"
        }
    }

    private fun getUser(callback: (User) -> Unit) {
        Manager().getWebAPI(this, object : Callback<WebAPI> {
            override fun onSuccess(webAPI: WebAPI?) {
                webAPI!!.me().enqueue(object : retrofit2.Callback<User> {
                    override fun onFailure(call: Call<User>?, t: Throwable?) {

                    }

                    override fun onResponse(call: Call<User>?, response: Response<User>?) {
                        if (response!!.isSuccessful) callback(response.body()!!)
                    }

                })
            }

            override fun onError() {

            }
        })
    }
}
