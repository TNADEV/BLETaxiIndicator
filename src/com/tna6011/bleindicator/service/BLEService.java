package com.tna6011.bleindicator.service;

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
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.util.List;

import static com.tna6011.bleindicator.util.IndicatorUtil.ACTION_DATA_AVAILABLE;
import static com.tna6011.bleindicator.util.IndicatorUtil.ACTION_GATT_CONNECTED;
import static com.tna6011.bleindicator.util.IndicatorUtil.ACTION_GATT_DISCONNECTED;
import static com.tna6011.bleindicator.util.IndicatorUtil.ACTION_GATT_SERVICES_DISCOVERED;
import static com.tna6011.bleindicator.util.IndicatorUtil.CCCD;
import static com.tna6011.bleindicator.util.IndicatorUtil.DEVICE_DOES_NOT_SUPPORT_UART;
import static com.tna6011.bleindicator.util.IndicatorUtil.EXTRA_DATA;
import static com.tna6011.bleindicator.util.IndicatorUtil.RX_CHAR_UUID;
import static com.tna6011.bleindicator.util.IndicatorUtil.RX_SERVICE_UUID;
import static com.tna6011.bleindicator.util.IndicatorUtil.TX_CHAR_UUID;

public class BLEService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private int mConnectionState = STATE_DISCONNECTED;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                // TO-DO : GATT 서비스를 발견했으나, 연결하지 못했을 때 처리.
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                // TO-DO : 블루투스 매니저의 초기화가 불가능할 때 처리.
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            // TO-DO : 블루투스 매니저로부터 어댑터를 얻을 수 없을 때 처리.
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {

            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            // TO-DO : 디바이스를 찾을 수 없을 때 처리.
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void enableTXNotification() {

        BluetoothGattService RxService = mBluetoothGatt
                .getService(RX_SERVICE_UUID);
        if (RxService == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattCharacteristic TxChar = RxService
                .getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(TxChar, true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    public void writeRXCharacteristic(byte[] value) {

        BluetoothGattService RxService = mBluetoothGatt
                .getService(RX_SERVICE_UUID);

        if (RxService == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattCharacteristic RxChar = RxService
                .getCharacteristic(RX_CHAR_UUID);

        if (RxChar == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        RxChar.setValue(value);
        mBluetoothGatt.writeCharacteristic(RxChar);
    }

}
