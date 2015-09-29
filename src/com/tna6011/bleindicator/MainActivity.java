package com.tna6011.bleindicator;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.tna6011.bleindicator.adpater.DeviceListAdapter;
import com.tna6011.bleindicator.adpater.DeviceListAdapter.DeviceListAdapterCallback;
import com.tna6011.bleindicator.service.BLEService;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tna6011.bleindicator.util.HexUtil.byteArrayToHex;
import static com.tna6011.bleindicator.util.HexUtil.hexToByteArray;
import static com.tna6011.bleindicator.util.HexUtil.hexToKorean;
import static com.tna6011.bleindicator.util.IndicatorUtil.ACTION_DATA_AVAILABLE;
import static com.tna6011.bleindicator.util.IndicatorUtil.ACTION_GATT_CONNECTED;
import static com.tna6011.bleindicator.util.IndicatorUtil.ACTION_GATT_DISCONNECTED;
import static com.tna6011.bleindicator.util.IndicatorUtil.ACTION_GATT_SERVICES_DISCOVERED;
import static com.tna6011.bleindicator.util.IndicatorUtil.BLE_REQUEST_EMPTY;
import static com.tna6011.bleindicator.util.IndicatorUtil.BLE_REQUEST_OCCUPIED;
import static com.tna6011.bleindicator.util.IndicatorUtil.BLE_REQUEST_REST;
import static com.tna6011.bleindicator.util.IndicatorUtil.BLE_RX_PREFIX;
import static com.tna6011.bleindicator.util.IndicatorUtil.DEVICE_DOES_NOT_SUPPORT_UART;
import static com.tna6011.bleindicator.util.IndicatorUtil.EXTRA_DATA;
import static com.tna6011.bleindicator.util.IndicatorUtil.getIndicatorRequestStatusString;
import static com.tna6011.bleindicator.util.IndicatorUtil.getIndicatorStatusString;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.METER_RX_PREFIX;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.TACHO_RX_PREFIX;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.TACHO_CHANNEL_OPEN;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.TACHO_CHANNEL_CLOSE;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.TACHO_CAR_INFO;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.getMeterDataDateTimeString;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.getMeterInOutString;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.getMeterInfoString;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.getMeterStatusString;
import static com.tna6011.bleindicator.util.TaxiMeterUtil.isMeterStatusPay;

public class MainActivity extends Activity implements DeviceListAdapterCallback {

    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private static final long SCAN_PERIOD = 10000;
    private final int TYPE_METER_STATUS = 0;
    private final int TYPE_METER_TACHO = 1;

    private int mState = UART_PROFILE_DISCONNECTED;
    private BLEService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBluetoothAdapter = null;

    Map<String, Integer> devRssiValues;
    private Handler mHandler;
    private boolean mScanning;

    private ListView deviceListView;
    private List<BluetoothDevice> deviceList;
    private DeviceListAdapter deviceListAdapter;
    private TextView deviceListLabel;
    private Button btnScan;
    private TextView controlPanelLabel;
    private RelativeLayout controlPanel;
    private Button btnDiscon;
    private TextView deviceStatus;
    private TextView indicatorStatus;
    private TextView meterStatus;
    private TextView timeView;
    private TextView inOutView;
    private TextView infoView;

