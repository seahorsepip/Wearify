package com.seapip.thomas.wearify;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import com.seapip.thomas.wearify.Browse.Activity;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Paging;
import com.seapip.thomas.wearify.Spotify.Playlist;
import com.seapip.thomas.wearify.Spotify.SavedTrack;
import com.seapip.thomas.wearify.Spotify.Service;
import com.seapip.thomas.wearify.Spotify.Util;
import com.squareup.picasso.Picasso;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.Spotify.Util.largestImageUrl;

public class NowPlayingActivity extends Activity {

    private String mUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer), null);

        WearableActionDrawer actionDrawer = (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);
        Menu menu = actionDrawer.getMenu();
        menu.add("Shuffle").setIcon(getDrawable(R.drawable.ic_shuffle_black_24dp));
        menu.add("Repeat").setIcon(getDrawable(R.drawable.ic_repeat_black_24dp));
        menu.add("Devices").setIcon(getDrawable(R.drawable.ic_devices_other_black_24dp));


        final ImageView backgroundImage = (ImageView) findViewById(R.id.background_image);
        final TextView title = (TextView) findViewById(R.id.title);
        final TextView subTitle = (TextView) findViewById(R.id.sub_title);

        mUri = getIntent().getStringExtra("uri");

        CircularProgressView progressView = (CircularProgressView) findViewById(R.id.circle_progress);
        progressView.setProgress(40);
        progressView.invalidate();

        ImageView playButton = (ImageView) findViewById(R.id.button_play);
        LayerDrawable layerDrawable = (LayerDrawable) getDrawable(R.drawable.nested_icon).mutate();
        GradientDrawable background = (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.nested_background);
        background.setTint(Color.parseColor("#00ffe0"));
        Drawable pauseIcon = getDrawable(R.drawable.ic_pause_black_24dp);
        pauseIcon.setTint(Color.argb(180, 0, 0, 0));
        layerDrawable.setDrawableByLayerId(R.id.nested_icon, pauseIcon);
        playButton.setImageDrawable(layerDrawable);
        Manager.getService(new Callback() {
            @Override
            public void onSuccess(Service service) {
                Call<Playlist> call = service.getPlaylist("spotify", "37i9dQZF1DXd28jAsVoMbV",
                        "images,tracks.items(track.artists(name),track.name,track.uri,track.album.images)",
                        "from_token");
                call.enqueue(new retrofit2.Callback<Playlist>() {
                    @Override
                    public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                        if(response.isSuccessful()) {
                            Playlist playlist = response.body();
                            title.setText(playlist.tracks.items[47].track.name);
                            subTitle.setText(Util.names(playlist.tracks.items[47].track.artists));
                            Picasso.with(getApplicationContext())
                                    .load(largestImageUrl(playlist.tracks.items[47].track.album.images))
                                    .fit().into(backgroundImage);
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
