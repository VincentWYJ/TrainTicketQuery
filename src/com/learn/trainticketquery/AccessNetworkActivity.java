package com.learn.trainticketquery;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

public class AccessNetworkActivity extends Activity implements View.OnClickListener{

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Intent intent = null;
        switch (id) {
            case R.id.network_mobile_button:
                intent =  new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                break;
            case R.id.network_wifi_button:
                intent =  new Intent(Settings.ACTION_WIFI_SETTINGS);
                break;
            default:
                break;
        }
        if(intent != null) {
            startActivity(intent);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.showLog("AccessNetworkActivity created");

        setContentView(R.layout.activity_network);

        Utils.mIsBackFromSetNetwork = true;

        ((Button) findViewById(R.id.network_mobile_button)).setOnClickListener(this);
        ((Button) findViewById(R.id.network_wifi_button)).setOnClickListener(this);
    }

    public void onResume() {
        super.onResume();

        Utils.showLog("AccessNetworkActivity Resume");

        if(Utils.isNetWorkConnected()) {
            finish();
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();

        Utils.showLog("AccessNetworkActivity destroyed");
    }
}