package client;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paddy-Gaming on 24.04.2015.
 */
public class Client implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private int reapeating = 1;
    private List<String> commandList;
    private Boolean autoMode;
    private Boolean manualMode = false;

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private String Host;
    private int Port;

    public Client(String host, int port, int repeat, Boolean auto, List<String> commands) {
        this.reapeating = repeat;
        this.autoMode = auto;
        this.commandList = commands;
        this.Host = host;
        this.Port = port;
        manualMode = (!autoMode && commandList.isEmpty());

        if (autoMode) {
            commandList = new ArrayList<>();
            commandList.add("REVERSE halloWELT");
            commandList.add("UPPERCASE halloWELT");
            commandList.add("LOWERCASE halloWELT");
            commandList.add("BYE");
        } else if (manualMode) {
            loginManually();
        }
    }

    private void initializeClient(String host, int port) {
        try {
            this.socket = new Socket(host, port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToServer(String cmd) {
        try {

            writer.write(cmd + '\n');
            System.out.println("Write to Server -> " + cmd);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(readServerOutput());
    }

    @Override
    public void run() {
        while (reapeating >= 1 || manualMode) {
            if (!manualMode) {
                initializeClient(Host, Port);
                // Found Commands!
                for (int i = 0; i < commandList.size(); i++) {
                    String current = commandList.get(i);
                    writeToServer(current);
                    if (i == commandList.size() - 1 && !(current == "BYE" || current.startsWith("SHUTDOWN"))) {
                        // Bye wasn't set, so I will do it for you ;)
                        writeToServer("BYE");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Manuelle Eingabe
                String input = null;

                System.out.println("Client:> ");
                try {
                    input = br.readLine().toString();
                    System.out.println(">" + input);
                } catch (IOException e) {
                    //gotcha!
                    e.printStackTrace();
                }
                if (input.toLowerCase().equals("exit")) break;
                else if (input.equals("connect")) loginManually();
                else writeToServer(input);
            }
            reapeating -= 1;
        }
    }

    private String readServerOutput() {
        String Result = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            Result = in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result;
    }

    private void loginManually() {
        String host = null;
        int port = 0;
        try {
            // Lese connect details
            System.out.println("Connect to Server");
            System.out.print("Host: ");
            host = br.readLine();

            System.out.print("Port: ");
            port = Integer.valueOf(br.readLine());

        } catch (Exception e) {
            e.printStackTrace();
        }
        initializeClient(host, port);

    }
}
