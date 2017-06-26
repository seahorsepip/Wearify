package com.seapip.thomas.wearify.browse;

import android.graphics.drawable.Drawable;
import android.support.wearable.view.WearableRecyclerView;

import com.seapip.thomas.wearify.spotify.objects.Album;
import com.seapip.thomas.wearify.spotify.objects.Artist;
import com.seapip.thomas.wearify.spotify.objects.Playlist;
import com.seapip.thomas.wearify.spotify.objects.Track;
import com.seapip.thomas.wearify.spotify.Util;

import static com.seapip.thomas.wearify.spotify.Util.names;
import static com.seapip.thomas.wearify.spotify.Util.smallestImageUrl;
import static com.seapip.thomas.wearify.spotify.Util.songCount;

public class Item {
    public Drawable image;
    public String imageUrl;
    public String title;
    public String subTitle;
    public String uri;
    public String contextUri;
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
    }

    public void setAlbum(final Album album) {
        uri = album.uri;
        title = album.name;
        subTitle = album.type.substring(0, 1).toUpperCase() + album.type.substring(1);
        if (album.artists != null && album.artists.length > 0) {
            subTitle += " • by " + names(album.artists);
        }
        imageUrl = smallestImageUrl(album.images);
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
    }

    public void setArtist(Artist artist, int songCount) {
        uri = artist.uri;
        title = artist.name;
        subTitle = songCount(songCount);
        imageUrl = smallestImageUrl(artist.images);
    }
}
