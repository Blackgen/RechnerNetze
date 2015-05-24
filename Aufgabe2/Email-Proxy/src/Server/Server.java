package Server;

import utils.EMailAccount;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Paddy-Gaming on 24.04.2015.
 */
public class Server implements Runnable {

    public static Boolean newClientConnected = false;
    public static Socket newCLient;
    public static List<EMailAccount> MailAccounts = new ArrayList();
    private final int maxClientConnections = 3;
    public Boolean running = true;
    List<RequestHandler> ClientList = null;
    private ServerSocket serverSocket;
    private int serverPort;
    private ShutdownInterface shutdownHandler;
    private String AccountFile = "Accounts.txt";

    public Server(int serverPort, List<RequestHandler> requestHandlerList, ShutdownInterface shutdownhandler) {
        this.serverPort = serverPort;
        this.ClientList = requestHandlerList;
        this.shutdownHandler = shutdownhandler;
        try {
            this.serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() {
        // Loading up all Accounts
        try {
            loadAccounts();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("ACCOUNTS: ");

        for (EMailAccount acc :MailAccounts) {
            System.out.println(acc.getUsername());
        }

        while (running) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                if (ClientList.size() < maxClientConnections) {
                    RequestHandler requestHandler = new RequestHandler(socket, shutdownHandler);
                    Thread n = new Thread(requestHandler);
                    ClientList.add(requestHandler);
                    n.start();
                }


                // Check if Clients are still alive, still alive, still alive..
                if (ClientList.size() > 0) {
                    for (RequestHandler instance : ClientList) {
                        if (!instance.isRunning()) {
                            ClientList.remove(instance);
                            System.out.println("REMOVED CLIENT");
                        }
                    }
                }

            } catch (IOException e) {
                // e.printStackTrace();
            }
        }
        System.out.println("Finish");
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void shutDownServer() {
        System.out.println("Shutdown received");
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        startServer();
    }

    private void loadAccounts() throws IOException{
        try {
            BufferedReader in = new BufferedReader(new FileReader(AccountFile));
            String c = in.readLine();
            while (c!=null) {
                System.out.println(".> "+c);
                String[] res = c.split("\\|");
                System.out.println(res[0]);
                EMailAccount acc = new EMailAccount();
                acc.setUsername(res[0]);
                acc.setPassword(res[1]);
                MailAccounts.add(acc);
                c= in.readLine();
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}