package server;

import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Paddy-Gaming on 24.04.2015.
 */
public class RequestHandler implements Runnable {
    Server Mother;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Boolean shouldClose = false;

    public RequestHandler(Socket socket, Server mother) {
        this.socket = socket;
        this.Mother = mother;
        initialize(socket);
    }

    @Override
    public void run() {
        System.out.println("Thread starts");
        while (!shouldClose) {
            String clientRequest = getRequestFromClient();
            System.out.println("clientrequest = " + clientRequest);
            if (clientRequest != null) {
                String parsedRequest = parseString(clientRequest);
                System.out.println("Answer for Client : " + parsedRequest);
                sendAnswerToClient(parsedRequest);
                if (shouldClose) {
                    try {
                        System.out.println("Closing");
                        socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        closeAll();
        this.stop();
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
            e.printStackTrace();
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

        while (((c = reader.read()) != -1) && !shouldClose) {
            System.out.println(count + " | " + buffer);
            buffer += (char) c;
            count += 1;
            if (count >= 255) {
                // Nachricht zu lang
                sendAnswerToClient("Error Message too long!");
                buffer = null;

                //Buffer leeren:
                while (((c = reader.read()) != -1) && (((char) c) != '\n') && !shouldClose) {

                }
                buffer = "";
                break;

            } else if ((char) c == '\n') {
                break;
            }

        }
        System.out.println(">" + buffer);
        return buffer;
    }

    public void stop() {
        shouldClose = true;
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

        stop();
        Mother.shutDownServer();
        return result;
    }

    public void closeAll() {
        try {
            socket.close();
            reader.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
