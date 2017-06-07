package com.seapip.thomas.wearify;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;

import com.seapip.thomas.wearify.Browse.ActionButtonSmall;
import com.seapip.thomas.wearify.Browse.Activity;
import com.seapip.thomas.wearify.Browse.Adapter;
import com.seapip.thomas.wearify.Browse.Header;
import com.seapip.thomas.wearify.Browse.Item;
import com.seapip.thomas.wearify.Browse.Loading;
import com.seapip.thomas.wearify.Browse.OnClick;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.Manager;
import com.seapip.thomas.wearify.Spotify.Objects.Paging;
import com.seapip.thomas.wearify.Spotify.Objects.SavedTrack;
import com.seapip.thomas.wearify.Spotify.Service;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

public class TracksActivity extends Activity {

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
        mItems.add(new Header("Songs"));
        ActionButtonSmall shuffle = new ActionButtonSmall();
        shuffle.icon = getDrawable(R.drawable.ic_shuffle_black_24dp);
        shuffle.iconColor = Color.argb(180, 0, 0, 0);
        shuffle.backgroundColor = Color.parseColor("#00ffe0");
        shuffle.text = "Shuffle Play";
        mItems.add(shuffle);
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
                            for (SavedTrack savedTrack : savedTracks.items) {
                                Item item = new Item();
                                item.setTrack(savedTrack.track);
                                item.onClick = new OnClick() {
                                    @Override
                                    public void run(Context context) {
                                        /*
                                        Manager.getController(context).shuffle(false, new Callback<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //Manager.play(null, contextUri, position, null);
                                            }
                                        });*/
                                    }
                                };
                                mItems.add(item);
                            }
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
}
