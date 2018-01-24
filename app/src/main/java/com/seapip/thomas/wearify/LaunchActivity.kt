package com.seapip.thomas.wearify

import android.animation.ObjectAnimator
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.seapip.thomas.wearify.spotify.Service
import com.seapip.thomas.wearify.wearify.Manager.getToken
import com.seapip.thomas.wearify.wearify.Token
import kotlinx.android.synthetic.main.activity_launch.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


class LaunchActivity : WearableActivity() {

    private var mDelay: AtomicLong = AtomicLong(0L)
    private val mHandler = Handler()
    private var mHasFocus = AtomicBoolean()
    private var mHasAnimationStarted = AtomicBoolean()
    private var mLoggedIn = AtomicBoolean()
    private var mCheckedLogin = AtomicBoolean()
    private val mFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or
            Intent.FLAG_ACTIVITY_TASK_ON_HOME;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        val isRunning = (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .getRunningServices(Integer.MAX_VALUE).any {
            Service::class.java.name == it.service.className
        }
        val now = System.currentTimeMillis()
        val intent = Intent(this@LaunchActivity,
                LibraryActivity::class.java).apply { flags = mFlags }

        checkLogin {
            mCheckedLogin.set(true)
            mDelay.set(if (isRunning) 0 else Math.max(1000 - System.currentTimeMillis() + now, 0))

            when {
                it -> mHandler.postDelayed({
                    finish()
                    startActivity(intent)
                }, mDelay.get())
                mHasFocus.get() -> welcomeScreen()
                else -> mLoggedIn.set(false)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        logo.y = logo.height / 5f
        container.visibility = View.GONE

        mHasFocus.set(hasFocus)

        welcomeScreen()
    }

    private fun welcomeScreen() {
        if (mHasFocus.get() && !mHasAnimationStarted.get() &&
                !mLoggedIn.get() && mCheckedLogin.get()) {

            val intent = Intent(this@LaunchActivity,
                    LoginAltActivity::class.java).apply { flags = mFlags }

            mHasAnimationStarted.set(true)

            mHandler.apply {
                postDelayed({
                    logo_animated.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    ObjectAnimator.ofFloat(logo_animated, "y",
                            logo.height / 5f).apply {
                        duration = 500
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                }, mDelay.get())
                postDelayed({
                    container.visibility = View.VISIBLE
                    welcome.setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    ObjectAnimator.ofFloat(welcome, "alpha", 1f).apply {
                        duration = 200
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                }, mDelay.get() + 300)
                postDelayed({
                    animation.visibility = View.GONE
                    logo.visibility = View.VISIBLE
                    logo_animated.setLayerType(View.LAYER_TYPE_NONE, null)
                    welcome.setLayerType(View.LAYER_TYPE_NONE, null)

                    next_welcome.setOnClickListener {
                        finish()
                        startActivity(intent)
                    }
                }, mDelay.get() + 500)
            }
        }
    }

    private fun checkLogin(callback: (Boolean) -> Unit) {
        getToken(this@LaunchActivity, object : com.seapip.thomas.wearify.wearify.Callback() {
            override fun onSuccess(token: Token) {
                callback(true)
            }

            override fun onError() {
                callback(false)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }


}
