package Server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by JanDennis on 27.04.2015.
 */
public class SocketListener implements Runnable {
    ServerSocket mother;

    public SocketListener() {
    }

    @Override
    public void run() {
        try {
            Socket Client = Server.serverSocket.accept();
            System.out.println("[Listener] New Client!");
            Server.newCLient = Client;
            Server.newClientConnected = true;
        } catch (Exception e) {
        }
    }
}
