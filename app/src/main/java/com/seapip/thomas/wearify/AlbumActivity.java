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

import com.bumptech.glide.Glide;
import com.seapip.thomas.wearify.browse.ActionButtonSmall;
import com.seapip.thomas.wearify.browse.Activity;
import com.seapip.thomas.wearify.browse.Adapter;
import com.seapip.thomas.wearify.browse.Header;
import com.seapip.thomas.wearify.browse.Item;
import com.seapip.thomas.wearify.browse.Loading;
import com.seapip.thomas.wearify.browse.OnClick;
import com.seapip.thomas.wearify.spotify.Callback;
import com.seapip.thomas.wearify.spotify.objects.Album;
import com.seapip.thomas.wearify.spotify.objects.Paging;
import com.seapip.thomas.wearify.spotify.objects.Track;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.spotify.Service.getWebAPI;
import static com.seapip.thomas.wearify.spotify.Util.largestImageUrl;
import static com.seapip.thomas.wearify.spotify.Util.names;
import static com.seapip.thomas.wearify.spotify.Util.songCount;

public class AlbumActivity extends Activity {

    private WearableRecyclerView mRecyclerView;
    private ArrayList<Item> mItems;
    private String mUri;
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
        ActionButtonSmall shuffle = new ActionButtonSmall();
        shuffle.icon = getDrawable(R.drawable.ic_shuffle_black_24dp);
        shuffle.iconColor = Color.argb(200, 0, 0, 0);
        shuffle.backgroundColor = Color.parseColor("#00ffe0");
        shuffle.text = "Shuffle Play";
        shuffle.onClick = new OnClick() {
            @Override
            public void run(final Context context) {
                /*
                Manager.getController(context).shuffle(true, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Manager.getController(context).play(null, mUri, -1, null);
                    }
                });*/
            }
        };
        mItems.add(shuffle);
        Adapter adapter = new Adapter(AlbumActivity.this, mItems);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(AlbumActivity.this));
        mRecyclerView.setAdapter(adapter);
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        getWebAPI(this, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Album> call = webAPI.getAlbum(mUri.split(":")[2], "from_token");
                call.enqueue(new retrofit2.Callback<Album>() {
                    @Override
                    public void onResponse(Call<Album> call, Response<Album> response) {
                        if (response.isSuccessful()) {
                            Album album = response.body();
                            mItems.remove(loading);
                            mItems.get(0).title = album.name;
                            mItems.get(0).subTitle = songCount(album.tracks.total);
                            if (album.artists.length > 0) {
                                mItems.get(0).subTitle = names(album.artists) + " • " + mItems.get(0).subTitle;
                            }
                            addTracks(album.tracks.items);
                            Glide.with(getApplicationContext())
                                    .load(largestImageUrl(album.images))
                                    .fitCenter()
                                    .into(backgroundImage);
                            if (album.tracks.total > album.tracks.items.length) {
                                getTracks(50, album.tracks.items.length);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Album> call, Throwable t) {

                    }
                });
            }
        });
    }

    private void getTracks(final int limit, final int offset) {
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        getWebAPI(this, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Paging<Track>> call = webAPI.getAlbumTracks(mUri.split(":")[2], limit, offset,
                        "from_token");
                call.enqueue(new retrofit2.Callback<Paging<Track>>() {
                    @Override
                    public void onResponse(Call<Paging<Track>> call, Response<Paging<Track>> response) {
                        if (response.isSuccessful()) {
                            mItems.remove(loading);
                            Paging<Track> albumTracks = response.body();
                            addTracks(albumTracks.items);
                            if (albumTracks.total > albumTracks.offset + limit) {
                                getTracks(limit, albumTracks.offset + limit);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<Track>> call, Throwable t) {

                    }
                });
            }
        });
    }

    private void addTracks(Track[] tracks) {
        for (Track track : tracks) {
            Item item = new Item();
            item.setTrack(track);
            item.contextUri = mUri;
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
