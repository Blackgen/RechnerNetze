package util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by Paddy-Gaming on 24.04.2015.
 */
public final class StringUtils {

    public static String encode(String inputText) {
        String out = null;
        try {
            out = URLEncoder.encode(inputText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static String decode(String inputText) {
        String out = null;
        try {
            out = java.net.URLDecoder.decode(inputText, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public static void write(String text, String color, String reset) {
        System.out.println(color + text + reset);
    }
}
