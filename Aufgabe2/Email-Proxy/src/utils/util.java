package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by JanDennis on 13.05.2015.
 */
public class util {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static String[] colors = {"\u001B[35m","\u001B[33m","\u001B[34m","\u001B[36m","\u001B[32m"};
    static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));


    private util() {
    }

    public static String readUserInput() {
        String result = null;
        try {
            result = br.readLine();
        } catch (IOException e) {
            writeError("Err: Reading Userinput!");
        }
        if (result.equals("exit")) System.exit(0);
        return result;
    }

    public static void write(String text, String color) {
        System.out.println(color + text + ANSI_RESET);
    }

    public static void writeError(String text) {
        write(text, ANSI_RED);
    }
}
