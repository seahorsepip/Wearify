package com.seapip.thomas.wearify.Browse;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.WearableRecyclerView;

import com.seapip.thomas.wearify.AlbumActivity;
import com.seapip.thomas.wearify.PlaylistActivity;
import com.seapip.thomas.wearify.Spotify.Album;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Playlist;
import com.seapip.thomas.wearify.Spotify.Service;
import com.seapip.thomas.wearify.Spotify.Track;
import com.seapip.thomas.wearify.Spotify.User;
import com.seapip.thomas.wearify.Spotify.Util;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.Spotify.Util.names;
import static com.seapip.thomas.wearify.Spotify.Util.smallestImageUrl;

public class Item {
    public Drawable image;
    public String imageUrl;
    public String title;
    public String subTitle;
    public String uri;
    public OnClick onClick;
    public int number;

    public void setPlaylist(final Playlist playlist, WearableRecyclerView recyclerView) {
        setPlaylist(playlist, recyclerView, false);
    }

    public void setPlaylist(final Playlist playlist, final WearableRecyclerView recyclerView, final boolean songCount) {
        title = playlist.name;
        subTitle = songCount ? Util.songCount(playlist.tracks.total) : "Playlist";
        imageUrl = smallestImageUrl(playlist.images);
        onClick = new OnClick() {
            @Override
            public void run(Context context) {
                Intent intent = new Intent(context, PlaylistActivity.class).putExtra("uri", playlist.uri);
                context.startActivity(intent);
            }
        };
        recyclerView.getAdapter().notifyDataSetChanged();
        Manager.getService(new Callback() {
            @Override
            public void onSuccess(Service service) {
                Call<User> call = service.getUser(playlist.owner.id);
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
                            if (songCount) {
                                subTitle = name + " • " + subTitle;
                            } else {
                                subTitle += " • " + name;
                            }
                            recyclerView.getAdapter().notifyDataSetChanged();
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
        setAlbum(album, false);
    }

    public void setAlbum(final Album album, boolean songCount) {
        title = album.name;
        subTitle = "Album";
        if (songCount) {
            subTitle = Util.songCount(album.tracks.total);
        } else if (album.type != null) {
            subTitle = album.type.substring(0, 1).toUpperCase() + album.type.substring(1);
        }
        if (album.artists != null && album.artists.length > 0) {
            if (songCount) {
                subTitle = names(album.artists) + " • " + subTitle;
            } else {
                subTitle += " • by " + names(album.artists);
            }
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
            subTitle = names(track.artists);
        }
    }
}
