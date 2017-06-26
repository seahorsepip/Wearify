package com.seapip.thomas.wearify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;

import com.seapip.thomas.wearify.browse.Activity;
import com.seapip.thomas.wearify.browse.Adapter;
import com.seapip.thomas.wearify.browse.Header;
import com.seapip.thomas.wearify.browse.Item;
import com.seapip.thomas.wearify.browse.Loading;
import com.seapip.thomas.wearify.browse.OnClick;
import com.seapip.thomas.wearify.spotify.Callback;
import com.seapip.thomas.wearify.spotify.Service;
import com.seapip.thomas.wearify.spotify.webapi.Manager;
import com.seapip.thomas.wearify.spotify.objects.Paging;
import com.seapip.thomas.wearify.spotify.objects.Playlist;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;
import com.seapip.thomas.wearify.spotify.objects.User;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.spotify.Service.getWebAPI;

public class PlaylistsActivity extends Activity {

    private WearableRecyclerView mRecyclerView;
    private ArrayList<Item> mItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer), 1);

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.content);
        mItems = new ArrayList<>();
        mItems.add(new Header("Playlists"));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter(this, mItems));
        getPlaylists(50, 0);
    }

    private void getPlaylists(final int limit, final int offset) {
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        getWebAPI(this, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Paging<Playlist>> call = webAPI.getPlaylists(limit, offset);
                call.enqueue(new retrofit2.Callback<Paging<Playlist>>() {
                    @Override
                    public void onResponse(Call<Paging<Playlist>> call, Response<Paging<Playlist>> response) {
                        if (response.isSuccessful()) {
                            mItems.remove(loading);
                            Paging<Playlist> playlists = response.body();
                            for (final Playlist playlist : playlists.items) {
                                final Item item = new Item();
                                item.setPlaylist(playlist, mRecyclerView, true);
                                if (item.imageUrl == null) {
                                    item.image = getDrawable(R.drawable.ic_playlist_black_24px);
                                }
                                item.onClick = new OnClick() {
                                    @Override
                                    public void run(Context context) {
                                        Intent intent = new Intent(context, PlaylistActivity.class).putExtra("uri", playlist.uri);
                                        context.startActivity(intent);
                                    }
                                };
                                mItems.add(item);
                                getWebAPI(PlaylistsActivity.this, new Callback<WebAPI>() {
                                    @Override
                                    public void onSuccess(WebAPI webAPI) {
                                        Call<User> call = webAPI.getUser(playlist.owner.id);
                                        call.enqueue(new retrofit2.Callback<User>() {
                                            @Override
                                            public void onResponse(Call<User> call, Response<User> response) {
                                                if (response.isSuccessful()) {
                                                    User user = response.body();
                                                    String name = "by ";
                                                    if (user.display_name != null) {
                                                        name += user.display_name;
                                                    } else {
                                                        name += user.id;
                                                    }
                                                    item.subTitle = name + " • " + item.subTitle;
                                                    mRecyclerView.getAdapter().notifyDataSetChanged();
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<User> call, Throwable t) {

                                            }
                                        });
                                    }
                                });
                            }
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                            if (playlists.total > playlists.offset + limit) {
                                getPlaylists(limit, playlists.offset + limit);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<Playlist>> call, Throwable t) {

                    }
                });
            }
        });
    }
}
