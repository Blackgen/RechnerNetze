package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paddy-Gaming on 24.04.2015.
 */
public class Server implements Runnable {

    private ServerSocket serverSocket;
    public Boolean running = true;
    private int serverPort;
    private final int maxClientConnections = 3;
    public static Boolean newClientConnected = false;
    public static Socket newCLient;
    private ShutdownInterface shutdownHandler;

    List<RequestHandler> ClientList = null;

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
