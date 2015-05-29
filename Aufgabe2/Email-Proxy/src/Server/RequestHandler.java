package Server;

import utils.EMailAccount;
import utils.util;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandler implements Runnable {
    private String myColor = "\u001B[30m";

    public static final String REGEX = "(?<KEYWORD>USER|PASS|STAT|LIST|RETR|DELE|NOOP|RSET|QUIT)( )*( (?<STRING>[\\x21-\\x7F]+))?(\r)?\n";
    private final String MailDropPath = System.getProperty("user.dir") + File.separator+"Mails"+File.separator;
    String State = "UserAuth";
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Boolean running = true;
    private ShutdownInterface shutdownHandler;
    private long lastExecution;
    private String User = null;
    private EMailAccount Account;
    private List<Integer> deleteList = new ArrayList<>();

    public RequestHandler(Socket socket, ShutdownInterface shutdownHandler, int i) {
        myColor=util.colors[i];
        this.socket = socket;
        this.shutdownHandler = shutdownHandler;
        initialize(socket);

    }

    @Override
    public void run() {
        write("Thread starts");
        while (running) {
            String clientRequest = getRequestFromClient();
            write("clientrequest = " + clientRequest);
            if (clientRequest != null) {

                String parsedRequest = parseString(clientRequest);
                write("Answer for Client : " + parsedRequest);
                sendAnswerToClient(parsedRequest);
                if (!running) {
                    try {
                        write("Closing");
                        closeAll();
//                        socket.close();
                    } catch (Exception e) {
                        // e.printStackTrace();
                    }
                }
            } else {
                running = false;
            }
        }
        closeAll();
        write("FIN REQUESTHANDLER");
    }

    public boolean isRunning() {
        return running;
    }

    private void initialize(Socket socket) {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendAnswerToClient("+OK POP3 Server H4xme-0.1 ready!");
        write("Client connection established!");
    }

    // Bekommt eine Anfrage vom Client
    public String getRequestFromClient() {
        String clientMessage = null;
        try {
            clientMessage = readMessage();
            write("Message from Client is: " + clientMessage);
        } catch (IOException e) {
            write("ERROR getReq");
            //e.printStackTrace();
        }
        return clientMessage;
    }

    // Sendet eine Antwort zum Client zurueck
    public void sendAnswerToClient(String answer) {
        try {
            write("Send: " + answer);
            writer.write(answer + "\r\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String parseString(String inputText) {
        String result = null;

        // REGEX Keyword + Any Ascii > 31 (Newline & Stuff)
        final Pattern REGEX = Pattern
                .compile(RequestHandler.REGEX);

        Matcher matcher = REGEX.matcher(inputText);

        if (!matcher.matches()) return "-ERR No Match";

        String keyword = matcher.group("KEYWORD");
        String text = matcher.group("STRING");
        switch (keyword) {
            case "USER":
                if (text == null || text.isEmpty()) {
                    result = "-ERR User can't be empty!";
                } else {
                    result = authUser(text);
                }
                break;
            case "PASS":

                result = authPass(text);
                break;
            case "STAT":

                result = getStatus();
                break;
            case "LIST":
                result = listEmails(text);
                break;
            case "RETR":
                result = retrieveMails(text);
                break;
            case "DELE":
                result = markForDeletion(text);
                break;
            case "NOOP":
                result = "+OK";
                break;
            case "RSET":
                deleteList.clear();
                result = "+OK";
                break;
            case "QUIT":
                if (!State.equals("Transaction")) result="-ERR Authentificate first";
                else {
                result = deleteMails();
                Account.setLocked(false);
                running = false;}
                break;
        }

        return result;
    }


    private String readMessage() throws IOException {

        String buffer = "";
        int count = 0;
        int c = 0;

        while (!buffer.endsWith("\n") && ((c = reader.read()) != -1)) {
            //write(count + " | " + buffer);
            buffer += (char) c;
            count += 1;

            if (count >= 255) {
                // Nachricht zu lang
                sendAnswerToClient("Error Message too long!");
                buffer = null;

                //Buffer leeren:
                while (((c = reader.read()) != -1) && (((char) c) != '\n')) {

                }

                buffer = "";
                //break;

            } else if ((char) c == '\n') {
                lastExecution = System.currentTimeMillis();
                //break;
            }

        }
        write(">" + buffer);
        return buffer;
    }

    public void stop() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                long timeout = 30000;
                long timeout2;
                while ((timeout2 = timeout - (System.currentTimeMillis() - lastExecution)) > 0) {
                    write(""+timeout);
                    try {
                        Thread.sleep(timeout2);
                    } catch (InterruptedException e) {
                        write("TESTFEHLER");
                    }
                }
                write("CloseSocket");
                running = false;
                try {
                    socket.close();
                } catch (IOException e) {
                    //
                }
            }
        }).start();
    }

    private String shutdown(String password) {
        String result = "";

        if (password.equals("dasspielistausman")) {
            result = "OK SHUTDOWN";
        } else if (password.equals("")) {
            result = "ERROR password cannot be empty!";
        } else {
            result = "ERROR this password is not correct!";
        }

        shutdownHandler.shutdown();
        running = false;

        return result;
    }

    public void closeAll() {
        try {
            socket.getOutputStream().close();
            socket.close();
            reader.close();
            writer.close();
        } catch (Exception e) {
            write("ERROR closeAll");
            e.printStackTrace();
        }

    }

    private String authUser(String Username) {
        String result = null;
        if (!State.equals("UserAuth")) return "-ERR Not in right state";
        if (Server.MailAccounts.stream().anyMatch(e -> e.getUsername().equals(Username))) {
            // Username vorhanden
            result = "+OK " + Username + " is a valid Account";
            User = Username;
            State = "PassAuth";
        } else {
            result = "-ERR " + Username + " is not a known account!";
        }
        return result;
    }

    private String authPass(String Password) {
        String result = null;
        if (Password == null || Password.isEmpty()) return "-ERR Pass can't be empty!";
        if (!State.equals("PassAuth")) return "-Err Not in right State!";

        //EMailAccount a = Server.MailAccounts.stream().filter(e -> e.getUsername().equals(User)).findAny().get();
        EMailAccount a = null;
        for (EMailAccount acc : Server.MailAccounts) {
            write(acc.getUsername() + " + " + acc.getPassword());
            if (acc.getUsername().equals(User)) {
                write("FOUND USER");
                a = acc;
                break;
            }
        }
        if ((a != null) && a.getPassword().equals(Password)) {
            if (a.isLocked()) {
                result = "-ERR " + User + " is already Locked!";
                State = "Begin";
            } else {
                result = "+OK";
                Account = a;
                State = "Transaction";
            }

        } else {
            write("P: " + a.getPassword());
            result = "-ERR Password not valid";
        }
        return result;
    }

    private String getStatus() {
        String result = null;

        if (!State.equals("Transaction")) return "-ERR not Authentificated";

        result = "+OK " + getMailCountAndSize();

        return result;
    }

    private String getMailCountAndSize() {
        write(System.getProperty("user.dir") + File.separator+"Mails"+File.separator);
        File folder = new File(System.getProperty("user.dir") + File.separator+"Mails"+File.separator);
        if (folder.listFiles() == null) return "0 0";
        List<File> listOfFiles = new ArrayList<File>(Arrays.asList(folder.listFiles()));

        int count = 0;
        long size = 0;

        for (File F : listOfFiles) {
            if (F.getName().startsWith(User)) {
                count++;
                size += F.length();
            }
        }
        return count + " " + size;
    }

    private String listEmails(String input) {
        String result = null;
        if (!State.equals("Transaction")) return "-ERR Authentificate first!";

        if (input == null || input.isEmpty()) {
            File folder = new File(MailDropPath);
            if (folder.listFiles() == null) return "+OK 0";
            List<File> listOfFiles = new ArrayList<File>(Arrays.asList(folder.listFiles()));

            int count = 0;
            long size = 0;
            List<String> resultSet = new ArrayList<>();

            for (File F : listOfFiles) {
                if (F.getName().startsWith(User)) {
                    String current = F.getName().substring(User.length()).replace(".txt", "");
                    current += " " + F.length();
                    resultSet.add(current);
                }
            }

            sendAnswerToClient("+OK " + resultSet.size());

            for (String line : resultSet) {
                sendAnswerToClient(line);
            }

            result = ".";
        } else {
            if (Integer.parseInt(input)<0) return "-ERR ID must be positive!";
            File wanted = new File(MailDropPath+User + input + ".txt");
            if (!wanted.exists()) result = "-ERR Mail not found!";
            else result = input + " " + wanted.length();
        }

        return result;
    }

    private String retrieveMails(String input) {
        String result = null;

        if (!State.equals("Transaction")) return "-ERR Wrong State!";
        if (input.isEmpty()) return "-ERR Please specify an ID!";
        if (Integer.parseInt(input)<0) return "-ERR ID must be positive!";

        File wanted = new File(MailDropPath + User + input + ".txt");
        write(">RETR " + MailDropPath + User + input + ".txt");

        if (wanted.exists()) {
            sendAnswerToClient("+OK " + wanted.length());

            try {
                Scanner in = new Scanner(new FileReader(wanted));
                while (in.hasNextLine()) {
                    sendAnswerToClient(in.nextLine());
                }
                in.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            result = ".";
        } else {
            result = "-ERR No such Message!";
        }

        return result;
    }

    private String markForDeletion(String input) {
        String result;
        if (!State.equals("Transaction")) return "-ERR Wrong State";
        if (input.isEmpty()) return "-ERR please select a messageid";
        if (Integer.parseInt(input)<0) return "-ERR ID must be positive!";

        int i = Integer.parseInt(input);

        File wanted = new File(MailDropPath + User + i + ".txt");
        if (wanted.exists()) {
            deleteList.add(i);
            result = "+OK " + i + " is marked to be deleted!";
        } else {
            result = "-ERR " + i + " is not a known messageid";
        }
        return result;
    }

    private String deleteMails() {
        String result = null;
        if (!State.equals("Transaction")) return "-Err Wrong State";
        Boolean r = true;


        for (int f : deleteList) {

            File w = new File(MailDropPath + User + f + ".txt");
            r &= w.delete();
            write("Delete " + MailDropPath + User + f + ".txt " + r);
        }
        result = r ? "+OK " : "-ERR Delete not sucessful!";
        return result;
    }

    private void write(String text) {
        util.write(text, myColor);
    }
}