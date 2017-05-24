package com.seapip.thomas.wearify;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.text.TextUtils;
import android.widget.ImageView;

import com.seapip.thomas.wearify.Browse.ActionButtonSmall;
import com.seapip.thomas.wearify.Browse.Activity;
import com.seapip.thomas.wearify.Browse.Adapter;
import com.seapip.thomas.wearify.Browse.Header;
import com.seapip.thomas.wearify.Browse.Item;
import com.seapip.thomas.wearify.Browse.Loading;
import com.seapip.thomas.wearify.Spotify.Album;
import com.seapip.thomas.wearify.Spotify.Artist;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.ImageUtil;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Service;
import com.seapip.thomas.wearify.Spotify.Track;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

public class AlbumActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_background);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer));

        final WearableRecyclerView recyclerView = (WearableRecyclerView) findViewById(R.id.content);
        setGradientOverlay(recyclerView, (ImageView) findViewById(R.id.background_overlay));

        final ImageView backgroundImage = (ImageView) findViewById(R.id.background_image);
        final ArrayList<Item> items = new ArrayList<>();
        items.add(new Header(""));
        ActionButtonSmall shuffle = new ActionButtonSmall();
        shuffle.icon = getDrawable(R.drawable.ic_shuffle_black_24dp);
        shuffle.iconColor = Color.argb(180, 0, 0, 0);
        shuffle.backgroundColor = Color.parseColor("#00ffe0");
        shuffle.text = "Shuffle Play";
        items.add(shuffle);
        items.add(new Loading(Color.parseColor("#00ffe0")));
        final Adapter adapter = new Adapter(AlbumActivity.this, items);
        recyclerView.setLayoutManager(new LinearLayoutManager(AlbumActivity.this));
        recyclerView.setAdapter(adapter);
        final String uri = getIntent().getStringExtra("uri");
        Manager.getService(new Callback() {
            @Override
            public void onSuccess(Service service) {
                Call<Album> call = service.getAlbum(uri.split(":")[2]);
                call.enqueue(new retrofit2.Callback<Album>() {
                    @Override
                    public void onResponse(Call<Album> call, Response<Album> response) {
                        if (response.isSuccessful()) {
                            Album album = response.body();
                            items.remove(2);
                            items.get(0).title = album.name;
                            items.get(0).subTitle = "Album";
                            if (album.artists.length > 0) {
                                ArrayList<String> names = new ArrayList<>();
                                for (Artist artist : album.artists) {
                                    names.add(artist.name);
                                }
                                items.get(0).subTitle += " • by " + TextUtils.join(", ", names);
                            }
                            for (Track track : album.tracks.items) {
                                Item item = new Item();
                                item.setTrack(track);
                                items.add(item);
                            }
                            Picasso.with(getApplicationContext())
                                    .load(ImageUtil.largestImageUrl(album.images))
                                    .fit().into(backgroundImage);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(Call<Album> call, Throwable t) {

                    }
                });
            }
        });
    }
}
