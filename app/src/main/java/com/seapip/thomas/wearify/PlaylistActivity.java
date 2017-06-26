package com.seapip.thomas.wearify;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.widget.ImageView;

import com.seapip.thomas.wearify.browse.ActionButtonSmall;
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
import com.seapip.thomas.wearify.spotify.objects.PlaylistTrack;
import com.seapip.thomas.wearify.spotify.objects.User;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.spotify.Service.getWebAPI;
import static com.seapip.thomas.wearify.spotify.Util.largestImageUrl;
import static com.seapip.thomas.wearify.spotify.Util.songCount;

public class PlaylistActivity extends Activity {

    private String mUri;
    private WearableRecyclerView mRecyclerView;
    private ArrayList<Item> mItems;
    private int mPosition;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_background);

        mUri = getIntent().getStringExtra("uri");

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer), 1);

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.content);
        setGradientOverlay(mRecyclerView, (ImageView) findViewById(R.id.background_overlay));

        final ImageView backgroundImage = (ImageView) findViewById(R.id.background_image);
        mItems = new ArrayList<>();
        mItems.add(new Header(""));
        final ActionButtonSmall shuffle = new ActionButtonSmall();
        shuffle.icon = getDrawable(R.drawable.ic_shuffle_black_24dp);
        shuffle.iconColor = Color.argb(180, 0, 0, 0);
        shuffle.backgroundColor = Color.parseColor("#00ffe0");
        shuffle.text = "Shuffle Play";
        shuffle.onClick = new OnClick() {
            @Override
            public void run(final Context context) {
                getService().play(null, mUri, -1, true, "off", 0);
            }
        };
        mItems.add(shuffle);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter(this, mItems));
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        getWebAPI(this, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Playlist> call = webAPI.getPlaylist(mUri.split(":")[2], mUri.split(":")[4],
                        "name,images,uri,owner.id,tracks.items(track.artists(name),track.name,track.uri,track.is_playable),tracks.total,tracks.offset",
                        "from_token");
                call.enqueue(new retrofit2.Callback<Playlist>() {
                    @Override
                    public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                        if (response.isSuccessful()) {
                            final Playlist playlist = response.body();
                            mItems.remove(loading);
                            mItems.get(0).title = playlist.name;
                            mItems.get(0).subTitle = "Playlist";
                            getWebAPI(PlaylistActivity.this, new Callback<WebAPI>() {
                                @Override
                                public void onSuccess(WebAPI webAPI) {
                                    Call<User> call = webAPI.getUser(playlist.owner.id);
                                    call.enqueue(new retrofit2.Callback<User>() {
                                        @Override
                                        public void onResponse(Call<User> call, Response<User> response) {
                                            if (response.isSuccessful()) {
                                                User user = response.body();
                                                mItems.get(0).subTitle = "by ";
                                                if (user.display_name != null) {
                                                    mItems.get(0).subTitle += user.display_name;
                                                } else {
                                                    mItems.get(0).subTitle += user.id;
                                                }
                                                mItems.get(0).subTitle += " • " + songCount(playlist.tracks.total);
                                                mRecyclerView.getAdapter().notifyDataSetChanged();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<User> call, Throwable t) {

                                        }
                                    });
                                }
                            });
                            shuffle.subTitle = playlist.tracks.items[0].track.uri;
                            boolean charts = playlist.owner.id.equals("spotifycharts");
                            addTracks(playlist.tracks.items, 0, charts);
                            Picasso.with(getApplicationContext())
                                    .load(largestImageUrl(playlist.images))
                                    .fit().into(backgroundImage);
                            if (playlist.tracks.total > playlist.tracks.items.length) {
                                getTracks(100, playlist.tracks.items.length, charts);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Playlist> call, Throwable t) {

                    }
                });
            }
        });
    }

    private void getTracks(final int limit, final int offset, final boolean charts) {
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        getWebAPI(this, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Paging<PlaylistTrack>> call = webAPI.getPlaylistTracks(mUri.split(":")[2],
                        mUri.split(":")[4],
                        "items(track.artists(name),track.name,track.uri,track.is_playable),total,offset", limit,
                        offset, "from_token");
                call.enqueue(new retrofit2.Callback<Paging<PlaylistTrack>>() {
                    @Override
                    public void onResponse(Call<Paging<PlaylistTrack>> call, Response<Paging<PlaylistTrack>> response) {
                        if (response.isSuccessful()) {
                            mItems.remove(loading);
                            Paging<PlaylistTrack> playlistTracks = response.body();
                            addTracks(playlistTracks.items, offset, charts);
                            if (playlistTracks.total > playlistTracks.offset + limit) {
                                getTracks(limit, playlistTracks.offset + limit, charts);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<PlaylistTrack>> call, Throwable t) {

                    }
                });
            }
        });
    }

    private void addTracks(PlaylistTrack[] playlistTracks, int offset, boolean charts) {
        for (PlaylistTrack playlistTrack : playlistTracks) {
            Item item = new Item();
            item.setTrack(playlistTrack.track);
            item.contextUri = mUri;
            if (charts) {
                offset++;
                item.number = offset;
            }
            final int position = mPosition++;
            item.onClick = new OnClick() {
                @Override
                public void run(final Context context) {
                    getService().play(null, mUri, position, false, "off", 0);
                }
            };
            mItems.add(item);
        }
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }
}
