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
import android.support.wearable.view.ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.Wearable.NodeApi
import com.google.android.wearable.intent.RemoteIntent
import com.google.android.wearable.intent.RemoteIntent.startRemoteActivity
import com.seapip.thomas.wearify.R.drawable.ic_qr_code_black_24px
import com.seapip.thomas.wearify.R.drawable.ic_smartphone_black_24dp
import com.seapip.thomas.wearify.R.layout.activity_login
import com.seapip.thomas.wearify.wearify.Manager
import com.seapip.thomas.wearify.wearify.Token
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginAltActivity : WearableActivity(), ConnectionCallbacks, OnConnectionFailedListener {

    private val mResultReceiver = object : ResultReceiver(Handler()) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            when (resultCode) {
                RemoteIntent.RESULT_OK -> ConfirmationOverlay()
                        .setType(OPEN_ON_PHONE_ANIMATION)
                        .showOn(this@LoginAltActivity)
                else -> ConfirmationOverlay()
                        .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                        .setFinishedAnimationListener {
                            onFailure()
                        }
                        .showOn(this@LoginAltActivity)
            }
        }
    }

    private val mGoogleApiClient by lazy {
        GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
    }

    private val mLoginStateHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_login)

        @Suppress("DEPRECATION")
        if (getPhoneDeviceType(applicationContext) == DEVICE_TYPE_ERROR_UNKNOWN) onFailure()
        else NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback { result ->
            if (result.status.isSuccess && result.nodes.size > 0) {
                result.nodes.forEach { node ->
                    if (node.isNearby) {
                        onSuccess(node)
                        return@setResultCallback
                    }
                }
                onFailure()
            } else onFailure()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mGoogleApiClient.isConnected) mGoogleApiClient.disconnect()
    }

    override fun onResume() {
        super.onResume()
        mGoogleApiClient.connect()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        onFailure()
    }

    private fun onSuccess(node: Node) {
        info_login.text = "Continue on your ${node.displayName} to login."
        button_login.setImageDrawable(getDrawable(ic_smartphone_black_24dp))
        button_login_text.text = "Open on phone"

        var uri = Uri.EMPTY

        var checkLoginState: ((String, String) -> Runnable)? = null
        checkLoginState = { token, key ->
            Runnable {
                getToken(token, key, {
                    mLoginStateHandler.removeCallbacksAndMessages(null)
                    Manager.setToken(this@LoginAltActivity, it)
                    ConfirmationOverlay().setFinishedAnimationListener {
                        val intent = Intent(this@LoginAltActivity, LibraryActivity::class.java)
                        intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_TASK_ON_HOME
                        finishAffinity()
                        startActivity(intent)
                    }.showOn(this@LoginAltActivity)
                })
                mLoginStateHandler.postDelayed(checkLoginState?.invoke(token, key), 5000)
            }
        }

        button_login_wrapper.setOnClickListener {
            if (Uri.EMPTY == uri) getToken({
                with(it) {
                    uri = Uri.parse("https://wearify.seapip.com/login/$token/$key")
                    checkLoginState(token, key).run()
                }
            })
            startRemoteActivity(
                    applicationContext,
                    Intent(Intent.ACTION_VIEW).addCategory(Intent.CATEGORY_BROWSABLE).setData(uri),
                    mResultReceiver)
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

    private fun onFailure() {
        info_login.text = "Scan the QR code with another device to login."
        button_login.setImageDrawable(getDrawable(ic_qr_code_black_24px))
        button_login_text.text = "Show QR code"

        button_login_wrapper.setOnClickListener {
            startActivity(Intent(this@LoginAltActivity, QRActivity::class.java))
        }
    }

    override fun onConnected(bundle: Bundle?) {

    }

    override fun onConnectionSuspended(i: Int) {

    }

    override fun onStop() {
        super.onStop()
        mLoginStateHandler.removeCallbacksAndMessages(null)
    }
}
