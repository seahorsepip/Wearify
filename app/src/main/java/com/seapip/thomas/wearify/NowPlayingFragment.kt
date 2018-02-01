package com.seapip.thomas.wearify

import android.app.Fragment
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.wear.ambient.AmbientMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.now_playing_view.*

class NowPlayingFragment : Fragment(), AmbientMode.AmbientCallbackProvider {

    private val mAmbientCallback = object : AmbientMode.AmbientCallback() {
        override fun onExitAmbient() {
            //Background
            album_art_background_image.visibility = View.VISIBLE

            //Progressbar
            progress_bar.visibility = View.VISIBLE

            //Play button
            button_play.apply {
                background = context.getDrawable(R.drawable.round_primary_button)
                setImageDrawable(context.getDrawable(R.drawable.ic_pause_black_24dp))
                imageTintList = ColorStateList.valueOf(context.getColor(R.color.primary_icon))
            }
        }

        override fun onEnterAmbient(ambientDetails: Bundle?) {
            val burnIn = ambientDetails!!.getBoolean(AmbientMode.EXTRA_BURN_IN_PROTECTION)
            val lowBit = ambientDetails.getBoolean(AmbientMode.EXTRA_LOWBIT_AMBIENT)

            //Background
            if (burnIn || lowBit) album_art_background_image.visibility = View.INVISIBLE

            //Progressbar
            progress_bar.visibility = View.INVISIBLE

            //Play button
            button_play.apply {
                if (burnIn || lowBit) {
                    background = context.getDrawable(R.drawable.round_primary_ambient_button)
                    imageTintList = ColorStateList.valueOf(Color.WHITE)
                }
                if (burnIn) {
                    setImageDrawable(context.getDrawable(R.drawable.ic_pause_black_burn_in_24dp))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater!!.inflate(R.layout.now_playing_view, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_play.setOnClickListener {
            Toast.makeText(context, "Button clicked!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getAmbientCallback(): AmbientMode.AmbientCallback {
        return mAmbientCallback
    }
}