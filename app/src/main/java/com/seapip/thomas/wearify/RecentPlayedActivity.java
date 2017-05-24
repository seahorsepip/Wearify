package com.seapip.thomas.wearify;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;

import com.seapip.thomas.wearify.Browse.ActionButton;
import com.seapip.thomas.wearify.Browse.Activity;
import com.seapip.thomas.wearify.Browse.Adapter;
import com.seapip.thomas.wearify.Browse.Category;
import com.seapip.thomas.wearify.Browse.Header;
import com.seapip.thomas.wearify.Browse.Item;
import com.seapip.thomas.wearify.Browse.Loading;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.CursorPaging;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.PlayHistory;
import com.seapip.thomas.wearify.Spotify.Playlist;
import com.seapip.thomas.wearify.Spotify.Service;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

public class RecentPlayedActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer));

        final WearableRecyclerView recyclerView = (WearableRecyclerView) findViewById(R.id.content);
        final ArrayList<Item> items = new ArrayList<>();
        ActionButton search = new ActionButton();
        search.icon = getDrawable(R.drawable.ic_search_black_24dp);
        search.iconColor = Color.argb(180, 0, 0, 0);
        search.backgroundColor = Color.parseColor("#00ffe0");
        items.add(search);
        items.add(new Category("Playlists", getDrawable(R.drawable.ic_playlist_black_24px)));
        items.add(new Category("Stations", getDrawable(R.drawable.ic_radio_black_24dp)));
        items.add(new Category("Songs", getDrawable(R.drawable.ic_song_black_24dp)));
        items.add(new Category("Albums", getDrawable(R.drawable.ic_album_black_24dp)));
        items.add(new Category("Artists", getDrawable(R.drawable.ic_artist_black_24dp)));
        items.add(new Category("Podcasts", getDrawable(R.drawable.ic_podcast_black_24dp)));
        items.add(new Header("Recently Played"));
        items.add(new Loading(Color.parseColor("#00ffe0")));
        Adapter adapter = new Adapter(RecentPlayedActivity.this, items);
        recyclerView.setLayoutManager(new LinearLayoutManager(RecentPlayedActivity.this));
        recyclerView.setAdapter(adapter);
        Manager.getService(new Callback() {
            @Override
            public void onSuccess(Service service) {
                Call<CursorPaging<PlayHistory>> call = service.getRecentPlayed(50);
                call.enqueue(new retrofit2.Callback<CursorPaging<PlayHistory>>() {
                    @Override
                    public void onResponse(Call<CursorPaging<PlayHistory>> call, Response<CursorPaging<PlayHistory>> response) {
                        if (response.isSuccessful()) {
                            items.remove(8);
                            recentPlayed(items, recyclerView, response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<CursorPaging<PlayHistory>> call, Throwable t) {

                    }
                });
            }
        });
    }

    private void recentPlayed(final ArrayList<Item> items,
                              final WearableRecyclerView recyclerView,
                              final CursorPaging<PlayHistory> playHistory) {
        for (final PlayHistory item : playHistory.items) {
            if (item.context != null && !containsUri(items, item.context.uri)) {
                final Item browseItem = new Item();
                browseItem.uri = item.context.uri;
                browseItem.played_at = item.played_at;
                switch (item.context.type) {
                    case "playlist":
                        Manager.getService(new Callback() {
                            @Override
                            public void onSuccess(Service service) {
                                Call<Playlist> call = service.getPlaylist(
                                        browseItem.uri.split(":")[2],
                                        browseItem.uri.split(":")[4],
                                        "name,images,uri,owner.id");
                                call.enqueue(new retrofit2.Callback<Playlist>() {
                                    @Override
                                    public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                                        if (response.isSuccessful()) {
                                            browseItem.setPlaylist(response.body(), recyclerView);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Playlist> call, Throwable t) {

                                    }
                                });
                            }
                        });
                        break;
                    case "album":
                        browseItem.setAlbum(item.track.album);
                        break;
                }
                items.add(browseItem);
                Adapter adapter = (Adapter) recyclerView.getAdapter();
                adapter.notifyDataSetChanged();
            }
        }
    }

    private boolean containsUri(ArrayList<Item> items, String uri) {
        for (Item item : items) {
            if (item.uri != null && item.uri.equals(uri)) {
                return true;
            }
        }
        return false;
    }
}
