package com.seapip.thomas.wearify;

import android.content.Context;
import android.content.Intent;

import com.seapip.thomas.wearify.browse.Adapter;
import com.seapip.thomas.wearify.browse.Item;
import com.seapip.thomas.wearify.browse.OnClick;
import com.seapip.thomas.wearify.spotify.objects.CursorPaging;
import com.seapip.thomas.wearify.spotify.objects.PlayHistory;
import com.seapip.thomas.wearify.spotify.objects.Playlist;
import com.seapip.thomas.wearify.spotify.objects.User;
import com.seapip.thomas.wearify.spotify.webapi.WebAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Response;

import static com.seapip.thomas.wearify.spotify.Service.getWebAPI;

public class LibraryManager {
    private List<Item> items;
    private Adapter adapter;

    public LibraryManager(List<Item> items, Adapter adapter) {
        this.items = items;
        this.adapter = adapter;
    }

    public void getRecentPlayed(final Context context, final int limit, final Callback<Void> callback) {
        getWebAPI(context, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<CursorPaging<PlayHistory>> call = webAPI.getRecentPlayed(limit);
                call.enqueue(new retrofit2.Callback<CursorPaging<PlayHistory>>() {
                    @Override
                    public void onResponse(Call<CursorPaging<PlayHistory>> call, Response<CursorPaging<PlayHistory>> response) {
                        if (response.isSuccessful()) {
                            final AtomicBoolean error = new AtomicBoolean(false);
                            final List<Item> addedItems = new ArrayList<>();
                            callback.onSuccess(null);
                            CursorPaging<PlayHistory> playHistories = response.body();
                            for (final PlayHistory playHistory : playHistories.items) {
                                if(error.get()) break;
                                if (playHistory.context != null && playHistory.context.type != null && !containsUri(addedItems, playHistory.context.uri)) {
                                    final Item item = new Item();
                                    item.uri = playHistory.context.uri;
                                    addedItems.add(item);
                                    Callback<Void> itemCallback = new Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                        }

                                        @Override
                                        public void onError() {
                                            if(!error.get()) {
                                                error.set(true);
                                                items.removeAll(addedItems);
                                                callback.onError();
                                            }
                                        }
                                    };
                                    switch (playHistory.context.type) {
                                        case "playlist":
                                            playlist(context, item, playHistory, itemCallback);
                                            break;
                                        case "album":
                                            album(context, item, playHistory, itemCallback);
                                            break;
                                    }
                                }
                            }
                            items.addAll(addedItems);
                            adapter.notifyDataSetChanged();
                        } else {
                            callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(Call<CursorPaging<PlayHistory>> call, Throwable t) {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }

    private boolean containsUri(List<Item> items, String uri) {
        for (Item item : items) {
            if (item.uri != null && item.uri.equals(uri)) {
                return true;
            }
        }
        return false;
    }

    private void playlist(final Context context, final Item item, final PlayHistory playHistory, final Callback<Void> callback) {
        getWebAPI(context, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<Playlist> call = webAPI.getPlaylist(
                        playHistory.context.uri.split(":")[2],
                        playHistory.context.uri.split(":")[4],
                        "name,images,uri,owner.id", "from_token");
                call.enqueue(new retrofit2.Callback<Playlist>() {
                    @Override
                    public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                        if (response.isSuccessful()) {
                            final Playlist playlist = response.body();
                            item.setPlaylist(playlist);
                            if (item.imageUrl == null) {
                                item.image = context.getDrawable(R.drawable.ic_playlist_black_24px);
                            }
                            item.onClick = new OnClick() {
                                @Override
                                public void run(Context context) {
                                    Intent intent = new Intent(context, PlaylistActivity.class).putExtra("uri", playlist.uri);
                                    context.startActivity(intent);
                                }
                            };
                            adapter.notifyDataSetChanged();
                            playlistUserName(context, item, playlist, callback);
                        } else {
                            callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(Call<Playlist> call, Throwable t) {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }

    private void playlistUserName(Context context, final Item item, final Playlist playlist, final Callback<Void> callback) {
        getWebAPI(context, new Callback<WebAPI>() {
            @Override
            public void onSuccess(WebAPI webAPI) {
                Call<User> call = webAPI.getUser(playlist.owner.id);
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
                            item.subTitle = name + " • " + item.subTitle;
                            adapter.notifyDataSetChanged();
                        } else {
                            callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        callback.onError();
                    }
                });
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });
    }

    private void album(Context context, Item item, final PlayHistory playHistory, final Callback<Void> callback) {
        item.setAlbum(playHistory.track.album);
        item.onClick = new OnClick() {
            @Override
            public void run(Context context) {
                Intent intent = new Intent(context, AlbumActivity.class).putExtra("uri", playHistory.track.album.uri);
                context.startActivity(intent);
            }
        };
    }
}
