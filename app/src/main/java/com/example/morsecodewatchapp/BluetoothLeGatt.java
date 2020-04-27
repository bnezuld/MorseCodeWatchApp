package com.example.morsecodewatchapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BluetoothLeGatt extends Service {
    private final static String TAG = BluetoothLeGatt.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    BluetoothLeAdvertiser bluetoothLeAdvertiser;
    BluetoothGattServer bluetoothGattServer;
    BluetoothGattService alertNotificationService;
    BluetoothDevice mainDevice;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String OPERATION_COMPLETE =
            "com.example.bluetooth.le.OPERATION_COMPLETE";
    public final static String OPERATION_STARTED =
            "com.example.bluetooth.le.OPERATION_STARTED";

    private Queue<String> WriteQueue = new LinkedList<>();
    private Semaphore sem = new Semaphore(1);

    private Map<String, Stack<Queue<String>>> chracteristicWriteQueue = new HashMap<>();

    AdvertiseSettings settings = new AdvertiseSettings.Builder()
            .setConnectable(true)
            .build();

    AdvertiseData advertiseData = new AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(true)
            .build();

    AdvertiseData scanResponseData = new AdvertiseData.Builder()
            .addServiceUuid(new ParcelUuid(UUID.fromString(GattAttributes.ALERT_NOTIFICATION_SERVICE_UUID)))
            .setIncludeTxPowerLevel(true)
            .build();


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                //Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                //Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                //Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
                sem.release();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                //Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            //Log.w(TAG, "Reading Status: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
            sem.release();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            sem.release();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //writeCustomCharacteristicsQueue();
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    BluetoothGattServerCallback mGattServercallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            byte[] value = characteristic.getValue();
            if(bluetoothGattServer.sendResponse ( device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value )) {
                String uuid = characteristic.getUuid().toString();
                if(chracteristicWriteQueue.containsKey(uuid) && !chracteristicWriteQueue.get(uuid).isEmpty()) //stack not empty(not notifications)
                {
                    chracteristicWriteQueue.get(uuid).peek().poll();
                    String nextMessageSegment = chracteristicWriteQueue.get(uuid).peek().peek();
                    if(value[2] == 0x01) //the message contains more segments
                    {
                        updateCharacteristicValueNotifyDevice(device, characteristic, nextMessageSegment);
                        //updateCharacteristicValue(characteristic, nextMessageSegment);
                    }else{ //this is the last segment, so the next time it will read the next newest notification
                        chracteristicWriteQueue.get(uuid).pop();//remove currect stacked notification
                        String nextMessageStart = chracteristicWriteQueue.get(uuid).peek().peek();//start the message for next notification
                        updateCharacteristicValue(characteristic, nextMessageStart);
                    }
                }else{
                    characteristic.setValue((byte[])null);
                }
                //characteristic.setValue((byte[])null);
                //service.getCharateristic(characteristic.getUuid().toString()) Map[characteristic.getUuid().toString()].getNext();
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            characteristic.setValue(value);
            if(responseNeeded) {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            descriptor.setValue(value);
            if(responseNeeded) {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            byte[] value = descriptor.getValue();
            bluetoothGattServer.sendResponse ( device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value );
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
        }
    };

    AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "BLE advertisement added successfully");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Failed to add BLE advertisement, reason: " + errorCode);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA,  characteristic.getUuid().toString() + "  " + characteristic.getService().getUuid()  + "\n" + new String(data) + "\n" + stringBuilder.toString() + "\n\n");
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeGatt getService() {
            return BluetoothLeGatt.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if(CreateAdvertisedService() == false){
            Log.e(TAG, "Unable to obtain Advertise service.");
            return false;
        }

        return true;
    }

    public boolean CreateAdvertisedService(){

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        if(bluetoothLeAdvertiser == null) {
            bluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback);


            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to obtain a BluetoothManager.");
                return false;
            }
            bluetoothGattServer = mBluetoothManager.openGattServer(getApplicationContext(), mGattServercallback);
            alertNotificationService = new BluetoothGattService(UUID.fromString(GattAttributes.ALERT_NOTIFICATION_SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

            BluetoothGattDescriptor descriptor;
            byte[] byteFilled = {127};
            BluetoothGattCharacteristic mSupportedNewAlertCategory = new BluetoothGattCharacteristic(UUID.fromString(GattAttributes.NEW_ALERT_CATAGORY),
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
            mSupportedNewAlertCategory.setValue(new byte[]{0x1f});
            /*
             * New alert part (char & descriptor)
             * */
            BluetoothGattDescriptor clientCharacteristicConfigNa = new BluetoothGattDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION),
                    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            clientCharacteristicConfigNa.setValue(new byte[]{1});

            BluetoothGattCharacteristic mNewAlert = new BluetoothGattCharacteristic(UUID.fromString(GattAttributes.NEW_ALERT_CHARACTERISTIC),
                    BluetoothGattCharacteristic.PROPERTY_WRITE |BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

            mNewAlert.addDescriptor(clientCharacteristicConfigNa);
            /*
             * Unread alert status associated with new alert
             * */
            BluetoothGattDescriptor clientCharacteristicConfigUa = new BluetoothGattDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION), BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            clientCharacteristicConfigUa.setValue(new byte[]{1});

            BluetoothGattCharacteristic mUnreadAlertStatus = new BluetoothGattCharacteristic(UUID.fromString(GattAttributes.UNREAD_CHARACTERISTIC),
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
            mUnreadAlertStatus.addDescriptor(clientCharacteristicConfigUa);

            BluetoothGattCharacteristic mSupportedUnreadCategory = new BluetoothGattCharacteristic(UUID.fromString(GattAttributes.UNREAD_CATAGORY),
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_NOTIFY  | BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
            mSupportedUnreadCategory.setValue(new byte[]{0x1f});

            BluetoothGattCharacteristic mAlertNotificationControlPoint = new BluetoothGattCharacteristic(UUID.fromString(GattAttributes.ALERT_NOTIFICATION_CONTROL),
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

            alertNotificationService.addCharacteristic(mSupportedNewAlertCategory);
            alertNotificationService.addCharacteristic(mNewAlert);
            alertNotificationService.addCharacteristic(mSupportedUnreadCategory);
            alertNotificationService.addCharacteristic(mUnreadAlertStatus);
            alertNotificationService.addCharacteristic(mAlertNotificationControlPoint);

            bluetoothGattServer.addService(alertNotificationService);
            return true;
        }
        return true;
    }

    public boolean RemoveAdvertisedService(){
        if(bluetoothLeAdvertiser == null){
            Log.e(TAG, "Unable to obtain a BluetoothLeAdvertiser.");
            return false;
        }
        if (bluetoothGattServer == null) {
            Log.e(TAG, "Unable to obtain a BluetoothGattServer.");
            return false;
        }
        bluetoothGattServer.removeService(alertNotificationService);
        //bluetoothGattServer.close();

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        bluetoothLeAdvertiser = null;

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        mainDevice = mBluetoothAdapter.getRemoteDevice(address);
        if (mainDevice == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = mainDevice.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        RemoveAdvertisedService();

        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        /*if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }*/
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void readAllCustomCharacteristic() {
        try {
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();
            for (BluetoothGattService gattService : gattServices) {
                Log.i(TAG, "service UUID Found: " + gattService.getUuid().toString());
                /*check if the service is available on the device*/
                //BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(GattAttributes.UART_SERVICE_UUID));
                if (gattService == null) {
                    Log.w(TAG, "Custom BLE Service not found");
                    return;
                }
                /*get the read characteristic from the service*/
                //mBluetoothGatt.isCharacteristicReadable(mCustomService, UUID.fromString(GattAttributes.RX_CHARACTERISTIC))
                List<BluetoothGattCharacteristic> b = gattService.getCharacteristics();
                boolean failed = true;
                for (BluetoothGattCharacteristic bgc : b) {
                    Log.i(TAG, "    characteristic UUID Found: " + bgc.getUuid().toString());
                    BluetoothGattCharacteristic mReadCharacteristic = gattService.getCharacteristic(bgc.getUuid());
                    sem.acquire();

                    if (mBluetoothGatt.readCharacteristic(mReadCharacteristic) == false) {
                        sem.release();
                        Log.w(TAG, "Failed to read characteristic");
                    } else {
                        failed = false;
                        Log.w(TAG, "Success to read characteristic");
                    }
                }
            }
        }catch (InterruptedException exc) {
            //System.out.println(exc);
        }

    }

    public synchronized void writeCustomCharacteristic(String value) {
        try {
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }

            /*check if the service is available on the device*/
            BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(GattAttributes.ALERT_NOTIFICATION_SERVICE_UUID));
            if (mCustomService == null) {
                Log.w(TAG, "Custom BLE Service not found");
                return;
            }

            /*get the read characteristic from the service*/
            BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(GattAttributes.NEW_ALERT_CHARACTERISTIC));
            mWriteCharacteristic.setValue(value);//, BluetoothGattCharacteristic.FORMAT_UINT8,0);
            sem.acquire();
            if (mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false) {
                Log.w(TAG, "Failed to write characteristic");
                sem.release();
            } else {
                Log.w(TAG, "Success to write characteristic");
            }
        } catch (InterruptedException exc) {
            //System.out.println(exc);
        }
    }

    public void writeMessage(String Message){
        int times = Byte.valueOf(String.valueOf(
                Message.length() / 20)) + 1;
        for(int i = 1; i <= times; i++)
        {
            int begin = i > 1 ? (i - 1) * 20 : 0;
            int end = i == times ? Message.length() : i * 20;
            Log.w(TAG, "i: " + i + "/" + times +  ", " + begin + "-" + end);

            writeCustomCharacteristic(Message.substring(begin, end));
        }
    }

    public void updateCharacteristicValue(BluetoothGattCharacteristic characteristic, String value)
    {
        if(value.length() < 20) {
            char[] charArray = new char[20 - value.length()];
            Arrays.fill(charArray, '\0');
            value += new String(charArray);
        }
        characteristic.setValue(value);//new byte[]{0x03, 0x01, 0x4d, 0x61,0x72, 0x79});
    }

    public boolean notifyCharacteristicChange(BluetoothDevice device, BluetoothGattCharacteristic characteristic)
    {
        if(characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION)).getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) {
            return bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
        }
        return false;
    }

    public boolean updateCharacteristicValueNotifyDevice(BluetoothDevice device, BluetoothGattCharacteristic characteristic, String value)
    {
        updateCharacteristicValue(characteristic, value);
        return notifyCharacteristicChange(device, characteristic);
    }

    public boolean AddMessageToCharacteristic(BluetoothGattCharacteristic characteristic, Queue<String> message)
    {
        String uuid = characteristic.getUuid().toString();
        if(!chracteristicWriteQueue.containsKey(uuid))
        {
            chracteristicWriteQueue.put(uuid, new Stack<Queue<String>>());
        }
        chracteristicWriteQueue.get(uuid).add(message);
        updateCharacteristicValue(characteristic, message.peek());
        return true;
    }
}
