package com.example.apoph.dash;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;
import android.os.Handler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by apoph on 24.3.2018.
 */

public class BtUart {
    private static final String TAG = "BTUART";
    private static final int SCAN_PERIOD = 10000;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothScanner;
    private BluetoothGatt mBluetoothGatt;

    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private OnDashDataCallback mCallback;
    private Handler mHandler = new Handler();
    private String mCurrentDev;
    private Map mBleDevices = new HashMap();
    private Context mCtx;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
                mCallback.onDashData("CONNECTED", null);
                BluetoothDevice dev = gatt.getDevice();
                mCurrentDev = dev.getAddress();
                Log.d(TAG, "Connected");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mCallback.onDashData("DISCONNECTED", null);
                Log.d(TAG, "Disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableTXNotification();
            }
        }

        //@Override
        //public void onCharacteristicRead(BluetoothGatt gatt,
        //                                 BluetoothGattCharacteristic characteristic,
        //                                 int status) {
        //    if (status == BluetoothGatt.GATT_SUCCESS) {
        //    }
        //}

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] rx = characteristic.getValue();
            mCallback.onDashData("DATA", rx);
            Log.d(TAG, "RX " + new String(rx));
        }
    };

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice dev = result.getDevice();
            String bdaddr = dev.getAddress();
            String name = dev.getName();

            if (!mBleDevices.containsKey(bdaddr)) {
                Log.i(TAG, "New device: " + bdaddr + " (" + name + ")");
                mBleDevices.put(bdaddr, name);
            }
        }
    };

    private Runnable mScanEnder = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "Stopped scan after " + SCAN_PERIOD + "ms");
            mBluetoothScanner.stopScan(mScanCallback);
            mCallback.onDashData("SCANSTOP", null);
        }
    };

    public boolean init(Context ctx, OnDashDataCallback callback) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }

        mCtx = ctx;
        mCallback = callback;
        // Start BLE scanning
        mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothScanner.startScan(mScanCallback);

        mHandler.postDelayed(mScanEnder, SCAN_PERIOD);

        mCallback.onDashData("SCANSTART", null);
        return true;
    }

    public Map getBleDevices() {
        return mBleDevices;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            mCallback.onDashData("INCOMPATIBLE", null);
            return false;
        }

        mBluetoothGatt = device.connectGatt(mCtx, false, mGattCallback);
        if (mBluetoothGatt == null) {
            mCallback.onDashData("INCOMPATIBLE", null);
            return false;
        }

        return true;
    }

    public boolean connect() {
        if (mCurrentDev != null) {
            return connect(mCurrentDev);
        }

        return false;
    }

    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    public void resetConnected() {
        mCurrentDev = null;
    }

    public void close() {
        mHandler.removeCallbacks(mScanEnder);

        if (mBluetoothScanner != null) {
            mBluetoothScanner.stopScan(mScanCallback);
            mBluetoothScanner = null;
        }

        disconnect();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    private void enableTXNotification() {
        BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
        if (RxService == null) {
            mCallback.onDashData("INCOMPATIBLE", null);
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            mCallback.onDashData("INCOMPATIBLE", null);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar,true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }
}



