package Client;

import utils.util;

import java.io.*;
import java.net.Socket;

/**
 * Created by JanDennis on 13.05.2015.
 */
public class Client implements Runnable {
    private static final String OK = "+OK";
    private final String ENDE = "\r\n";
    public Boolean shouldStop = false;
    private String myColor = "\u001B[34m"; // BLUE
    private String host;
    private int port;
    private String username;
    private String password;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    public Client(String host, int port, String user, String pass, String color) {
        this.host = host;
        this.port = port;
        this.username = user;
        this.password = pass;
        this.myColor = color;
    }

    public void run() {
        write("Client started.");
        while (!shouldStop) {
            write("Activate");
            try {
                connect();
            } catch (IOException e) {
                e.printStackTrace();
            }


            int mailCount = checkMails();
            write("MailCount = " + mailCount);
            while (mailCount > 0) {
                getMails(mailCount);
                mailCount--;
            }
            //shouldStop = true;
            quit();

            try {
//                quit();
                //shouldStop = true;
                write("Sleepmode activate");
                Thread.sleep(5000);
                write("Where you are?");
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

    public Integer checkMails() {
        String result = null;
        String backslashRN = "\r\n";
        try {
            write("Send LIST");
            writer.write("LIST" + ENDE);
            writer.flush();
            result = readText(backslashRN + "." + backslashRN);
            write("[ChkMail] Got: " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Integer.parseInt(result.substring(0,result.indexOf("\n")).replace("+OK",""));
        write("Result : " + result);
        write("< " + result.substring(0, result.indexOf("\n")));
        write("> " + result.substring(0, result.indexOf("\n") - 1).replace("+OK ", "") + " <");
        //return Integer.parseInt(result.substring(5, result.indexOf(' ', 5)));
        return Integer.parseInt(result.substring(0, result.indexOf("\n") - 1).replace("+OK ", ""));
    }

    public void getMails(int number) {
        write("Send: RETR " + number);
        String message = null;
        String retrEnding = "\r\n.\r\n";
        try {
            writer.write("RETR " + number + ENDE);
            writer.flush();
            message = readText(retrEnding);
            write("[getMail] Got: " + message);
            message = message.substring(message.indexOf("\n"));
            message = message.replace("\n.\n", "");
            message = message.replace("\r\n.\r\n", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            File mail = new File(System.getProperty("user.dir") + "\\Mails1\\", username + number + ".txt");
            FileWriter fstream = new FileWriter(mail);
            fstream.write(message);
            fstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer.write("DELE " + number + ENDE);
            writer.flush();
            if (!readTextAndStartsWithOK()) {
                write("-ERR: DELE NOT OK");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void quit() {
        try {
            writer.write("QUIT " + ENDE);
            writer.flush();
            readTextAndStartsWithOK();
            socket.close();
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
            writer.write("USER " + username + ENDE);
            writer.flush();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private void sendPassword(String password) {

        password = this.password;

        try {
            writer.write("PASS " + password + ENDE);
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
            //write("Gimme dat Buffer : " + buffer);
        }

        if (!buffer.startsWith(OK)) {
            write("Buffer : " + buffer);
            throw new RuntimeException("Kein +OK am Anfang vorhanden!");
        }
        return buffer;
    }

    private static final class Lock {
    }
}