    private String currentRawStatus;
    private int currentRawStatusType;

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void initApp() {
        mHandler = new Handler();
        deviceListLabel = (TextView) findViewById(R.id.label_device_list);
        btnScan = (Button) findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScanning == false) {
                    scanLeDevice(true);
                } else {
                    scanLeDevice(false);
                }
            }
        });

        controlPanelLabel = (TextView) findViewById(R.id.label_control_panel);
        btnDiscon = (Button) findViewById(R.id.btn_discon);
        btnDiscon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDevice != null) {
                    showDeviceList();
                    mService.disconnect();
                }
            }
        });

        deviceList = new ArrayList<BluetoothDevice>();
        deviceListAdapter = new DeviceListAdapter(this, R.layout.item_device,
                deviceList);
        deviceListAdapter.setCallback(this);
        deviceListView = (ListView) findViewById(R.id.list_device);
        deviceListView.setAdapter(deviceListAdapter);
        devRssiValues = new HashMap<String, Integer>();

        controlPanel = (RelativeLayout) findViewById(R.id.control_panel);

        deviceStatus = (TextView) findViewById(R.id.label_device_status);
        indicatorStatus = (TextView) findViewById(R.id.indicatorStatusView);
        meterStatus = (TextView) findViewById(R.id.meterStatusView);
        timeView = (TextView) findViewById(R.id.dateView);
        inOutView = (TextView) findViewById(R.id.inOutView);
        infoView = (TextView) findViewById(R.id.infoView);
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {

        if (enable) {

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    btnScan.setText(getString(R.string.btn_rescan));
                    deviceListLabel.setText(getString(R.string.label_device_list_complete));

                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            btnScan.setText(getString(R.string.btn_stop_scan));
            deviceListLabel.setText(getString(R.string.label_device_list));
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            btnScan.setText(getString(R.string.btn_rescan));
            deviceListLabel.setText(getString(R.string.label_device_list_complete));
        }
    }

    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;

        for (BluetoothDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        devRssiValues.put(device.getAddress(), rssi);
        if (!deviceFound) {

            deviceList.add(device);
            deviceListAdapter.notifyDataSetChanged();
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addDevice(device, rssi);
                        }
                    });

                }
            });
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder rawBinder) {
            mService = ((BLEService.LocalBinder) rawBinder).getService();

            if (!mService.initialize()) {
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            // 테스트 용도
            // mService.disconnect(mDevice);
            mService = null;
        }
    };

    public void onDeviceSelected(String deviceAddress) {
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
                deviceAddress);
        showControlPanel();
        mService.connect(deviceAddress);
    }

    public void showDeviceList() {
        deviceListLabel.setVisibility(View.VISIBLE);
        deviceListView.setVisibility(View.VISIBLE);
        btnScan.setVisibility(View.VISIBLE);
        controlPanelLabel.setVisibility(View.GONE);
        controlPanel.setVisibility(View.GONE);
        btnDiscon.setVisibility(View.GONE);
    }

    public void showControlPanel() {
        if (mDevice != null) {
            deviceListLabel.setVisibility(View.GONE);
            deviceListView.setVisibility(View.GONE);
            btnScan.setVisibility(View.GONE);
            controlPanelLabel.setVisibility(View.VISIBLE);
            controlPanelLabel.setText("빈차 표시등 - " + mDevice.getName());
            controlPanel.setVisibility(View.VISIBLE);
            btnDiscon.setVisibility(View.VISIBLE);
        }
    }

    public void getDeviceStatus(byte[] rawStatus) {
        String hexStatus = byteArrayToHex(rawStatus);

        if (hexStatus.startsWith(BLE_RX_PREFIX)) {
            indicatorStatus.setText("빈차 표시등 상태 : " + getIndicatorStatusString(hexStatus));
        } else {
            if (hexStatus.startsWith(TACHO_RX_PREFIX)) {
                currentRawStatusType = TYPE_METER_TACHO;
                currentRawStatus = hexStatus;
            } else if (hexStatus.startsWith(METER_RX_PREFIX)) {
                currentRawStatusType = TYPE_METER_STATUS;
                currentRawStatus = hexStatus;
            } else {
                if (currentRawStatusType == TYPE_METER_STATUS) {
                    if ((hexStatus.length() == 38) || (hexStatus.length() == 32)) {
                        currentRawStatus += hexStatus;
                        if (currentRawStatus.length() == 108) {
                            timeView.setText("정보 수신 일시 : " + getMeterDataDateTimeString(currentRawStatus));
                            meterStatus.setText("택시 미터기 상태 : " + getMeterStatusString(currentRawStatus));

                            if (isMeterStatusPay(currentRawStatus)) {
                                inOutView.setText(getMeterInOutString(currentRawStatus));
                                infoView.setText(getMeterInfoString(currentRawStatus));
                            } else {
                                inOutView.setText("");
                                infoView.setText("");
                            }
                            currentRawStatus = "";
                        }
                    }
                } else if (currentRawStatusType == TYPE_METER_TACHO) {
                    currentRawStatus += hexStatus;
                    if (currentRawStatus.length() == 162) {
                        displayTachoInfo();
                        currentRawStatus = "";
                    }
                }
            }
        }

        deviceStatus.setText("수신 완료 (" + java.text.DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + ")");
    }

    public void displayTachoInfo() {
        ((TextView) findViewById(R.id.tachoInfoCarNumber)).setText("차량 번호 : " + hexToKorean(currentRawStatus.substring(8, 38)));
        ((TextView) findViewById(R.id.tachoInfoBusinessNumber)).setText("사업자 번호 : " + hexToKorean(currentRawStatus.substring(38, 68)));
        ((TextView) findViewById(R.id.tachoInfoPhoneNumber)).setText("전화 번호 : " + hexToKorean(currentRawStatus.substring(68, 98)));
        ((TextView) findViewById(R.id.tachoInfoMobileNumber)).setText("휴대폰 번호 : " + hexToKorean(currentRawStatus.substring(98, 128)));
        ((TextView) findViewById(R.id.tachoInfoEtc)).setText("코드 : 기사 [" + hexToKorean(currentRawStatus.substring(128, 136)) + "] / 그룹 [" + hexToKorean(currentRawStatus.substring(136, 144)) + "] / 회사 [" + hexToKorean(currentRawStatus.substring(144, 152)) + "]");
    }

    public void setDeviceStatus(String hexRequest) {
        deviceStatus.setText("송신 중 : 변경 - " + getIndicatorRequestStatusString(hexRequest));
        indicatorStatus.setText("빈차 표시등 상태 : " + getIndicatorRequestStatusString(hexRequest) + " (변경 중)");
        mService.writeRXCharacteristic(hexToByteArray(hexRequest));
    }

    public void requestEmpty(View view) {
        setDeviceStatus(BLE_REQUEST_EMPTY);
    }

    public void requestRest(View view) {
        setDeviceStatus(BLE_REQUEST_REST);
    }

    public void requestOccupied(View view) {
        setDeviceStatus(BLE_REQUEST_OCCUPIED);
    }

    public void directOn(View view) {
        try {
            mService.writeRXCharacteristic(TACHO_CHANNEL_OPEN.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void directOff(View view) {
        try {
            mService.writeRXCharacteristic(TACHO_CHANNEL_CLOSE.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void requestTachoInfo(View view) {
        try {
            mService.writeRXCharacteristic(hexToByteArray(TACHO_CAR_INFO));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, BLEService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showControlPanel();
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            if (action.equals(ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showDeviceList();
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                    }
                });
            }

            if (action.equals(ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }

            if (action.equals(ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent
                        .getByteArrayExtra(EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        getDeviceStatus(txValue);
                    }
                });
            }

            if (action.equals(DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage(getString(R.string.toast_uart_nc));
                mService.disconnect();
            }

        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        service_init();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showMessage(getString(R.string.toast_bt_nc));
            finish();
            return;
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {

                if (!getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_BLUETOOTH_LE)) {
                    showMessage(getString(R.string.toast_ble_nc));
                    finish();
                } else {
                    initApp();
                }

            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onStop() {
        super.onStop();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(
                    UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        currentRawStatus = "";
    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.dialog_quit_title))
                    .setMessage(getString(R.string.dialog_quit_body))
                    .setPositiveButton(getString(R.string.btn_ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    finish();
                                }
                            }).setNegativeButton(getString(R.string.btn_cancel), null).show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    showMessage(getString(R.string.toast_bt_on));
                    initApp();

                } else {
                    showMessage(getString(R.string.toast_bt_off));
                    finish();
                }
                break;
            default:
                break;
        }
    }
}