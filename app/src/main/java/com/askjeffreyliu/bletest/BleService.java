package com.askjeffreyliu.bletest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static com.askjeffreyliu.bletest.AndroidBluetoothEventReceiver.BLE_OS_EVENT_ADAPTER_NOT_ON;
import static com.askjeffreyliu.bletest.AndroidBluetoothEventReceiver.BLE_OS_EVENT_ADAPTER_ON;
import static com.askjeffreyliu.bletest.AndroidBluetoothEventReceiver.BLE_OS_EVENT_BOND_STATE_BONDED;

public class BleService extends Service {
    private static final String TAG = "BleService";

    public static final String BLE_DEVICE = "ble_device";

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothManager mBluetoothManager = null;
    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothGattCharacteristic gattCharacteristicRx = null;
    private BluetoothGattCharacteristic gattCharacteristicTx = null;
    private BluetoothGattCharacteristic batteryCharacteristic = null;

    @Override
    public void onCreate() {
        super.onCreate();

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Logger.e("Unable to initialize BluetoothManager.");
            Toast.makeText(this, "Unable to initialize BluetoothManager.", Toast.LENGTH_SHORT).show();
            return;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Logger.e("Unable to obtain a BluetoothAdapter.");
            Toast.makeText(this, "Unable to obtain a BluetoothAdapter", Toast.LENGTH_SHORT).show();
            return;
        }


