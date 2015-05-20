package Server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by JanDennis on 27.04.2015.
 */
public class SocketListener implements Runnable{
    ServerSocket mother;
    public SocketListener(ServerSocket s) {
        mother=s;
    }
    @Override
    public void run() {
        try{
        Socket Client = mother.accept();
            Server.newCLient=Client;
        Server.newClientConnected=true;}
        catch (Exception e) {}
    }
}
