package at.yawk.katbot;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.xml.sax.InputSource;

/**
 * @author yawkat
 */
@Singleton
@Slf4j
public class RssFeedListener {
    @Inject Client client;
    @Inject ScheduledExecutorService executor;
    @Inject Config config;
    @Inject HttpClient httpClient;
    // lazy init
    @Inject Provider<UrlShortener> urlShortener;

    private final SyndFeedInput input = new SyndFeedInput();

    private final Map<URI, Instant> lastPollTimes = new HashMap<>();

    private SyndFeed loadFeed(URI uri) throws IOException, FeedException {
        HttpResponse response = httpClient.execute(new HttpGet(uri));
        try (InputStream in = response.getEntity().getContent()) {
            return input.build(new InputSource(in));
        }
    }

    private synchronized void poll() throws Exception {
        for (Map.Entry<URI, FeedConfiguration> conf : config.getFeeds().entrySet()) {
            URI uri = conf.getKey();

            SyndFeed feed = loadFeed(uri);
            Instant deadline = lastPollTimes.get(uri);
            Instant newDeadline = null;
            for (SyndEntry feedEntry : feed.getEntries()) {
                Instant entryTime = feedEntry.getPublishedDate().toInstant();
                if (newDeadline == null || entryTime.isAfter(newDeadline)) {
                    newDeadline = entryTime;
                }
                if (deadline != null && entryTime.isAfter(deadline)) {
                    fire(conf.getValue(), feedEntry);
                }
            }
            lastPollTimes.put(uri, newDeadline);
        }
    }

    private void fire(FeedConfiguration conf, SyndEntry feedEntry) {
        Map<String, Supplier<String>> parameters = new HashMap<>();
        parameters.put("title", feedEntry::getTitle);
        parameters.put("uri", feedEntry::getUri);
        parameters.put("uri.short", () -> {
            try {
                return urlShortener.get().shorten(URI.create(feedEntry.getUri())).toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        String message = conf.messagePattern;
        for (Map.Entry<String, Supplier<String>> parameter : parameters.entrySet()) {
            String substitution = "${" + parameter.getKey() + '}';
            if (message.contains(substitution)) {
                message = message.replace(substitution, parameter.getValue().get());
            }
        }

        log.info("Sending feed update '{}' to {} channels", message, conf.channels.size());

        for (String channelName : conf.channels) {
            Optional<Channel> channelOptional = client.getChannel(channelName);
            if (!channelOptional.isPresent()) {
                log.warn("Could not find channel {} for feed", channelName);
                continue;
            }
            channelOptional.get().sendMessage(message);
        }
    }

    public void start() {
        executor.scheduleWithFixedDelay(() -> {
            try {
                poll();
            } catch (Exception e) {
                log.error("Failed to poll RSS feeds", e);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Value
    public static class FeedConfiguration {
        private final List<String> channels;
        private final String messagePattern;
    }
}
