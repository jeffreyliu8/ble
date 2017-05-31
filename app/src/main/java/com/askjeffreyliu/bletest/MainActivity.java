package com.askjeffreyliu.bletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startService(new Intent(this, QvCardService.class));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                //scan specified devices only with ScanFilter
                ScanFilter scanFilter = new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(Constant.UUID_SWYP_NORDIC_SERVICE))
                        .build();
                List<ScanFilter> scanFilters = new ArrayList<>();
                scanFilters.add(scanFilter);

                ScanSettings scanSetting = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();

                mBluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, scanSetting, mScanCallback);

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                //tryGetPermissionAndExecuteTask();
            } else {
                Log.e(TAG, "Must enable Bluetooth to continue");
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // if (!TextUtils.isEmpty(result.getDevice().getName()))
            Log.d(TAG, "onScanResult: " + result.getDevice().getAddress() + " " + result.getScanRecord().toString() + " " + result.getScanRecord().getDeviceName());
            onScanCardFound(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults() called with: results = [" + results + "]");
            for (ScanResult sr : results) {
                onScanCardFound(sr.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED: {
                    Log.e(TAG, "SCAN_FAILED_ALREADY_STARTED");
                    break;
                }
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED: {
                    Log.e(TAG, "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                    break;
                }
                case SCAN_FAILED_FEATURE_UNSUPPORTED: {
                    Log.e(TAG, "SCAN_FAILED_FEATURE_UNSUPPORTED");
                    break;
                }
                case SCAN_FAILED_INTERNAL_ERROR: {
                    Log.e(TAG, "SCAN_FAILED_INTERNAL_ERROR");
                    break;
                }
                default:
                    Log.e(TAG, "SCAN_FAILED_UNKNOWN_ERROR");
                    break;
            }
        }
    };

    private synchronized void onScanCardFound(BluetoothDevice device) {
        Log.d(TAG, "onLeScan: found " + device.getAddress() + " " + device.getName());
        if (!TextUtils.isEmpty(device.getName()) &&
                device.getName().length() > 4 &&
                device.getName().substring(0, 4).equalsIgnoreCase("swyp")) {
            Logger.d("onLeScan: found " + device.getAddress() + " " + device.getName());


            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);


            boolean result = device.createBond();
            Log.d(TAG, "onScanCardFound: " + result);
        }
    }

    @Override
    protected void onStop() {
        //mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
        super.onDestroy();
    }
}
