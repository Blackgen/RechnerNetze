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
    private String Username;
    private String Password;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    public Boolean shouldStop = false;

    public Client(String host, int port, String user, String pass,String color) {
        this.host = host;
        this.port = port;
        this.Username = user;
        this.Password = pass;
        this.myColor = color;
    }
    public void run() {
        write("Client started.");
        try {
            connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while(!shouldStop) {
            int mailCount = checkMails();
            while(mailCount > 0) {
                getMails(mailCount);
            }
            try {
                wait(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void connect() throws IOException{
        write("Connecting to " + host + ":" + port + ", as " + Username);
        try {
            socket = new Socket(host,port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            write("Connection failed!");
            //e.printStackTrace();
        }
        if (!reader.readLine().startsWith("OK")) {
            write("ERR: Server dont greet");
            return;
        }
        writer.write("USER "+Username);
        if (!reader.readLine().startsWith("OK")) {
            write("ERR: User not OK");
            return;
        }
        writer.write("PASS "+Password);
        if (!reader.readLine().startsWith("OK")) {
            write("ERR: Login not OK");
            return;
        }
    }

    private Integer checkMails() {
        String result = null;
        try {
            writer.write("STAT");
            result = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (result.startsWith("OK")) {
            return Integer.parseInt(result.substring(0, result.indexOf(' ')));
        }

        return 0;
    }

    private void getMails(int numb) {

        String msg = null;
        try {
            writer.write("RETR " + numb);
            msg = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileWriter fstream = new FileWriter("Email" + numb);
            fstream.write(msg);
            fstream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer.write("DELE " + numb);
            if (!reader.readLine().startsWith("OK")) {
                write("ERR: DELE NOT OK");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void write(String text) {
        util.write(text, myColor);
    }

}

