package com.seapip.thomas.wearify.Browse;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.WearableRecyclerView;

import com.seapip.thomas.wearify.AlbumActivity;
import com.seapip.thomas.wearify.ArtistActivity;
import com.seapip.thomas.wearify.PlaylistActivity;
import com.seapip.thomas.wearify.Spotify.Album;
import com.seapip.thomas.wearify.Spotify.Artist;
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
import static com.seapip.thomas.wearify.Spotify.Util.songCount;

public class Item {
    public Drawable image;
    public String imageUrl;
    public String title;
    public String subTitle;
    public String uri;
    public String contextUri;
    public int position;
    public OnClick onClick;
    public int number;
    public boolean disabled;

    public void setPlaylist(final Playlist playlist, WearableRecyclerView recyclerView) {
        setPlaylist(playlist, recyclerView, false);
    }

    public void setPlaylist(final Playlist playlist, final WearableRecyclerView recyclerView, final boolean songCount) {
        uri = playlist.uri;
        title = playlist.name;
        subTitle = songCount ? Util.songCount(playlist.tracks.total) : "Playlist";
        imageUrl = smallestImageUrl(playlist.images);
        onClick = new OnClick() {
            @Override
            public void run(Context context) {
                Intent intent = new Intent(context, PlaylistActivity.class).putExtra("uri", uri);
                context.startActivity(intent);
            }
        };
        Manager.getService(new Callback<Service>() {
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
        uri = album.uri;
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
                Intent intent = new Intent(context, AlbumActivity.class).putExtra("uri", uri);
                context.startActivity(intent);
            }
        };
    }

    public void setTrack(Track track) {
        uri = track.uri;
        title = track.name;
        if (track.artists.length > 0) {
            subTitle = names(track.artists);
        }
        if (track.is_playable != null) {
            disabled = !track.is_playable;
        }
        onClick = new OnClick() {
            @Override
            public void run(Context context) {
                Manager.shuffle(false, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Manager.play(null, contextUri, position, null);
                    }
                });
            }
        };
    }

    public void setArtist(Artist artist, int songCount) {
        uri = artist.uri;
        title = artist.name;
        subTitle = songCount(songCount);
        imageUrl = smallestImageUrl(artist.images);
        onClick = new OnClick() {
            @Override
            public void run(Context context) {
                Intent intent = new Intent(context, ArtistActivity.class).putExtra("uri", uri);
                context.startActivity(intent);
            }
        };
    }
}
