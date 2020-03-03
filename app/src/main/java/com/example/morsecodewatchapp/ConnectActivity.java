package com.example.morsecodewatchapp;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import static android.app.PendingIntent.getActivity;

public class ConnectActivity extends AppCompatActivity {

    ListView mainListView;
    LeDeviceListAdapter mLeDeviceListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        mainListView = (ListView) findViewById(R.id.list);
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Map.Entry<String,?> entry = mLeDeviceListAdapter.getDevice(position);
                if (entry == null) return;
                final Intent intent = new Intent(ConnectActivity.this, ConnectedDeviceActivity.class);
                intent.putExtra(ConnectedDeviceActivity.EXTRAS_DEVICE_NAME, entry.getValue().toString());
                intent.putExtra(ConnectedDeviceActivity.EXTRAS_DEVICE_ADDRESS, entry.getKey());
                startActivity(intent);
            }
        });
        mainListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
                final Map.Entry<String,?> entry = mLeDeviceListAdapter.getDevice(position);
                if (entry == null) return false;
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.remove(entry.getKey());
                editor.commit();

                mLeDeviceListAdapter = new LeDeviceListAdapter();
                mainListView.setAdapter(mLeDeviceListAdapter);

                for (Map.Entry<String,?> sharedPrefEntry : sharedPref.getAll().entrySet()) {
                    mLeDeviceListAdapter.addEntry(sharedPrefEntry);
                }
                return true;
            }
        });

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mainListView.setAdapter(mLeDeviceListAdapter);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        for (Map.Entry<String,?> entry : sharedPref.getAll().entrySet()) {
            mLeDeviceListAdapter.addEntry(entry);
        }
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<Map.Entry<String,?>> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<Map.Entry<String,?>>();
            mInflator = ConnectActivity.this.getLayoutInflater();
        }

        public void addEntry(Map.Entry<String,?> entry) {
            if(!mLeDevices.contains(entry)) {
                mLeDevices.add(entry);
            }
        }

        public void removeEntry(int position) {
            mLeDevices.remove(position);
        }

        public Map.Entry<String,?> getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            PairDeviceActivity.ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new PairDeviceActivity.ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (PairDeviceActivity.ViewHolder) view.getTag();
            }

            Map.Entry<String,?> entry = mLeDevices.get(i);
            final String deviceName = entry.getValue().toString();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(entry.getKey());

            return view;
        }
    }


}
