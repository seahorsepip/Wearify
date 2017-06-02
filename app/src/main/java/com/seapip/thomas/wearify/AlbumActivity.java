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

import com.seapip.thomas.wearify.Browse.ActionButtonSmall;
import com.seapip.thomas.wearify.Browse.Activity;
import com.seapip.thomas.wearify.Browse.Adapter;
import com.seapip.thomas.wearify.Browse.Header;
import com.seapip.thomas.wearify.Browse.Item;
import com.seapip.thomas.wearify.Browse.Loading;
import com.seapip.thomas.wearify.Browse.OnClick;
import com.seapip.thomas.wearify.Spotify.Album;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Paging;
import com.seapip.thomas.wearify.Spotify.Service;
import com.seapip.thomas.wearify.Spotify.Track;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.Spotify.Util.largestImageUrl;
import static com.seapip.thomas.wearify.Spotify.Util.names;
import static com.seapip.thomas.wearify.Spotify.Util.songCount;

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
        shuffle.iconColor = Color.argb(180, 0, 0, 0);
        shuffle.backgroundColor = Color.parseColor("#00ffe0");
        shuffle.text = "Shuffle Play";
        shuffle.onClick = new OnClick() {
            @Override
            public void run(Context context) {
                Manager.shuffle(true, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Manager.play(null, mUri, -1, null);
                    }
                });
            }
        };
        mItems.add(shuffle);
        Adapter adapter = new Adapter(AlbumActivity.this, mItems);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(AlbumActivity.this));
        mRecyclerView.setAdapter(adapter);
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        Manager.getService(this, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Album> call = service.getAlbum(mUri.split(":")[2], "from_token");
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
                            Picasso.with(getApplicationContext())
                                    .load(largestImageUrl(album.images))
                                    .fit().into(backgroundImage);
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
        Manager.getService(this, new Callback<Service>() {
            @Override
            public void onSuccess(Service service) {
                Call<Paging<Track>> call = service.getAlbumTracks(mUri.split(":")[2], limit, offset,
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
                public void run(Context context) {
                    Manager.shuffle(false, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Manager.play(null, mUri, position, null);
                        }
                    });
                }
            };
            mItems.add(item);
        }
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }
}
