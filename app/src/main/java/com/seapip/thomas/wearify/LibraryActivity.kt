package com.seapip.thomas.wearify

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.seapip.thomas.wearify.R.color.primary
import com.seapip.thomas.wearify.R.drawable.*
import com.seapip.thomas.wearify.browse.*
import kotlinx.android.synthetic.main.activity_browse.*

class LibraryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        setDrawers(drawer_layout, top_navigation_drawer, bottom_action_drawer, 0)

        val loading = Loading(getColor(primary))
        val retry = ActionButtonSmall(getDrawable(ic_repeat_black_24dp), "Failed loading, retry?")


        val items = arrayListOf<Item>(
                ActionButton(getDrawable(ic_search_black_24dp)),
                Category("Playlists", getDrawable(ic_playlist_black_24px), OnClick {
                    it.startActivity(Intent(it, PlaylistsActivity::class.java))
                }),
                Category("Songs", getDrawable(R.drawable.ic_song_black_24dp), OnClick {
                    it.startActivity(Intent(it, TracksActivity::class.java))
                }),
                Category("Albums", getDrawable(R.drawable.ic_album_black_24dp), OnClick {
                    it.startActivity(Intent(it, AlbumsActivity::class.java))
                }),
                Category("Artists", getDrawable(R.drawable.ic_artist_black_24dp), OnClick {
                    it.startActivity(Intent(it, ArtistsActivity::class.java))
                }),
                Category("New UI", getDrawable(R.drawable.ic_arrow_forward_black_24dp), OnClick {
                    it.startActivity(Intent(it, MainActivity::class.java))
                }),
                Header("Recently Played"),
                loading
        )

        val adapter = Adapter(this, items)

        content.layoutManager = LinearLayoutManager(this)
        content.adapter = adapter

        LibraryManager(items, adapter).getRecentPlayed(this, 50, object : Callback<Void> {
            override fun onSuccess(aVoid: Void?) {
                items.remove(loading)
                adapter.notifyDataSetChanged()
            }

            override fun onError() {
                items.remove(loading)
                items.add(retry)
                adapter.notifyDataSetChanged()
            }
        })
    }
}
