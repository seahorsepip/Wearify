package com.seapip.thomas.wearify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableRecyclerView;
import android.widget.ProgressBar;

import com.seapip.thomas.wearify.browse.Adapter;
import com.seapip.thomas.wearify.browse.Header;
import com.seapip.thomas.wearify.browse.Item;
import com.seapip.thomas.wearify.browse.OnClick;
import com.seapip.thomas.wearify.spotify.Callback;
import com.seapip.thomas.wearify.spotify.controller.Controller;
import com.seapip.thomas.wearify.spotify.Manager;
import com.seapip.thomas.wearify.spotify.objects.Device;
import com.seapip.thomas.wearify.spotify.objects.Devices;
import com.seapip.thomas.wearify.spotify.Service;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

import static android.view.View.GONE;
import static com.seapip.thomas.wearify.spotify.controller.Service.CONNECT_CONTROLLER;
import static com.seapip.thomas.wearify.spotify.controller.Service.NATIVE_CONTROLLER;

public class DeviceActivity extends WearableActivity {

    private boolean mIsBound;
    private com.seapip.thomas.wearify.spotify.controller.Service mController;
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
            mController = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            mIsBound = true;
            mController = ((com.seapip.thomas.wearify.spotify.controller.Service.ControllerBinder) service).getServiceInstance();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        final String uris = getIntent().getStringExtra("uris");
        final String contextUri = getIntent().getStringExtra("contextUri");
        final int position = getIntent().getIntExtra("position", -1);
        final boolean shuffleState = getIntent().getBooleanExtra("shuffleState", false);
        final String repeatState = getIntent().getStringExtra("repeatState");
        final int positionMs = getIntent().getIntExtra("positionMs", 0);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#00ffe0"), PorterDuff.Mode.SRC_ATOP);
        final WearableRecyclerView recyclerView = (WearableRecyclerView) findViewById(R.id.content);
        final ArrayList<Item> items = new ArrayList<>();
        items.add(new Header("Devices"));
        Adapter adapter = new Adapter(this, items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        Manager.getService(this, new Callback<Service>() {
            @Override
            public void onSuccess(com.seapip.thomas.wearify.spotify.Service service) {
                Call<Devices> call = service.devices();
                call.enqueue(new retrofit2.Callback<Devices>() {
                    @Override
                    public void onResponse(Call<Devices> call, Response<Devices> response) {
                        if (response.isSuccessful()) {
                            Devices devices = response.body();
                            progressBar.setVisibility(GONE);
                            Item watch = new Item();
                            watch.title = Build.MODEL;
                            watch.subTitle = "Watch";
                            watch.image = getDrawable(R.drawable.ic_watch_black_24dp);
                            watch.onClick = new OnClick() {
                                @Override
                                public void run(Context context) {
                                    mController.setController(NATIVE_CONTROLLER, null,
                                            new Callback<Controller>() {
                                                @Override
                                                public void onSuccess(Controller controller) {
                                                    if (uris != null || contextUri != null) {
                                                        controller.play(uris, contextUri,
                                                                position, shuffleState,
                                                                repeatState, positionMs);
                                                    }
                                                }
                                            });
                                    finish();
                                }
                            };
                            items.add(watch);
                            for (final Device device : devices.devices) {
                                if (!device.is_restricted) {
                                    Item item = new Item();
                                    item.title = device.name;
                                    switch (device.type) {
                                        case "Smartphone":
                                            item.subTitle = "Phone";
                                            item.image = getDrawable(R.drawable.ic_smartphone_black_24dp);
                                            break;
                                        case "Tablet":
                                            item.subTitle = "Tablet";
                                            item.image = getDrawable(R.drawable.ic_tablet_black_24dp);
                                            break;
                                        default:
                                        case "Computer":
                                            item.subTitle = "Computer";
                                            item.image = getDrawable(R.drawable.ic_computer_black_24dp);
                                            break;
                                    }
                                    item.onClick = new OnClick() {
                                        @Override
                                        public void run(Context context) {
                                            mController.setController(CONNECT_CONTROLLER, device.id,
                                                    new Callback<Controller>() {
                                                        @Override
                                                        public void onSuccess(Controller controller) {
                                                            if (uris != null || contextUri != null) {
                                                                controller.play(uris, contextUri,
                                                                        position, shuffleState,
                                                                        repeatState, positionMs);
                                                            }
                                                        }
                                                    });
                                            finish();
                                        }
                                    };
                                    items.add(item);
                                }
                            }
                        }
                        recyclerView.getAdapter().notifyDataSetChanged();
                    }


                    @Override
                    public void onFailure(Call<Devices> call, Throwable t) {

                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent mIntent = new Intent(this, com.seapip.thomas.wearify.spotify.controller.Service.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsBound) {
            unbindService(mConnection);
        }
    }
}
