package com.seapip.thomas.wearify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;

import com.seapip.thomas.wearify.browse.ActionButton;
import com.seapip.thomas.wearify.browse.ActionButtonSmall;
import com.seapip.thomas.wearify.browse.Activity;
import com.seapip.thomas.wearify.browse.Adapter;
import com.seapip.thomas.wearify.browse.Category;
import com.seapip.thomas.wearify.browse.Header;
import com.seapip.thomas.wearify.browse.Item;
import com.seapip.thomas.wearify.browse.Loading;
import com.seapip.thomas.wearify.browse.OnClick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer), 1);


        final List<Item> items = new ArrayList<>();

        final Adapter adapter = new Adapter(this, items);

        RecyclerView recyclerView = (WearableRecyclerView) findViewById(R.id.content);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ActionButton search = new ActionButton();
        search.icon = getDrawable(R.drawable.ic_search_black_24dp);
        search.iconColor = Color.argb(200, 0, 0, 0);
        search.backgroundColor = Color.parseColor("#00ffe0");

        final Loading loading = new Loading(Color.parseColor("#00ffe0"));

        final ActionButtonSmall retry = new ActionButtonSmall();
        retry.icon = getDrawable(R.drawable.ic_repeat_black_24dp);
        retry.iconColor = Color.argb(200, 0, 0, 0);
        retry.backgroundColor = Color.parseColor("#00ffe0");
        retry.text = "Failed loading, retry?";

        items.addAll(Arrays.asList(
                search,
                new Category("Playlists", getDrawable(R.drawable.ic_playlist_black_24px),
                        new OnClick() {
                            @Override
                            public void run(Context context) {
                                context.startActivity(new Intent(context, PlaylistsActivity.class));
                            }
                        }),
                new Category("Songs", getDrawable(R.drawable.ic_song_black_24dp),
                        new OnClick() {
                            @Override
                            public void run(Context context) {
                                context.startActivity(new Intent(context, TracksActivity.class));
                            }
                        }),
                new Category("Albums", getDrawable(R.drawable.ic_album_black_24dp),
                        new OnClick() {
                            @Override
                            public void run(Context context) {
                                context.startActivity(new Intent(context, AlbumsActivity.class));
                            }
                        }),
                new Category("Artists", getDrawable(R.drawable.ic_artist_black_24dp),
                        new OnClick() {
                            @Override
                            public void run(Context context) {
                                context.startActivity(new Intent(context, ArtistsActivity.class));
                            }
                        }),
                new Header("Recently Played"),
                loading));
        new LibraryManager(items, adapter).getRecentPlayed(this, 50, new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                items.remove(loading);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError() {
                items.remove(loading);
                items.add(retry);
                adapter.notifyDataSetChanged();
            }
        });
    }
}