        EventBus.getDefault().register(this);
    }

    // currently onstartcommand will do nothing but connecting to an
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mBluetoothDevice = intent.getParcelableExtra(BLE_DEVICE);

        if (mBluetoothDevice != null)
            mBluetoothGatt = mBluetoothDevice.connectGatt(this, false, mGattCallback);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // This method will be called when a MessageEvent is posted
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageParsed(BleOsEvent event) {
        switch (event.message) {
            case BLE_OS_EVENT_ADAPTER_NOT_ON: {
                Logger.d("BLE_OS_EVENT_ADAPTER_NOT_ON() called with: event = [" + event + "]");
                break;
            }
            case BLE_OS_EVENT_ADAPTER_ON: {
                Logger.d("BLE_OS_EVENT_ADAPTER_ON() called with: event = [" + event + "]");
                break;
            }
            case BLE_OS_EVENT_BOND_STATE_BONDED: {
                Logger.d("BLE_OS_EVENT_BOND_STATE_BONDED() called with: event = [" + event + "]");
                if (!setCharacteristicNotification(gattCharacteristicRx, true)) {
                    // clear();
                }
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        disconnect();
        Log.d(TAG, "onDestroy() called");
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    private boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                  boolean enabled) {
        if (mBluetoothAdapter == null) {
            Logger.e("set char: mBluetoothAdapter null");
            return false;
        }
        if (mBluetoothGatt == null) {
            Logger.e("set char: mBluetoothGatt null");
            return false;
        }
        if (characteristic == null) {
            Logger.e("set char: characteristic null");
            return false;
        }
        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {
            Logger.e("setCharacteristicNotification failed");
            return false;
        }

        if (Constant.UUID_SWYP_NORDIC_RX_CHARACTERISTIC.equals(characteristic.getUuid().toString())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(Constant.UUID_SWYP_NORDIC_RX_NOTIFICATION_DESCRIPTOR));
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                return mBluetoothGatt.writeDescriptor(descriptor);
            }
        }
        return false;
    }


    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null) {
            //Logger.e("disconnect: BluetoothAdapter not initialized");
            return;
        }
        if (mBluetoothGatt == null) {
            //Logger.e("disconnect: mBluetoothGatt not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Logger.d("onConnectionStateChange() STATE_CONNECTED");


                mBluetoothGatt = gatt;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Logger.e("Disconnected from GATT server.");


                // add delay for reconnecting, because it is possible the service doesn't know bluetooth is off
                stopSelf();


            } else {
                Logger.e("onConnectionStateChange get state " + status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Logger.d("onServicesDiscovered successfully");
                mBluetoothGatt = gatt;
                displayGattServices(gatt.getServices());
            } else {
                Logger.e("onServicesDiscovered failed " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(UUID.fromString(Constant.UUID_SWYP_NORDIC_RX_CHARACTERISTIC))) {
                Logger.d("onCharacteristicChanged() called with: " + "gatt = [" + gatt + "], characteristic = [" + characteristic + "]");

                String senddata = UtilsBLE.bytesToHex(characteristic.getValue());
                Logger.d("receiving " + formatHexString(senddata));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor != null && Constant.UUID_SWYP_NORDIC_RX_NOTIFICATION_DESCRIPTOR.equals(descriptor.getUuid().toString())) {
                    byte[] value = descriptor.getValue();
                    if (value.length == 2 && value[0] == 0x01 && value[1] == 0x00) {
                        Logger.d("onDescriptorWrite successfully " + UtilsBLE.bytesToHex(descriptor.getValue()));


                        // send 0402 0000 0000 ffff 0000
                        byte[] getFwPkt = {(byte) 0x4, (byte) 0x2, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0xff, (byte) 0xff, (byte) 0x0, (byte) 0x0};

                        //String senddata = UtilsBLE.bytesToHex(getFwPkt);
                        //Logger.d("sending " + BLEMgr.formatHexString(senddata)); // response should be 04ff 0402 0000 0000 4b78 000a 0000 0193 0004 1002     0001 0300?

                        if (gattCharacteristicTx != null) {
                            gattCharacteristicTx.setValue(getFwPkt);
                            writeCharacteristic(gattCharacteristicTx);
                        } else {
                            Logger.e("gattCharacteristicTx is null");
                            //clear();
                        }
                        return;
                    }
                }
            }


            Logger.e("onDescriptorWrite: status error, status = " + status);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logger.d("onCharacteristicWrite() successful");
                // onCharacteristicsWrittenEvent();
            } else {
                Logger.e("onCharacteristicWrite() fail " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                Logger.d("onCharacteristicRead() successful battery read " + batteryLevel);
//                Utils.setBatteryLevel(BleService.this, batteryLevel);
//                EventBus.getDefault().post(new OnReceiveBatteryLevelEvent(batteryLevel));
            } else {
                Logger.e("onCharacteristicRead: status error, status = " + status);
            }
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(final List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String uuid = gattService.getUuid().toString();

            if (uuid.equalsIgnoreCase(Constant.UUID_SWYP_NORDIC_SERVICE)) {
                for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                    uuid = gattCharacteristic.getUuid().toString();
                    int charaProp = gattCharacteristic.getProperties();

                    if (Constant.UUID_SWYP_NORDIC_RX_CHARACTERISTIC.equals(uuid)) {
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            gattCharacteristicRx = gattCharacteristic;
                        }
                    } else if (Constant.UUID_SWYP_NORDIC_TX_CHARACTERISTIC.equals(uuid)) {
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                            gattCharacteristicTx = gattCharacteristic;
                        }
                    }
                }
            } else if (uuid.equalsIgnoreCase(Constant.UUID_SWYP_BATTERY_SERVICE)) {
                for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                    uuid = gattCharacteristic.getUuid().toString();
                    int charaProp = gattCharacteristic.getProperties();
                    if (Constant.UUID_SWYP_BATTERY_CHARACTERISTIC.equals(uuid)) {
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            batteryCharacteristic = gattCharacteristic;
                        }
                    }
                }
            }
        }

        if (batteryCharacteristic == null) {
            Logger.e("NO battery characteristic, if fw latest?");
        }

        if (gattCharacteristicRx == null ||
                gattCharacteristicTx == null) {
            Logger.e("displayGattServices: gattCharacteristic null, prepare to refresh bond");
            return;
        }


        Logger.d("displayGattServices: end");

        if (mBluetoothDevice.getBondState() == BOND_BONDED) {
            if (!setCharacteristicNotification(gattCharacteristicRx, true)) {
                // clear();
            }
        } else {
            Logger.d("displayGattServices: CREATING BOND!!");
            boolean createBondResult = mBluetoothDevice.createBond();
            Logger.d("displayGattServices: createBondResult = " + createBondResult);
        }

        //    boolean createBondResult = mBluetoothDevice.createBond();

//        Logger.d("displayGattServices: createBondResult = " + createBondResult);
//        if (!createBondResult) {
//            // most likely bond was already created, now just enable notification
//            if (!setCharacteristicNotification(gattCharacteristicRx, true)) {
//                // clear();
//            }
//        }
        // now go check if bond is completed
    }

    private void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || characteristic == null) {
            Logger.e("write char:BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    private String formatHexString(String input) {
        StringBuilder str = new StringBuilder(input);
        int idx = str.length() - 4;
        while (idx > 0) {
            str.insert(idx, " ");
            idx = idx - 4;
        }
        return str.toString();
    }
}
