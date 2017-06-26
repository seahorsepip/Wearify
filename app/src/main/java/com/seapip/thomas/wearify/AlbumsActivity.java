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

import com.seapip.thomas.wearify.browse.Activity;
import com.seapip.thomas.wearify.browse.Adapter;
import com.seapip.thomas.wearify.browse.Header;
import com.seapip.thomas.wearify.browse.Item;
import com.seapip.thomas.wearify.browse.LetterGroupHeader;
import com.seapip.thomas.wearify.browse.Loading;
import com.seapip.thomas.wearify.browse.OnClick;
import com.seapip.thomas.wearify.spotify.Callback;
import com.seapip.thomas.wearify.spotify.Manager;
import com.seapip.thomas.wearify.spotify.objects.Paging;
import com.seapip.thomas.wearify.spotify.objects.SavedTrack;
import com.seapip.thomas.wearify.spotify.Service;
import com.seapip.thomas.wearify.spotify.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import retrofit2.Call;
import retrofit2.Response;

public class AlbumsActivity extends Activity {

    private WearableRecyclerView mRecyclerView;
    private ArrayList<Item> mItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        setDrawers((WearableDrawerLayout) findViewById(R.id.drawer_layout),
                (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer),
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer), 1);

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.content);
        mItems = new ArrayList<>();
        mItems.add(new Header("Albums"));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter(this, mItems));
        getTracks(50, 0);
    }

    private void getTracks(final int limit, final int offset) {
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        Manager.getService(this, new Callback<Service>() {
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
                                if (savedTrack.track.artists != null) {
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
                                }

                            }
                            groupByLetter();
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

    private void groupByLetter() {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i) instanceof LetterGroupHeader) {
                mItems.remove(i);
            }
        }
        Collections.sort(mItems.subList(1, mItems.size()), new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                return o1.subTitle.trim().replaceFirst("^(?i)The ", "")
                        .compareTo(o2.subTitle.trim().replaceFirst("^(?i)The ", ""));
            }
        });
        String letter = null;
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).uri != null) {
                String currentLetter = mItems.get(i).subTitle
                        .trim()
                        .toUpperCase()
                        .replaceFirst("THE ", "")
                        .replaceFirst("\\d", "#")
                        .substring(0, 1);
                if (!currentLetter.equals(letter)) {
                    mItems.add(i, new LetterGroupHeader(currentLetter));
                    letter = currentLetter;
                }
            }
        }
    }
}