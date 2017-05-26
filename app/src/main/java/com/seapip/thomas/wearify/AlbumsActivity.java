package com.seapip.thomas.wearify;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;

import com.seapip.thomas.wearify.Browse.Activity;
import com.seapip.thomas.wearify.Browse.Adapter;
import com.seapip.thomas.wearify.Browse.Header;
import com.seapip.thomas.wearify.Browse.Item;
import com.seapip.thomas.wearify.Browse.Loading;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Paging;
import com.seapip.thomas.wearify.Spotify.SavedAlbum;
import com.seapip.thomas.wearify.Spotify.Service;

import java.util.ArrayList;

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
                (WearableActionDrawer) findViewById(R.id.bottom_action_drawer));

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.content);
        mItems = new ArrayList<>();
        mItems.add(new Header("Albums"));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter(this, mItems));
        getAlbums(50, 0);
    }

    private void getAlbums(final int limit, final int offset) {
        final Loading loading = new Loading(Color.parseColor("#00ffe0"));
        mItems.add(loading);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        Manager.getService(new Callback() {
            @Override
            public void onSuccess(Service service) {
                Call<Paging<SavedAlbum>> call = service.getAlbums(limit, offset, "from_token");
                call.enqueue(new retrofit2.Callback<Paging<SavedAlbum>>() {
                    @Override
                    public void onResponse(Call<Paging<SavedAlbum>> call, Response<Paging<SavedAlbum>> response) {
                        if (response.isSuccessful()) {
                            mItems.remove(loading);
                            Paging<SavedAlbum> savedAlbums = response.body();
                            for (SavedAlbum savedAlbum : savedAlbums.items) {
                                Item item = new Item();
                                item.setAlbum(savedAlbum.album, true);
                                if (item.imageUrl == null) {
                                    item.image = getDrawable(R.drawable.ic_album_black_24dp);
                                }
                                mItems.add(item);
                            }
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                            if (savedAlbums.total > savedAlbums.offset + limit) {
                                getAlbums(limit, savedAlbums.offset + limit);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<SavedAlbum>> call, Throwable t) {

                    }
                });
            }
        });
    }
}
