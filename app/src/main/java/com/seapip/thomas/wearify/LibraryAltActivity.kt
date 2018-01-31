package com.seapip.thomas.wearify

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.support.wear.ambient.AmbientMode
import android.support.wear.ambient.AmbientMode.FRAGMENT_TAG
import android.support.wear.widget.drawer.WearableDrawerLayout
import android.support.wear.widget.drawer.WearableDrawerView
import android.view.View
import com.seapip.thomas.wearify.R.color.primary_icon
import com.seapip.thomas.wearify.R.drawable.*
import com.seapip.thomas.wearify.R.layout.activity_library
import kotlinx.android.synthetic.main.activity_library.*

class LibraryAltActivity : Activity(), AmbientMode.AmbientCallbackProvider {

    private var mAmbientController: AmbientMode.AmbientController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_library)

        drawer_layout.setDrawerStateCallback(object : WearableDrawerLayout.DrawerStateCallback() {
            override fun onDrawerClosed(layout: WearableDrawerLayout?, drawerView: WearableDrawerView?) {
                super.onDrawerClosed(layout, drawerView)
                //Not being called on close ugh.....
            }

            override fun onDrawerOpened(layout: WearableDrawerLayout?, drawerView: WearableDrawerView?) {
                super.onDrawerOpened(layout, drawerView)
                if (drawerView == now_playing_drawer && mAmbientController == null) {
                    mAmbientController = AmbientMode.attachAmbientSupport(this@LibraryAltActivity)
                }
            }
        })

        val drawablePlaying = getDrawable(ic_logo_waves_animated) as AnimatedVectorDrawable
        icon_drawer_peek.setImageDrawable(drawablePlaying)
        drawablePlaying.start()

        button_play.setOnClickListener {
            if (mAmbientController != null) {
                val ambientFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
                fragmentManager
                        .beginTransaction()
                        .remove(ambientFragment)
                        .commit()
            }
        }

        now_playing_drawer.controller.peekDrawer()
    }

    override fun getAmbientCallback(): AmbientMode.AmbientCallback {
        return object : AmbientMode.AmbientCallback() {
            override fun onExitAmbient() {
                super.onExitAmbient()
                //Background
                album_art_background_image.visibility = View.VISIBLE

                //Progressbar
                progress_bar.visibility = View.VISIBLE

                //Play button
                button_play.apply {
                    background = getDrawable(round_primary_button)
                    setImageDrawable(getDrawable(ic_pause_black_24dp))
                    imageTintList = ColorStateList.valueOf(getColor(primary_icon))
                }
            }

            override fun onEnterAmbient(ambientDetails: Bundle?) {
                super.onEnterAmbient(ambientDetails)
                //Background
                album_art_background_image.visibility = View.INVISIBLE

                //Progressbar
                progress_bar.visibility = View.INVISIBLE

                //Play button
                button_play.apply {
                    background = getDrawable(round_primary_ambient_button)
                    setImageDrawable(getDrawable(ic_pause_black_burn_in_24dp))
                    imageTintList = ColorStateList.valueOf(Color.WHITE)
                }

                //Show now playing view in ambient mode
                //if (mAmbientController != null) now_playing_drawer.controller.openDrawer()
            }
        }
    }
}