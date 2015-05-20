package server;

import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Boolean running = true;
    private ShutdownInterface shutdownHandler;
    private long lastExecution;

    public RequestHandler(Socket socket, ShutdownInterface shutdownHandler) {
        this.socket = socket;
        this.shutdownHandler = shutdownHandler;
        initialize(socket);
    }

    @Override
    public void run() {
        System.out.println("Thread starts");
        while (running) {
            String clientRequest = getRequestFromClient();
            System.out.println("clientrequest = " + clientRequest);
            if (clientRequest != null) {

                String parsedRequest = parseString(clientRequest);
                System.out.println("Answer for Client : " + parsedRequest);
                sendAnswerToClient(parsedRequest);
                if (!running) {
                    try {
                        System.out.println("Closing");
                        socket.close();
                    } catch (Exception e) {
                       // e.printStackTrace();
                    }
                }
            } else {
                running = false;
            }
        }
        closeAll();
        System.out.println("FIN REQUESTHANDLER");
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
        System.out.println("Client connection established!");
    }

    // Bekommt eine Anfrage vom Client
    public String getRequestFromClient() {
        String clientMessage = null;
        try {
            clientMessage = readMessage();
            System.out.println("Message from Client is: " + clientMessage);
        } catch (IOException e) {
            System.out.println("ERROR getReq");
            //e.printStackTrace();
        }
        return clientMessage;
    }

    // Sendet eine Antwort zum Client zurueck
    public void sendAnswerToClient(String answer) {
        try {
            writer.write(answer + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String parseString(String inputText) {
        String result = null;

        final Pattern REGEX = Pattern
                .compile("(?<KEYWORD>REVERSE|LOWERCASE|UPPERCASE|SHUTDOWN) (?<STRING>[a-zA-Z0-9]+)\n");

        Matcher matcher = REGEX.matcher(inputText);
        if (!matcher.matches()) {
            if (inputText.equals("BYE" + '\n')) {
                result = "OK BYE";
                stop();
            } else {
                result = "ERROR Unknown Command! Server sagt NEIN!";
            }
        } else {
            String keyword = matcher.group("KEYWORD");
            String text = matcher.group("STRING");
            switch (keyword) {
                case "REVERSE":
                    result = "OK " + reverse(text);
                    break;
                case "UPPERCASE":
                    result = "OK " + uppercase(text);
                    break;
                case "LOWERCASE":
                    result = "OK " + lowercase(text);
                    break;
                case "SHUTDOWN":
                    result = shutdown(text);
                    ;
                    break;
            }
        }
        return result;
    }

    private String reverse(String input) {
        return new StringBuilder(input).reverse().toString();
    }

    private String uppercase(String input) {
        return input.toUpperCase();
    }

    private String lowercase(String input) {
        return input.toLowerCase();
    }

    private String readMessage() throws IOException {

        String buffer = "";
        int count = 0;
        int c = 0;

        while (!buffer.endsWith("\n") && ((c = reader.read()) != -1)  ) {
            System.out.println(count + " | " + buffer);
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
        System.out.println(">" + buffer);
        return buffer;
    }

    public void stop() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                long timeout = 30000;
                long timeout2;
                while (( timeout2 = timeout - (System.currentTimeMillis() - lastExecution) ) > 0){
                    System.out.println(timeout);
                    try {
                        Thread.sleep(timeout2);
                    } catch (InterruptedException e) {
                        System.out.println("TESTFEHLER");
                    }
                }
                System.out.println("CloseSocket");
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
            socket.close();
            reader.close();
            writer.close();
        } catch (Exception e) {
            System.out.println("ERROR closeAll");
            e.printStackTrace();
        }

    }
}
