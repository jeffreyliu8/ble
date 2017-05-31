package com.askjeffreyliu.bletest;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;


public class AndroidBluetoothEventReceiver extends BroadcastReceiver {
    public static final String BLE_OS_EVENT_ADAPTER_NOT_ON = "com.askjeffreyliu.bletest.adapter_not_on";
    public static final String BLE_OS_EVENT_ADAPTER_ON = "com.askjeffreyliu.bletest.adapter_on";
    public static final String BLE_OS_EVENT_BOND_STATE_BONDED = "com.askjeffreyliu.bletest.bonded";

    public void onReceive(Context ctx, Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED: {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                    case BluetoothAdapter.STATE_TURNING_ON: {
                        //Logger.e("Bluetooth state change NOT on");
                        EventBus.getDefault().post(new BleOsEvent(BLE_OS_EVENT_ADAPTER_NOT_ON));
                        break;
                    }
                    case BluetoothAdapter.STATE_ON: {
                        //Logger.d("Bluetooth is STATE_ON");
                        EventBus.getDefault().post(new BleOsEvent(BLE_OS_EVENT_ADAPTER_ON));
                        break;
                    }
                }
                break;
            }
            default:
            case BluetoothDevice.ACTION_ACL_CONNECTED:
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
            case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED: {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Logger.d("onReceive() called with: action = [" + action + "], device = [" + device.getName() + "]");
                EventBus.getDefault().post(new BleOsEvent(action));
                break;
            }
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Logger.d("onReceive() called with: action = [" + action + "], device = [" + device.getName() + "]");
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                switch (bondState) {
                    case BluetoothDevice.BOND_BONDED:
                        Logger.d("onReceive: BOND_BONDED");
                         EventBus.getDefault().post(new BleOsEvent(BLE_OS_EVENT_BOND_STATE_BONDED));
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Logger.d("onReceive: BOND_BONDING");
                        // EventBus.getDefault().post(new BleOsEvent(BluetoothDevice.BOND_BONDED));
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Logger.d("onReceive: BOND_NONE");
                        // EventBus.getDefault().post(new BleOsEvent(BluetoothDevice.BOND_BONDED));
                        break;
                    default:
                        Logger.e("onReceive: bond state = " + bondState);
                        break;
                }
                break;
            }
        }
    }
}