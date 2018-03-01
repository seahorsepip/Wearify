package com.seapip.thomas.wearify

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.support.wear.ambient.AmbientMode
import android.support.wear.ambient.AmbientMode.*
import android.support.wear.widget.drawer.WearableDrawerLayout
import android.support.wear.widget.drawer.WearableDrawerLayout.DrawerStateCallback
import android.util.Log
import android.view.View
import android.view.View.OnApplyWindowInsetsListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowInsets
import com.seapip.thomas.wearify.R.drawable.ic_logo_waves_animated
import com.seapip.thomas.wearify.R.layout.main_view
import com.seapip.thomas.wearify.spotify.SpotifyApi
import com.seapip.thomas.wearify.wearify.WearifyApi
import kotlinx.android.synthetic.main.main_view.*
import org.koin.android.ext.android.inject
import java.lang.Math.pow
import java.lang.Math.sqrt


class MainActivity : Activity(), AmbientCallbackProvider, OnApplyWindowInsetsListener {

    private val mWearifyApi: WearifyApi by inject()
    private val mSpotifyApi: SpotifyApi by inject()
    private var mAmbientMode: AmbientMode? = null
    private var mAmbientAttached = false
    private val mAmbientCallback = object : AmbientCallback() {
        override fun onExitAmbient() {
            super.onExitAmbient()
            (drawer_content as NowPlayingFragment).ambientCallback.onExitAmbient()
        }

        override fun onEnterAmbient(ambientDetails: Bundle?) {
            super.onEnterAmbient(ambientDetails)
            (drawer_content as NowPlayingFragment).ambientCallback.onEnterAmbient(ambientDetails)
        }
    }
    private val mDrawerStateCallback = object : DrawerStateCallback() {
        override fun onDrawerStateChanged(layout: WearableDrawerLayout?, newState: Int) {
            super.onDrawerStateChanged(layout, newState)
            if (newState == 0) {
                now_playing_drawer.apply {
                    if (isClosed || isPeeking) removeAmbientSupport()
                    else if (isOpened) addAmbientSupport()
                }
            }
        }
    }
    private lateinit var mWindowInsets: WindowInsets;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(main_view)

        drawer_layout.setOnApplyWindowInsetsListener(this)
        drawer_layout.setDrawerStateCallback(mDrawerStateCallback)

        val drawablePlaying = getDrawable(ic_logo_waves_animated) as AnimatedVectorDrawable
        peek_view_icon.setImageDrawable(drawablePlaying)
        drawablePlaying.start()

        track_title.isSelected = true

        mWearifyApi.getToken(this, {
            Log.e("WEARIFY", it.toString())
        })

        //Adapt peek view for screens with a chin
        peek_view_content.waitForLayout {
            (mWindowInsets.systemWindowInsetBottom * 1.2).toInt().also {
                if (mWindowInsets.systemWindowInsetBottom > 0) setPadding(it, 0, it, 0)
            }
        }

        //Make peek view width fit
        peek_view.waitForLayout {
            val rect = Rect()
            getGlobalVisibleRect(rect)
            layoutParams.width = (2.0 * (rect.right / 2.0).let {
                sqrt(pow(it, 2.0) - pow((it - mWindowInsets.systemWindowInsetBottom - height), 2.0))
            }).toInt()
            requestLayout()
        }

        //Make drawer content ignore chin
        drawer_content.view.waitForLayout {
            layoutParams.height = width
        }

        Log.e("WEARIFY", mSpotifyApi.hello())
    }

    override fun onApplyWindowInsets(view: View?, insets: WindowInsets?): WindowInsets {
        mWindowInsets = insets!!
        view!!.onApplyWindowInsets(insets)
        return insets;
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) now_playing_drawer.controller.openDrawer()
    }

    private fun addAmbientSupport() {
        if (!mAmbientAttached) {
            mAmbientAttached = true

            fragmentManager.apply {
                mAmbientMode.also {
                    if (it == null) attachAmbientSupport(this@MainActivity)
                    else beginTransaction().add(it, FRAGMENT_TAG).commit()
                }
            }
        }
    }

    private fun removeAmbientSupport() {
        if (mAmbientAttached) {
            mAmbientAttached = false

            fragmentManager.apply {
                mAmbientMode.also {
                    if (it == null) mAmbientMode = findFragmentByTag(FRAGMENT_TAG) as AmbientMode?
                    if (mAmbientMode != null) beginTransaction().remove(mAmbientMode).commit()
                }
            }
        }
    }

    override fun getAmbientCallback(): AmbientCallback {
        return mAmbientCallback
    }

    private inline fun <T : View> T.waitForLayout(crossinline f: T.() -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (width > 0 && height > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f()
                }
            }
        })
    }
}