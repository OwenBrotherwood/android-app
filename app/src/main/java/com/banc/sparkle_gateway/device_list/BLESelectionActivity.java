package com.banc.sparkle_gateway.device_list;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.banc.BLEManagement.BLEDeviceInfo;
import com.banc.BLEManagement.BLEDeviceInfoList;
import com.banc.BLEManagement.BLEEvent;
import com.banc.sparkle_gateway.R;
import com.banc.sparkle_gateway.login.ParticleLoginActivity;
import com.banc.sparkle_gateway.service.BLEService;
import com.banc.sparkle_gateway.service.ServiceManager;
import com.banc.util.Utils;
import com.banc.util.Values;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class BLESelectionActivity extends AppCompatActivity {

    private static ServiceManager sManager;
    static BLEDeviceInfoList sCurrentDevices;

    private DeviceAdapter mAdapter;

    private Button mScanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("GloveSelection", "Creating Glove Selection");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_list);

        ParticleCloudSDK.init(this);

        if (!ParticleCloudSDK.getCloud().isLoggedIn()) {
            Intent loginIntent = new Intent(this, ParticleLoginActivity.class);
            loginIntent.putExtra(Values.EXTRA_FROM_START, true);
            startActivity(loginIntent);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.t_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
        }

        ListView list = (ListView) findViewById(R.id.listView1);
        mAdapter = new DeviceAdapter(this);
        list.setAdapter(mAdapter);

        mScanButton = (Button) findViewById(R.id.scanButton);

        sManager = new ServiceManager(this, BLEService.class, new HandlerExtension());
        if (!sManager.isRunning()) {
            Log.d("GloveSelection", "Service is not running. Starting!");
            sManager.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.getItem(0);
        if (ParticleCloudSDK.getCloud().isLoggedIn()) {
            item.setTitle(R.string.logout);
        } else {
            item.setTitle(R.string.login);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_login) {
            loginButtonPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        Log.d("GloveSelection", "Starting Glove Selection");
        super.onStart();

        invalidateOptionsMenu();

        sManager.bind();
        if (Utils.isBluetoothEnabled()
                && Utils.hasLocationPermission(this)
                && Utils.locationIsEnabled(this)) {
            startScan();
        } else {
            stopScan();
        }
    }

    @Override
    protected void onStop() {
        Log.d("GloveSelection", "Stopping Glove Selection");
        super.onStop();

        //tell the service to stop discovery
        stopScan();
    }

    @Override
    protected void onDestroy() {
        Log.d("GloveSelection", "Destroying Glove Selection");
        super.onDestroy();

        sManager.stop();
        sManager.unbind();
    }

    public void loginButtonPressed() {
        // Do something in response to button
        Log.d("Clicked", "Clicked");
        if (ParticleCloudSDK.getCloud().isLoggedIn()) {
            ParticleCloudSDK.getCloud().logOut();
            invalidateOptionsMenu();
        } else {
            Intent intent = new Intent(this, ParticleLoginActivity.class);
            startActivity(intent);
        }
    }

    public void scanButtonPressed(View view) {
        // Do something in response to button
        Log.d("Clicked", "Clicked");

        if ("stop".equalsIgnoreCase((String) mScanButton.getText())) {
            stopScan();
        } else {
            if (!Utils.isBluetoothEnabled()) {
                Utils.enableBluetooth(this);
            } if (!Utils.hasLocationPermission(this)) {
                Utils.askLocationPermission(this);
            } if (!Utils.locationIsEnabled(this)) {
                Utils.enableLocation(this);
            } else {
                startScan();
            }
        }
    }

    private void updateTable(BLEDeviceInfoList devices) {
        sCurrentDevices = devices;
        if (mAdapter != null) {
            mAdapter.updateDevices(devices);
        }
    }

    private void startScan() {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putInt("info", BLEService.START_DISCOVERY);
        msg.setData(b);
        try {
            sManager.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScanButton.setText(R.string.stop);
            }
        });
    }

    private void stopScan() {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putInt("info", BLEService.STOP_DISCOVERY);
        msg.setData(b);
        try {
            sManager.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScanButton.setText(R.string.scan);
            }
        });
    }

    private class HandlerExtension extends Handler {

        @Override
        public void handleMessage(Message msg) {
            int type = msg.getData().getInt("BLEEventType", -1);

            if (type != -1) {
                //this means it is a BLEEvent from the service
                BLEEvent event = (BLEEvent) msg.obj;
                if (event.BLEEventType == BLEEvent.EVENT_UPDATE || event.BLEEventType == BLEEvent.EVENT_DEVICE_STATE_CHANGE) {
                    BLEDeviceInfoList devices = (BLEDeviceInfoList) event.Contents;
                    updateTable(devices);
                }
            } else {
                //otherwise, it is a message from the ServiceManager
                type = msg.getData().getInt("info", -1);
                if (type == ServiceManager.SERVICE_BOUND) {
                    Message discoverMessage = new Message();
                    Bundle b = new Bundle();
                    b.putInt("info", BLEService.START_DISCOVERY);
                    discoverMessage.setData(b);
                    try {
                        sManager.send(discoverMessage);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            //msg.recycle();
        }
    }

    public OnClickListener connectButtonClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (Integer) v.getTag();
            Log.d("DEBUG", "Clicked connect Button at Position: " + position);
            BLEDeviceInfo devInfo = sCurrentDevices.GetBLEDeviceInfo(position);
            if (devInfo.State == BLEDeviceInfo.STATE_CONNECTING) {
                Log.d("DEBUG", "User selected STATE_CONNECTING");
                return;
            }
            String address = devInfo.GetMAC();
            Log.d("DEBUG", "User selected " + address);
            Message msg = new Message();
            Bundle b = new Bundle();
            if (devInfo.State == BLEDeviceInfo.STATE_CONNECTED) {
                b.putInt("info", BLEService.DISCONNECT);
            } else {
                b.putInt("info", BLEService.CONNECT);
            }
            b.putString("address", address);
            msg.setData(b);
            try {
                sManager.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    public OnClickListener claimButtonClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (Integer) v.getTag();
            Log.d("DEBUG", "Clicked Claim Button at Position: " + position);
            final BLEDeviceInfo devInfo = sCurrentDevices.GetBLEDeviceInfo(position);
            Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {
                @Override
                public Object callApi(@NonNull ParticleCloud sparkCloud) throws ParticleCloudException, IOException {
                    ParticleCloudSDK.getCloud().claimDevice(devInfo.GetCloudID());
                    return 1;
                }

                @Override
                public void onSuccess(@NonNull Object value) {
                    Log.d("Device Claimed", "");
                    devInfo.SetClaimed(true);
                    Toaster.s(BLESelectionActivity.this, "Claimed!");
                    updateTable(sCurrentDevices);
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException e) {
                    Log.d("Device Not Claimed", "");
                    Toaster.s(BLESelectionActivity.this, "Error Claiming Device!");
                }
            });
        }
    };

}
