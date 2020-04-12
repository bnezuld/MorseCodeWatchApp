/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.morsecodewatchapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

//import android.support.v4.app.NotificationCompat;

import android.widget.Button ;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeGatt}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class ConnectedDeviceActivity extends AppCompatActivity implements NotificationListener {
    private final static String TAG = ConnectedDeviceActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeGatt mBluetoothLeGatt;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private TextView txtView;
    public static final String NOTIFICATION_CHANNEL_ID = "10001" ;
    private final static String default_notification_channel_id = "default" ;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeGatt = ((BluetoothLeGatt.LocalBinder) service).getService();
            if (!mBluetoothLeGatt.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeGatt.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeGatt = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeGatt.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeGatt.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeGatt.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeGatt.getSupportedGattServices());
            } else if (BluetoothLeGatt.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BluetoothLeGatt.EXTRA_DATA);
                displayData(data);
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeGatt.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeGatt.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeGatt.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    private void clearUI() {
        //mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_device);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        /*
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
    mConnectionState = (TextView) findViewById(R.id.connection_state);
    */
        mDataField = (TextView) findViewById(R.id.data_value);

        //getActionBar().setTitle(mDeviceName);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeGatt.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        new NotificationService().setListener( this ) ;
        txtView = findViewById(R.id. textView ) ;

        Button btnCreateNotification = findViewById(R.id. btnCreateNotification ) ;
        btnCreateNotification.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("YOUR_CHANNEL_ID",
                            "YOUR_CHANNEL_NAME",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription("YOUR_NOTIFICATION_CHANNEL_DESCRIPTION");
                    mNotificationManager.createNotificationChannel(channel);
                }
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "YOUR_CHANNEL_ID")
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("textTitle")
                        .setContentText("textContent")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                if (android.os.Build.VERSION. SDK_INT >= android.os.Build.VERSION_CODES. O ) {
                    int importance = NotificationManager. IMPORTANCE_HIGH ;
                    NotificationChannel notificationChannel = new NotificationChannel( NOTIFICATION_CHANNEL_ID , "NOTIFICATION_CHANNEL_NAME" , importance) ;
                    mBuilder.setChannelId( NOTIFICATION_CHANNEL_ID ) ;
                    assert mNotificationManager != null;
                    mNotificationManager.createNotificationChannel(notificationChannel) ;
                }
                assert mNotificationManager != null;
                mNotificationManager.notify(( int ) System. currentTimeMillis () , mBuilder.build()) ;
            }
        }) ;

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeGatt != null) {
            final boolean result = mBluetoothLeGatt.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeGatt = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.connected, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeGatt.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeGatt.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id. action_settings :
                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS" ) ;
                startActivity(intent) ;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.append(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, GattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeGatt.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeGatt.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeGatt.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeGatt.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void onClickWrite(View v){
        if(mBluetoothLeGatt != null) {
            EditText editText = findViewById(R.id.editText);
            String message = editText.getText().toString();
            message = message.replaceAll("[^\\p{ASCII}]", "");
            mBluetoothLeGatt.writeMessage(message.toUpperCase() + "\r");

            TextView tv = findViewById(R.id.data_value);
            tv.setText("\n" + message);
        }
    }

    public void onClickRead(View v){
        if(mBluetoothLeGatt != null) {
            mBluetoothLeGatt.readAllCustomCharacteristic();
        }
    }

    @Override
    public void NewNotification (String message, NotificationService.NotificationType notificationType) {
        if(mBluetoothLeGatt != null) {
            message = message.replaceAll("[^\\p{ASCII}]", "");
            //instead of updating and notifying might change when the characteristic is read it will get the next in the queue
            mBluetoothLeGatt.updateCharacteristicValueNotifyDevice(mBluetoothLeGatt.mainDevice,
                    mBluetoothLeGatt.alertNotificationService.getCharacteristic(UUID.fromString(GattAttributes.NEW_ALERT_CHARACTERISTIC)),
                    message);
        }
        txtView .append( " \n " + message) ;
    }
}
