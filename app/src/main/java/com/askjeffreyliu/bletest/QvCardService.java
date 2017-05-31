package com.askjeffreyliu.bletest;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.askjeffreyliu.bletest.AndroidBluetoothEventReceiver.BLE_OS_EVENT_ADAPTER_NOT_ON;
import static com.askjeffreyliu.bletest.AndroidBluetoothEventReceiver.BLE_OS_EVENT_ADAPTER_ON;
import static com.askjeffreyliu.bletest.AndroidBluetoothEventReceiver.BLE_OS_EVENT_BOND_STATE_BONDED;

public class QvCardService extends Service {
    private static final String TAG = "QvCardService";

    public QvCardService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
                break;
            }
            case BluetoothDevice.ACTION_ACL_CONNECTED: {
                Logger.d("ACTION_ACL_CONNECTED() called with: event = [" + event + "]");
                break;
            }
            case BluetoothDevice.ACTION_ACL_DISCONNECTED: {
                Logger.d("ACTION_ACL_DISCONNECTED() called with: event = [" + event + "]");
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
