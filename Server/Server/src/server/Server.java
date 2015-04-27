package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paddy-Gaming on 24.04.2015.
 */
public class Server implements Runnable {

    private ServerSocket serverSocket;
    public static Boolean running = true;
    private int serverPort;
    private final int maxClientConnections = 3;
    private int currentClientConnections = 0;
    public static Boolean newClientConnected=false;
    public static Socket newCLient;
    public Boolean waitingForClient=false;

    List<Thread> ClientList = new ArrayList<>();

    public Server(int serverPort) {
        this.serverPort = serverPort;
        try {
            this.serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() {
        Socket socket = null;
        while (running) {


            if (ClientList.size() < maxClientConnections) {
                if (!waitingForClient) {
                    System.out.println("New Listener");
                    new Thread(new SocketListener(serverSocket)).start();
                    waitingForClient=true;
                } else {
                    if (newClientConnected) {
                        newClientConnected=false;
                        waitingForClient=false;

                        socket = newCLient;

                        Thread n = new Thread(new RequestHandler(socket,this));
                        try {
                            socket.setSoTimeout(10000);
                        } catch (SocketException e) {
                            System.out.println("Client Timeout");
                            n.interrupt();
                            try {
                                socket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        ClientList.add(n);
                        n.start();
                        System.out.println("Client connected" + ClientList.size());
                    }
                }
            }
            for (Thread instance : ClientList) {
                if (instance== null || !instance.isAlive()) {
                    /*
                    And believe me I am still alive.
                    I'm doing science and I'm still alive.
                    I feel fantastic and I'm still alive.
                    While you're dying I'll be still alive.
                    And when you're dead I will be, still alive.
                    Still alive, still alive.
                     */
                    ClientList.remove(instance);
                    System.out.println("REMOVED");
                }
            }
        }
        System.out.println("Wait for threads");
        for (Thread instance : ClientList) {
            try {
            instance.join();} catch(Exception e){}
        }
        System.out.println("FIN");
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutDownServer() {
        System.out.println("Shutdown received");
        running = false;
    }

    public void initializeServer() {
        try {
            this.serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            System.out.println("Cannot initialize with serverport.");
            e.printStackTrace();
        }
    }

    public void run() {
        startServer();
    }
}
