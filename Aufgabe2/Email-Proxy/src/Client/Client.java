package Client;

import utils.util;

import java.io.*;
import java.net.Socket;

/**
 * Created by JanDennis on 13.05.2015.
 */
public class Client implements Runnable {

    private String myColor = "\u001B[34m"; // BLUE
    private String host;
    private int port;
    private String username;
    private String password;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    public Boolean shouldStop = false;
    private static final String OK = "+OK";
    private final String ENDE = "\r\n";

    public Client(String host, int port, String user, String pass, String color) {
        this.host = host;
        this.port = port;
        this.username = user;
        this.password = pass;
        this.myColor = color;
    }

    public void run() {
        write("Client started.");
        try {
            connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (!shouldStop) {
            int mailCount = checkMails();
            while (mailCount > 0) {
                getMails(mailCount);
            }
            try {
                wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void connect() throws IOException {
        write("Connecting to " + host + ":" + port + ", as " + username);
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            write("Connection failed!");
            e.printStackTrace();
        }

        if (!readTextAndStartsWithOK()) {
            write("-ERR: Server don't want to answer you!");
            return;
        }

        sendUsername(username);
        if (!readTextAndStartsWithOK()) {
            write("-ERR: Invalid Username.");
            return;
        }

        sendPassword(password);
        if (!readTextAndStartsWithOK()) {
            write("-ERR: Invalid password or this account is locked.");
            return;
        }
    }

    private Integer checkMails() {
        String result = null;
        String backslashRN = "\r\n";
        try {
            writer.write("STAT" + ENDE);
            result = readText(backslashRN);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return Integer.parseInt(result.substring(0, result.indexOf(' ')));
    }

    private void getMails(int number) {

        String message = null;
        String retrEnding = "\r\n.\r\n";
        try {
            writer.write("RETR " + number + ENDE);
            message = readText(retrEnding);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileWriter fstream = new FileWriter("Email" + number);
            fstream.write(message);
            fstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer.write("DELE " + number + ENDE);
            if (!readTextAndStartsWithOK()) {
                write("-ERR: DELE NOT OK");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(String text) {
        util.write(text, myColor);
    }

    private void sendUsername(String username) {

        username = this.username;

        try {
            writer.write("USER" + username + ENDE);
            writer.flush();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private void sendPassword(String password) {

        password = this.password;

        try {
            writer.write("PASS" + password + ENDE);
            writer.flush();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private boolean readTextAndStartsWithOK() {
        String backslashRN = "\r\n";
        return readText(backslashRN).startsWith(OK);
    }

    // prüft beim reader.read, ob der entgegengenommene sting mit einem OK beginnt
    private String readText(String ending) {
        String buffer = "";
        int charState = 0;

        try {
            // Lesen eines einzelnen Characters
            charState = reader.read();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        buffer = buffer + (char) charState;
        // schaue ob mit reader.read nicht das ende erreicht wurde (charstate) und das das letzte zeichen kein \r\n ist
        while ((charState != -1) && (buffer.endsWith(ending) == false)) {

            try {
                charState = reader.read();
                buffer = buffer + (char) charState;
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }

        if (!buffer.startsWith(OK)) {
            throw new RuntimeException("Kein +OK am Anfang vorhanden!");
        }
        return buffer;
    }
}

