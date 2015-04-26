/**
 * Created by Paddy-Gaming on 26.04.2015.
 */
import server.Server;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JanDennis on 26.04.2015.
 */
public class Main {
    private static String host = "localhost";
    private static int port = 1337;
    private static int repeat = 1;
    private static Boolean auto = false;
    private static List<String> commands = new ArrayList<String>();

    public static void main(String[] args) {
        try {
            parseParamater(args);
        } catch (Exception e) {
            System.out.println("Error IlligalArgument");
        }

        Server server = new Server(port);
        server.run();
    }

    private static void showHelp() {
        System.out.println("Create a Client to connect to a simple server");
        System.out.println("Parameter:");
        System.out.println("-h | -host <String>:    add Hostname (Default localhost)");
        System.out.println("-p | -port <int>:       add Port (Default 1337)");
        System.out.println("-a | -auto:             enable Automode (Default off)");
        System.out.println("                        Client will perform a number of Commands");
        System.out.println("-r | -repeat <int>:     repeat commands (Default 1)");
        System.out.println("                        Will be ignored if automode is on");
        System.out.println("-c | -command <string>: add a Command (Default none)");
        System.out.println("                        can be added multiple times");
        System.out.println("                        Will be ignored if automode is on");
    }

    private static void parseParamater(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "-host":
                    host = args[++i];
                    break;
                case "-p":
                case "-port":
                    port = Integer.valueOf(args[++i]);
                    break;
                case "-a":
                case "-auto":
                    auto = true;
                    break;
                case "-c":
                case "-command":
                    commands.add(args[++i]);
                    break;
                case "/?":
                case "--help":
                    showHelp();
                    break;
                case "-r":
                case "-repeat":
                    repeat = Integer.valueOf(args[++i]);
                    break;
                default:
                    showCreationError();
                    System.exit(-1);
                    break;
            }
        }
    }

    private static void showCreationError() {
        System.out.println("Error unknown Parameter!");
    }
}
