package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paddy-Gaming on 24.04.2015.
 */
public class Server implements Runnable{

    private ServerSocket serverSocket;
    private int serverPort;
    private final int maxClientConnections = 3;
    private int currentClientConnections = 0;

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
        while(true) {


            if(ClientList.size() < maxClientConnections) {
                try {
                    socket = serverSocket.accept();
                    Thread n =new Thread(new RequestHandler(socket));
                    ClientList.add(n);
                    n.start();
                    System.out.println("Client connected" +ClientList.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for(Thread instance: ClientList) {
                if (!instance.isAlive()) {
                    ClientList.remove(instance);
                    System.out.println("REMOVED");
                }
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
