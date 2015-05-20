import utils.util;
import Client.Client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by JanDennis on 13.05.2015.
 */
public class Main {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private String myColor = "\u001B[32m"; // GREEN
    private List<Client> Clientlist =new ArrayList<>();

    public static void main(String[] args) {

    }

    private void startConsole() {
        write("Email Proxy");
        write("===========\n");
        write("What would you like to set up?");
        write("server / client / both");
        String result = util.readUserInput();

        switch (result) {
            case "server":
            case "s":
                write("I will act as a Server!");
                result="s";
                break;
            case "client":
            case "c":
                write("I'll create a Client!");
                result="c";
                break;
            case "both":
            case "b":
            case "":
                write("I'll set up both!");
                result="b";
                break;
            default:
                write("Couln't parse your Input! Try again.");
                break;
        }
        if(result.equals(null)) System.exit(0);

        if (result.equals("c") || result.equals("b")) {
            write("How many Clients should I set up? ");
            int count= Integer.parseInt(util.readUserInput());
            for (int i = 0; i < count; i++) {
                write("\n"+i+". Client: ");
                setupClient();
            }
        }

        if (result.equals("s")||result.equals("b")) {
            write("Setting up Server.");
            setupServer();
        }
    }

    private void write(String text) {
        util.write(text, myColor);
    }
    private void setupServer() {

    }
    private void setupClient(){
        String host,user,pass;
        int port;
        write("Hostname: ");
        host=util.readUserInput();
        write("Port: ");
        port=Integer.parseInt(util.readUserInput());
        write("Username: ");
        user=util.readUserInput();
        write("Passwort: ");
        pass=util.readUserInput();
        Client c = new Client(host,port,user,pass,"\u001B[36m");
        Clientlist.add(c);
    }
}
