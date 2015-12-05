package at.yawk.katbot;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class Config {
    private final String nick;
    private final Server server;
    private final Map<URI, RssFeedListener.FeedConfiguration> feeds;
    private final at.yawk.paste.client.Config paste;

    @Value
    public static class Server {
        private final String host;
        private final int port;
        private final boolean secure;
        private final String password;
    }
}
