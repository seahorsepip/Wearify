package com.seapip.thomas.wearify

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.wearable.activity.WearableActivity
import android.support.wearable.phone.PhoneDeviceType.DEVICE_TYPE_ERROR_UNKNOWN
import android.support.wearable.phone.PhoneDeviceType.getPhoneDeviceType
import android.support.wearable.view.ConfirmationOverlay
import android.support.wearable.view.ConfirmationOverlay.FAILURE_ANIMATION
import android.support.wearable.view.ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION
import android.view.View.GONE
import android.widget.TextView
import com.google.android.wearable.intent.RemoteIntent
import com.google.android.wearable.intent.RemoteIntent.startRemoteActivity
import com.seapip.thomas.wearify.R.layout.activity_login
import com.seapip.thomas.wearify.wearify.Manager
import com.seapip.thomas.wearify.wearify.Token
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginAltActivity : WearableActivity() {

    private val mResultReceiver = object : ResultReceiver(Handler()) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            when (resultCode) {
                RemoteIntent.RESULT_OK -> ConfirmationOverlay()
                        .setType(OPEN_ON_PHONE_ANIMATION)
                        .showOn(this@LoginAltActivity)
                else -> {
                    info_login.text = "To sign in, ensure \nyour phone is on and \nnearby.";
                    (button_login_phone.getChildAt(1) as TextView).text = "Retry"
                    ConfirmationOverlay()
                            .setType(FAILURE_ANIMATION)
                            .showOn(this@LoginAltActivity)
                }
            }
        }
    }

    private val mLoginStateHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_login)

        var uri = Uri.EMPTY

        var checkLoginState: ((String, String) -> Runnable)? = null
        checkLoginState = { token, key ->
            Runnable {
                getToken(token, key, {
                    mLoginStateHandler.removeCallbacksAndMessages(null)
                    Manager.setToken(this@LoginAltActivity, it)
                    ConfirmationOverlay().setFinishedAnimationListener {
                        finishAffinity()
                        startActivity(
                                Intent(this@LoginAltActivity, LibraryActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                            Intent.FLAG_ACTIVITY_TASK_ON_HOME
                                }
                        )
                    }.showOn(this@LoginAltActivity)
                })
                mLoginStateHandler.postDelayed(checkLoginState?.invoke(token, key), 5000)
            }
        }

        val openOnPhone = {
            startRemoteActivity(
                    applicationContext,
                    Intent(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE).setData(uri),
                    mResultReceiver)
        }

        if (getPhoneDeviceType(applicationContext) == DEVICE_TYPE_ERROR_UNKNOWN) {
            button_login_phone.visibility = GONE
        }

        button_login_phone.setOnClickListener {
            if (Uri.EMPTY == uri) getToken({
                with(it) {
                    uri = Uri.parse("https://wearify.seapip.com/login/$token/$key")
                    checkLoginState(token, key).run()
                    openOnPhone()
                }
            }) else openOnPhone()
        }

        button_login_qr.setOnClickListener {
            startActivity(Intent(this@LoginAltActivity, QRActivity::class.java))
        }
    }

    private fun getToken(success: (Token) -> Unit, failure: () -> Unit = {}) {
        Manager.getService().token.enqueue(object : Callback<Token> {
            override fun onResponse(call: Call<Token>, response: Response<Token>) {
                if (response.isSuccessful) success(response.body()!!)
                else failure()
            }

            override fun onFailure(call: Call<Token>, t: Throwable) {
                failure();
            }
        })
    }

    private fun getToken(token: String, key: String, success: (Token) -> Unit, failure: () -> Unit = {}) {
        Manager.getService().getToken(token, key).enqueue(object : Callback<Token> {
            override fun onResponse(call: Call<Token>, response: Response<Token>) {
                if (response.isSuccessful) success(response.body()!!)
                else failure()
            }

            override fun onFailure(call: Call<Token>, t: Throwable) {
                failure()
            }
        })
    }

    override fun onStop() {
        super.onStop()
        mLoginStateHandler.removeCallbacksAndMessages(null)
    }
}
