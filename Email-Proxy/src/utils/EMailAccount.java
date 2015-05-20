package utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by JanDennis on 17.05.2015.
 */
public class EMailAccount {

    private String host;
    private int port;
    private String username;
    private String password;
    private boolean ssl = false;


    public EMailAccount(String host, int port, String username, String password, boolean useSSL) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.ssl = useSSL;
    }

    public EMailAccount(String [] splitted) {
        this.host = splitted[0];
        this.port = Integer.parseInt(splitted[1]);
        this.username = splitted[2];
        this.password = splitted[3];
        this.ssl = Boolean.parseBoolean(splitted[4]);
    }

    public boolean isSsl() {
        return this.ssl;
    }

    public void setSsl(boolean useSSL) {
        this.ssl = useSSL;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }


}