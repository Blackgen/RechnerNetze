package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Paddy-Gaming on 24.04.2015.
 */
public class Server implements Runnable{

    private ServerSocket serverSocket;
    private int serverPort;
    private final int maxClientConnections = 3;
    private int currentClientConnections = 0;

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
        while(true) {


            if(currentClientConnections <= maxClientConnections) {
                try {
                    socket = serverSocket.accept();
                    new Thread(new RequestHandler(socket)).start();
                    System.out.println("Client connected");
                    currentClientConnections++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Maximale Anzahl an Clients erreicht!");
            }
        }
    }

    public void shutDownServer() {

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
