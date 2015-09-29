package com.tna6011.bleindicator.util;

import java.util.UUID;

public class IndicatorUtil {
	public final static String ACTION_GATT_CONNECTED = "com.tna6011.bleindicator.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.tna6011.bleindicator.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.tna6011.bleindicator.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.tna6011.bleindicator.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "com.tna6011.bleindicator.EXTRA_DATA";
	public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.tna6011.bleindicator.DEVICE_DOES_NOT_SUPPORT_UART";

	public static final UUID TX_POWER_UUID = UUID
			.fromString("00001804-0000-1000-8000-00805f9b34fb");
	public static final UUID TX_POWER_LEVEL_UUID = UUID
			.fromString("00002a07-0000-1000-8000-00805f9b34fb");
	public static final UUID CCCD = UUID
			.fromString("00002902-0000-1000-8000-00805f9b34fb");
	public static final UUID FIRMWARE_REVISON_UUID = UUID
			.fromString("00002a26-0000-1000-8000-00805f9b34fb");
	public static final UUID DIS_UUID = UUID
			.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	public static final UUID RX_SERVICE_UUID = UUID
			.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID RX_CHAR_UUID = UUID
			.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID TX_CHAR_UUID = UUID
			.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

	public static final String BLE_REQUEST_EMPTY = "900191";
	public static final String BLE_REQUEST_OCCUPIED = "900494";
	public static final String BLE_REQUEST_REST = "900898";
    
	public static final String BLE_RX_PREFIX = "800";
    public static final String BLE_RX_POSTFIX = "d0a";
	public static final String BLE_STATUS_EMPTY = BLE_RX_PREFIX + "1810" + BLE_RX_POSTFIX;
	public static final String BLE_STATUS_DRIVE = BLE_RX_PREFIX + "2820" + BLE_RX_POSTFIX;
	public static final String BLE_STATUS_REST = BLE_RX_PREFIX + "8880" + BLE_RX_POSTFIX;
	public static final String BLE_STATUS_OCCUPIED = BLE_RX_PREFIX + "4840" + BLE_RX_POSTFIX;


    public static String getIndicatorStatusString(String code){
        String result = "";
        if (code.equals(BLE_STATUS_EMPTY)) {
            result = "점등 (빈차)";
        } else if (code.equals(BLE_STATUS_DRIVE)) {
            result = "소등 (주행)";
        } else if (code.equals(BLE_STATUS_REST)) {
            result = "휴무";
        } else if (code.equals(BLE_STATUS_OCCUPIED)) {
            result = "예약";
        }
        return result;
    }

    public static String getIndicatorRequestStatusString(String code){
        String result = "";
        if (code.equals(BLE_REQUEST_REST)) {
            result = "휴무";
        } else if (code.equals(BLE_REQUEST_OCCUPIED)) {
            result = "예약";
        } else if (code.equals(BLE_REQUEST_EMPTY)) {
            result = "점등";
        }
        return result;
    }
}