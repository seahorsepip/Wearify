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
import android.widget.ImageView;

import com.seapip.thomas.wearify.Browse.ActionButtonSmall;
import com.seapip.thomas.wearify.Browse.Activity;
import com.seapip.thomas.wearify.Browse.Adapter;
import com.seapip.thomas.wearify.Browse.Header;
import com.seapip.thomas.wearify.Browse.Item;
import com.seapip.thomas.wearify.Browse.LetterGroupHeader;
import com.seapip.thomas.wearify.Browse.Loading;
import com.seapip.thomas.wearify.Browse.OnClick;
import com.seapip.thomas.wearify.Spotify.Artist;
import com.seapip.thomas.wearify.Spotify.Artists;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Paging;
import com.seapip.thomas.wearify.Spotify.SavedTrack;
import com.seapip.thomas.wearify.Spotify.Service;
import com.seapip.thomas.wearify.Spotify.Util;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.Spotify.Util.largestImageUrl;

public class ArtistActivity extends Activity {

    private WearableRecyclerView mRecyclerView;
    private ArrayList<Item> mItems;
    private String mUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_background);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer));

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.content);
        setGradientOverlay(mRecyclerView, (ImageView) findViewById(R.id.background_overlay));

        final ImageView backgroundImage = (ImageView) findViewById(R.id.background_image);
        mItems = new ArrayList<>();
        mItems.add(new Header("Artists"));
        ActionButtonSmall shuffle = new ActionButtonSmall();
        shuffle.icon = getDrawable(R.drawable.ic_shuffle_black_24dp);
        shuffle.iconColor = Color.argb(180, 0, 0, 0);
        shuffle.backgroundColor = Color.parseColor("#00ffe0");
        shuffle.text = "Shuffle Play";
        mItems.add(shuffle);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter(this, mItems));
        mUri = getIntent().getStringExtra("uri");
        Manager.getService(new Callback() {
            @Override
            public void onSuccess(Service service) {
                Call<Artists> call = service.getArtists(mUri.split(":")[2]);
                call.enqueue(new retrofit2.Callback<Artists>() {
                    @Override
                    public void onResponse(Call<Artists> call, Response<Artists> response) {
                        if (response.isSuccessful()) {
                            Artist artist = response.body().artists[0];
                            mItems.get(0).title = artist.name;
                            mItems.get(0).subTitle = "Artist";
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                            Picasso.with(getApplicationContext())
                                    .load(largestImageUrl(artist.images))
                                    .fit().into(backgroundImage);
                        }
                    }

                    @Override
                    public void onFailure(Call<Artists> call, Throwable t) {

                    }
                });
            }
        });
        getTracks(50, 0);
    }

    private void getTracks(final int limit, final int offset) {
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        Manager.getService(new Callback() {
            @Override
            public void onSuccess(Service service) {
                Call<Paging<SavedTrack>> call = service.getTracks(limit, offset, "from_token");
                call.enqueue(new retrofit2.Callback<Paging<SavedTrack>>() {
                    @Override
                    public void onResponse(Call<Paging<SavedTrack>> call, Response<Paging<SavedTrack>> response) {
                        if (response.isSuccessful()) {
                            mItems.remove(loading);
                            Paging<SavedTrack> savedTracks = response.body();
                            for (final SavedTrack savedTrack : savedTracks.items) {
                                if (savedTrack.track.artists != null && savedTrack.track.artists[0].uri.equals(mUri)) {
                                    if (!containsUri(savedTrack.track.album.uri)) {
                                        Item item = new Item();
                                        item.uri = savedTrack.track.album.uri;
                                        item.title = savedTrack.track.album.name;
                                        item.subTitle = savedTrack.track.album.artists[0].name;
                                        item.imageUrl = Util.smallestImageUrl(savedTrack.track.album.images);
                                        item.image = getDrawable(R.drawable.ic_album_black_24dp);
                                        item.onClick = new OnClick() {
                                            @Override
                                            public void run(Context context) {
                                                Intent intent = new Intent(context, AlbumActivity.class).putExtra("uri", savedTrack.track.album.uri);
                                                context.startActivity(intent);
                                            }
                                        };
                                        mItems.add(item);
                                    }
                                    Item item = new Item();
                                    item.setTrack(savedTrack.track);
                                    mItems.add(item);
                                }

                            }
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                            if (savedTracks.total > savedTracks.offset + limit) {
                                getTracks(limit, savedTracks.offset + limit);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<SavedTrack>> call, Throwable t) {

                    }
                });
            }
        });
    }

    private boolean containsUri(String uri) {
        for (Item item : mItems) {
            if (item.uri != null && item.uri.equals(uri)) {
                return true;
            }
        }
        return false;
    }
}
