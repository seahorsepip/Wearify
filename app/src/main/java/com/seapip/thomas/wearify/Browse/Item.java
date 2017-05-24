package com.seapip.thomas.wearify.Browse;

import android.app.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.WearableRecyclerView;
import android.text.TextUtils;

import com.seapip.thomas.wearify.AlbumActivity;
import com.seapip.thomas.wearify.PlaylistActivity;
import com.seapip.thomas.wearify.Spotify.Album;
import com.seapip.thomas.wearify.Spotify.Artist;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Playlist;
import com.seapip.thomas.wearify.Spotify.Service;
import com.seapip.thomas.wearify.Spotify.Track;
import com.seapip.thomas.wearify.Spotify.User;

import java.util.ArrayList;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.Spotify.ImageUtil.smallestImageUrl;

public class Item {
    public Drawable image;
    public String imageUrl;
    public String title;
    public String subTitle;
    public Date played_at;
    public String uri;
    public OnClick onClick;
    public int number;

    public void setPlaylist(final Playlist playlist, WearableRecyclerView recyclerView) {
        title = playlist.name;
        subTitle = "Playlist";
        imageUrl = smallestImageUrl(playlist.images);
        onClick = new OnClick() {
            @Override
            public void run(Context context) {
                Intent intent = new Intent(context, PlaylistActivity.class).putExtra("uri", playlist.uri);
                context.startActivity(intent);
            }
        };
        final Adapter adapter = (Adapter) recyclerView.getAdapter();
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
                            subTitle += " • " + "by ";
                            if (user.display_name != null) {
                                subTitle += user.display_name;
                            } else {
                                subTitle += user.id;
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

    public void setAlbum(final Album album) {
        title = album.name;
        subTitle = "Album";
        if (album.artists.length > 0) {
            ArrayList<String> names = new ArrayList<>();
            for (Artist artist : album.artists) {
                names.add(artist.name);
            }
            subTitle += " • by " + TextUtils.join(", ", names);
        }
        imageUrl = smallestImageUrl(album.images);
        onClick = new OnClick() {
            @Override
            public void run(Context context) {
                Intent intent = new Intent(context, AlbumActivity.class).putExtra("uri", album.uri);
                context.startActivity(intent);
            }
        };
    }

    public void setTrack(Track track) {
        title = track.name;
        if (track.artists.length > 0) {
            ArrayList<String> names = new ArrayList<>();
            for (Artist artist : track.artists) {
                names.add(artist.name);
            }
            //Collections.sort(names);
            subTitle = TextUtils.join(", ", names);
        }
    }
}
