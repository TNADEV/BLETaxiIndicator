package com.tna6011.bleindicator.util;

import static com.tna6011.bleindicator.util.HexUtil.hexToByteArray;

public final class TaxiMeterUtil {

    public static final String METER_RX_PREFIX = "0233305a";
    private static final String METER_STATUS_PAY_NORMAL = "10";
    private static final String METER_STATUS_EMPTY_NORMAL = "20";
    private static final String METER_STATUS_DRIVE_NORMAL = "40";
    private static final String METER_STATUS_EXTRA_NORMAL = "80";
    private static final String METER_STATUS_CALL_NORMAL = "04";
    private static final String METER_STATUS_PAY_MIX = "18";
    private static final String METER_STATUS_EMPTY_MIX = "28";
    private static final String METER_STATUS_DRIVE_MIX = "48";
    private static final String METER_STATUS_EXTRA_MIX = "88";
    private static final String METER_STATUS_CALL_MIX = "04";

    public static final String TACHO_RX_PREFIX = "02344241";
    public static final String TACHO_CHANNEL_OPEN = "+++";
    public static final String TACHO_CHANNEL_CLOSE = "---";
    public static final String TACHO_CAR_INFO = "02303361334303";

    private static String parseHex(String bundle) {
        byte[] b = hexToByteArray(bundle.substring(8, 98));
        String result = "";

        try {
            result = new String(b, "ASCII");
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    private static String getStatusCode(String bundle) {
        return parseHex(bundle).substring(1, 3);
    }

    public static boolean isMeterStatusPay(String bundle) {
        String status = getStatusCode(bundle);

        if ((status.equals(METER_STATUS_PAY_MIX)) || (status.equals(METER_STATUS_PAY_NORMAL))) {
            return true;
        } else {
            return false;
        }
    }

    public static String getMeterDataDateTimeString(String bundle) {
        String dateTime = parseHex(bundle).substring(3, 13);
        return "20" + dateTime.substring(0, 2) + "년 " + dateTime.substring(2, 4) + "월 " + dateTime.substring(4, 6) + "일 " + dateTime.substring(6, 8) + "시 " + dateTime.substring(8, 10) + "분";
    }

    public static String getMeterInOutString(String bundle) {
        String inTime = parseHex(bundle).substring(13, 17);
        String outTime = parseHex(bundle).substring(17, 21);
        return "탑승 시각 : " + inTime.substring(0, 2) + "시 " + inTime.substring(2, 4) + "분 / 하차 시각 : " + outTime.substring(0, 2) + "시 " + outTime.substring(2, 4) + "분";
    }

    public static String getMeterInfoString(String bundle) {
        String cost = parseHex(bundle).substring(21, 27).replaceFirst("^0+(?!$)", "");
        String dist = parseHex(bundle).substring(27, 33).replaceFirst("^0+(?!$)", "");
        String callCost = parseHex(bundle).substring(33, 39).replaceFirst("^0+(?!$)", "");
        return "주행 거리 : " + dist + "m / 요금 : " + cost + "원 (콜비 " + callCost + "원 포함)";
    }

    public static String getMeterStatusString(String bundle) {
        String status = getStatusCode(bundle);
        String result = "";
        if (status.equals(METER_STATUS_CALL_MIX) || status.equals(METER_STATUS_CALL_NORMAL)) {
            result = "호출";
        } else if (status.equals(METER_STATUS_EMPTY_MIX) || status.equals(METER_STATUS_EMPTY_NORMAL)) {
            result = "빈차";
        } else if (status.equals(METER_STATUS_DRIVE_MIX) || status.equals(METER_STATUS_DRIVE_NORMAL)) {
            result = "주행";
        } else if (status.equals(METER_STATUS_EXTRA_MIX) || status.equals(METER_STATUS_EXTRA_NORMAL)) {
            result = "할증";
        } else if (status.equals(METER_STATUS_PAY_MIX) || status.equals(METER_STATUS_PAY_NORMAL)) {
            result = "지불";
        }
        return result;
    }
}
