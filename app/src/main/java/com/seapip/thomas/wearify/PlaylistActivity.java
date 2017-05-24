package com.seapip.thomas.wearify;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.view.Display;
import android.widget.ImageView;

import com.seapip.thomas.wearify.Browse.ActionButton;
import com.seapip.thomas.wearify.Browse.Activity;
import com.seapip.thomas.wearify.Browse.Adapter;
import com.seapip.thomas.wearify.Browse.Header;
import com.seapip.thomas.wearify.Browse.Item;
import com.seapip.thomas.wearify.Browse.Loading;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.ImageUtil;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Playlist;
import com.seapip.thomas.wearify.Spotify.PlaylistTrack;
import com.seapip.thomas.wearify.Spotify.Service;
import com.seapip.thomas.wearify.Spotify.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

public class PlaylistActivity extends Activity {

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
        ActionButton shuffle = new ActionButton();
        shuffle.icon = getDrawable(R.drawable.ic_shuffle_black_24dp);
        shuffle.iconColor = Color.argb(180, 0, 0, 0);
        shuffle.backgroundColor = Color.parseColor("#00ffe0");
        shuffle.text = "Shuffle Play";
        items.add(shuffle);
        items.add(new Loading(Color.parseColor("#00ffe0")));
        final Adapter adapter = new Adapter(PlaylistActivity.this, items);
        recyclerView.setLayoutManager(new LinearLayoutManager(PlaylistActivity.this));
        recyclerView.setAdapter(adapter);
        final String uri = getIntent().getStringExtra("uri");
        Manager.getService(new Callback() {
            @Override
            public void onSuccess(Service service) {
                Call<Playlist> call = service.getPlaylist(
                        uri.split(":")[2],
                        uri.split(":")[4],
                        "name,images,uri,owner.id,tracks.items(track.artists(name),track.name,track.uri)");
                call.enqueue(new retrofit2.Callback<Playlist>() {
                    @Override
                    public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                        if (response.isSuccessful()) {
                            final Playlist playlist = response.body();
                            items.remove(2);
                            items.get(0).title = playlist.name;
                            items.get(0).subTitle = "Playlist";
                            Manager.getService(new Callback() {
                                @Override
                                public void onSuccess(Service service) {
                                    Call<User> call = service.getUser(playlist.owner.id);
                                    call.enqueue(new retrofit2.Callback<User>() {
                                        @Override
                                        public void onResponse(Call<User> call, Response<User> response) {
                                            if (response.isSuccessful()) {
                                                User user = response.body();
                                                items.get(0).subTitle += " • " + "by ";
                                                if (user.display_name != null) {
                                                    items.get(0).subTitle += user.display_name;
                                                } else {
                                                    items.get(0).subTitle += user.id;
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
                            int x = 0;
                            for (PlaylistTrack playlistTrack : playlist.tracks.items) {
                                Item item = new Item();
                                item.setTrack(playlistTrack.track);
                                x++;
                                if (playlist.owner.id.equals("spotifycharts")) {
                                    item.number = x;
                                }
                                items.add(item);
                            }
                            Picasso.with(getApplicationContext())
                                    .load(ImageUtil.largestImageUrl(playlist.images))
                                    .fit().into(backgroundImage);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(Call<Playlist> call, Throwable t) {

                    }
                });
            }
        });
    }
}
