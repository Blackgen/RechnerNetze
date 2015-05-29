package utils;

/**
 * Created by JanDennis on 17.05.2015.
 */
public class EMailAccount {

    private String host;
    private int port;
    private String username;
    private String password;
    private boolean locked = false;

    public EMailAccount() {
    }

    public EMailAccount(String host, int port, String username, String password, boolean isLocked) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.locked = isLocked;
    }

    public EMailAccount(String[] splitted) {
        this.host = splitted[0];
        this.port = Integer.parseInt(splitted[1]);
        this.username = splitted[2];
        this.password = splitted[3];
        this.locked = Boolean.parseBoolean(splitted[4]);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean isLocked) {
        this.locked = isLocked;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


}