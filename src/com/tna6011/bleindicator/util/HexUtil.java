package com.tna6011.bleindicator.util;

import java.net.URLDecoder;

public final class HexUtil {
    
    public static byte[] hexToByteArray(String hex) {
        if (hex == null || hex.length() == 0) {
            return null;
        }

        byte[] ba = new byte[hex.length() / 2];
        for (int i = 0; i < ba.length; i++) {
            ba[i] = (byte) Integer
                    .parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return ba;
    }

    public static String byteArrayToHex(byte[] ba) {
        if (ba == null || ba.length == 0) {
            return null;
        }

        StringBuffer sb = new StringBuffer(ba.length * 2);
        String hexNumber;
        for (int x = 0; x < ba.length; x++) {
            hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

            sb.append(hexNumber.substring(hexNumber.length() - 2));
        }
        return sb.toString();
    }

    public static String hexToKorean(String hex){
        
        try{
            StringBuffer sb = new StringBuffer();
            int index = 0;
            for(int i = 0; i < hex.length()/2; i++, index=index+2) {
                sb.append("%");
                sb.append(hex.substring(index,index+2));
            }
            String result = URLDecoder.decode(sb.toString(), "euc-kr");
            if (result.equals("")){
                result = "없음";
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return "오류";
        }

    }
}
