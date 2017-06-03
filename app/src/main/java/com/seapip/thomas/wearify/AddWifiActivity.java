package com.seapip.thomas.wearify;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class AddWifiActivity extends WearableActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_network);

        LinearLayout buttonAddWifi = (LinearLayout) findViewById(R.id.button_add_wifi);
        buttonAddWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddWifiActivity.this.startActivity(
                        new Intent("com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS"));
            }
        });

    }
}
