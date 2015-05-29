package Server;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by patrick_steinhauer on 13.05.2015.
 */
public class ServerManager {

    private Server server = null;
    private List<RequestHandler> listWithRequestHandler = new ArrayList<>();
    private ShutdownInterface shutdownHandler;

    public ServerManager(int serverport) {

        shutdownHandler = new ShutdownInterface() {
            @Override
            public void shutdown() {
                server.shutDownServer();
                for (RequestHandler req : listWithRequestHandler) {
                    System.out.println("StopReq");
                    req.stop();
                }
            }
        };

        this.server = new Server(serverport, listWithRequestHandler, shutdownHandler);
        new Thread(server).start();
    }
}
