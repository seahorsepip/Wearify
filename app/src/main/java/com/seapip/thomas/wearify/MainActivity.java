package com.seapip.thomas.wearify;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;

import com.seapip.thomas.wearify.Spotify.Artist;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.CursorPaging;
import com.seapip.thomas.wearify.Spotify.Image;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.PlayHistory;
import com.seapip.thomas.wearify.Spotify.Playlist;
import com.seapip.thomas.wearify.Spotify.Service;
import com.seapip.thomas.wearify.Spotify.User;

import java.util.ArrayList;
import java.util.Collections;

import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends WearableActivity {
    private WearableNavigationDrawer mWearableNavigationDrawer;
    private WearableDrawerLayout mWearableDrawerLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Main Wearable Drawer Layout that wraps all content
        mWearableDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mWearableDrawerLayout.peekDrawer(Gravity.BOTTOM);
        mWearableDrawerLayout.peekDrawer(Gravity.TOP);

        // Top Navigation Drawer
        mWearableNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer);
        mWearableNavigationDrawer.setAdapter(new NavigationDrawerAdapter(this));

        // Peeks Navigation drawer on the top.
        mWearableDrawerLayout.peekDrawer(Gravity.TOP);

        Manager.getService(new Callback() {
            @Override
            public void onSuccess(Service service) {
                Call<CursorPaging<PlayHistory>> call = service.getRecentPlayed(50);
                call.enqueue(new retrofit2.Callback<CursorPaging<PlayHistory>>() {
                    @Override
                    public void onResponse(Call<CursorPaging<PlayHistory>> call, Response<CursorPaging<PlayHistory>> response) {
                        if (response.isSuccessful()) {
                            CursorPaging<PlayHistory> playHistory = response.body();
                            final WearableRecyclerView recyclerView = (WearableRecyclerView) findViewById(R.id.content);
                            final ArrayList<BrowseItem> items = new ArrayList<>();
                            for (final PlayHistory item : playHistory.items) {
                                if (item.context != null && !containsUri(items, item.context.uri)) {
                                    final BrowseItem browseItem = new BrowseItem();
                                    browseItem.uri = item.context.uri;
                                    browseItem.played_at = item.played_at;
                                    switch (item.context.type) {
                                        case "playlist":
                                            Manager.getService(new Callback() {
                                                @Override
                                                public void onSuccess(Service service) {
                                                    Call<Playlist> call = service.getPlaylist(
                                                            item.context.uri.split(":")[2],
                                                            item.context.uri.split(":")[4]);
                                                    call.enqueue(new retrofit2.Callback<Playlist>() {
                                                        @Override
                                                        public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                                                            if (response.isSuccessful()) {
                                                                final Playlist playlist = response.body();
                                                                browseItem.title = playlist.name;
                                                                browseItem.subTitle = "Playlist";
                                                                browseItem.image = smallestImageUrl(playlist.images);
                                                                final BrowseItemAdapter adapter = (BrowseItemAdapter) recyclerView.getAdapter();
                                                                adapter.notifyDataSetChanged();
                                                                Manager.getService(new Callback() {
                                                                    @Override
                                                                    public void onSuccess(Service service) {
                                                                        Call<User> call = service.getUser(playlist.owner.id);
                                                                        call.enqueue(new retrofit2.Callback<User>() {
                                                                            @Override
                                                                            public void onResponse(Call<User> call, Response<User> response) {
                                                                                if (response.isSuccessful()) {
                                                                                    User user = response.body();
                                                                                    browseItem.subTitle += " • " + "by ";
                                                                                    if (user.display_name != null) {
                                                                                        browseItem.subTitle += user.display_name;
                                                                                    } else {
                                                                                        browseItem.subTitle += user.id;
                                                                                    }
                                                                                    adapter.notifyDataSetChanged();
                                                                                }
                                                                            }

                                                                            @Override
                                                                            public void onFailure(Call<User> call, Throwable t) {

                                                                            }
                                                                        });
                                                                    }
                                                                });
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
                                            browseItem.title = item.track.album.name;
                                            browseItem.subTitle = "Album";
                                            if (item.track.artists.length > 0) {
                                                ArrayList<String> names = new ArrayList<>();
                                                for (Artist artist : item.track.artists) {
                                                    names.add(artist.name);
                                                }
                                                Collections.sort(names);
                                                browseItem.subTitle += " • by " + TextUtils.join(", ", names);
                                            }
                                            browseItem.image = smallestImageUrl(item.track.album.images);
                                            break;
                                    }
                                    items.add(browseItem);
                                }
                            }
                            Collections.sort(items, new BrowseItemComparator());
                            BrowseItemAdapter adapter = new BrowseItemAdapter(MainActivity.this, items);
                            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                            recyclerView.setAdapter(adapter);
                            Log.d("WEARIFY", playHistory.items[0].track.name);
                            Log.d("WEARIFY", playHistory.items[1].track.name);
                            Log.d("WEARIFY", playHistory.items[2].track.name);
                        }
                    }

                    @Override
                    public void onFailure(Call<CursorPaging<PlayHistory>> call, Throwable t) {
                        Log.e("WEARIFY", t.toString());
                    }
                });
            }
        });
    }

    private boolean containsUri(ArrayList<BrowseItem> items, String uri) {
        for (BrowseItem item : items) {
            if (item.uri.equals(uri)) {
                return true;
            }
        }
        return false;
    }

    private String smallestImageUrl(Image[] images) {
        int size = 0;
        for (Image image : images) {
            if (image.width < size || size == 0) {
                size = image.width;
            }
        }
        for (Image image : images) {
            if (image.width == size) {
                return image.url;
            }
        }
        return null;
    }
}
