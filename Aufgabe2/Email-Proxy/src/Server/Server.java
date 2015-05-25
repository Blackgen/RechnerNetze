package Server;

import utils.EMailAccount;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Paddy-Gaming on 24.04.2015.
 */
public class Server implements Runnable {

    public static Boolean newClientConnected = false;
    public static Socket newCLient;
    public static List<EMailAccount> MailAccounts = new ArrayList();
    public static ServerSocket serverSocket;
    private final int maxClientConnections = 3;
    public Boolean running = true;
    public Boolean isWaitingForClient = false;
    List<RequestHandler> ClientList = null;
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

        for (EMailAccount acc : MailAccounts) {
            System.out.println(acc.getUsername());
        }

        while (running) {

            if (!isWaitingForClient) {
                System.out.println("Start Listener");
                new SocketListener().run();
                isWaitingForClient = true;
            } else if (newClientConnected) {
                System.out.println("[Server] New Client!");
                if (ClientList.size() < maxClientConnections) {
                    System.out.println("[Server] Start Handler");
                    RequestHandler requestHandler = new RequestHandler(newCLient, shutdownHandler);
                    Thread n = new Thread(requestHandler);
                    ClientList.add(requestHandler);
                    n.start();
                } else {
                    try {
                        System.out.println("[Server] No free Slots, closing");
                        newCLient.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                isWaitingForClient = false;
            }
            // Check if Clients are still alive, still alive, still alive..

//            Iterator<RequestHandler> iter = ClientList.iterator();
//
//            while(iter.hasNext()) {
//                RequestHandler requestHandler = iter.next();
//                if (!instance.isRunning()) {
//                    ClientList.remove(instance);
//                    System.out.println("REMOVED CLIENT");
//                }
//            }

            if (ClientList.size() > 0) {
                for (RequestHandler instance : ClientList) {
                    if (!instance.isRunning()) {
                        ClientList.remove(instance);
                        System.out.println("REMOVED CLIENT");
                    }
                }
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

    private void loadAccounts() throws IOException {
        try {
            BufferedReader in = new BufferedReader(new FileReader(AccountFile));
            String c = in.readLine();
            while (c != null) {
                System.out.println(".> " + c);
                String[] res = c.split("\\|");
                System.out.println(res[0]);
                EMailAccount acc = new EMailAccount();
                acc.setUsername(res[0]);
                acc.setPassword(res[1]);
                MailAccounts.add(acc);
                c = in.readLine();
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}