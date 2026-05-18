package orionpay.merchant.infrastructure.adapters.output.payment;

import java.nio.charset.StandardCharsets;

public class IsoUtils {

    /**
     * Converts a Hex String to a byte array.
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Converts a String containing decimal digits to a BCD byte array.
     * Pads with a leading '0' if the string length is odd.
     */
    public static byte[] stringToBcd(String num) {
        if (num.length() % 2 != 0) {
            num = "0" + num;
        }
        return hexStringToByteArray(num);
    }

    /**
     * Returns a string padded to the left with '0's to the specified length.
     */
    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);
        return sb.toString();
    }
    
    /**
     * Returns a string padded to the right with spaces to the specified length.
     */
    public static String padRightSpaces(String inputString, int length) {
         if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder(inputString);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Converts a String to ASCII bytes.
     */
    public static byte[] stringToAsciiBytes(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Converts a BCD byte array to a String.
     */
    public static String bcdToString(byte[] bcd) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bcd) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
