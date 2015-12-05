package at.yawk.katbot;

import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class Config {
    private final String nick;
    private final Server server;

    @Value
    public static class Server {
        private final String host;
        private final int port;
        private final boolean secure;
        private final String password;
    }
}
