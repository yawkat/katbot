package at.yawk.katbot;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
class ForumListener {
    @Inject ScheduledExecutorService executor;
    @Inject Config config;
    // lazy init
    @Inject Provider<UrlShortener> urlShortener;
    @Inject Client client;

    private final Map<URI, ForumHolder> forums = new HashMap<>();

    public void start() {
        executor.scheduleWithFixedDelay(() -> {
            try {
                pollAll();
            } catch (Exception e) {
                log.error("Failed to poll forums", e);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private synchronized void pollAll() throws IOException {
        for (Map.Entry<URI, ForumConfiguration> entry : config.getForums().entrySet()) {
            forums.computeIfAbsent(entry.getKey(), k -> new ForumHolder(k, entry.getValue())).poll();
        }
    }

    static void sendToChannels(Client client, List<String> channels, String message) {
        for (String channelName : channels) {
            Optional<Channel> channelOptional = client.getChannel(channelName);
            if (!channelOptional.isPresent()) {
                log.warn("Could not find channel {}", channelName);
                continue;
            }
            channelOptional.get().sendMessage(message);
        }
    }

    @RequiredArgsConstructor
    private class ForumHolder {
        private final URI uri;
        private final ForumConfiguration configuration;

        private final Set<Integer> sentThreadIds = new HashSet<>();
        private boolean firstPass = true;

        private void poll() throws IOException {
            assert Thread.holdsLock(ForumListener.this);

            List<ThreadInfo> threads = getThreads();
            for (ThreadInfo thread : threads) {
                boolean added = sentThreadIds.add(thread.getId());
                if (thread.replyCount <= 0 && added && !firstPass) {
                    String message = Template.parse(configuration.messagePattern)
                            .set("title", thread.getTitle())
                            .set("author", thread.getAuthor())
                            .set("uri", thread.getUri().toString())
                            .set("uri.short", () -> {
                                try {
                                    return urlShortener.get().shorten(thread.getUri()).toString();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .finish();

                    log.info("Sending forum update '{}' to {} channels", message, configuration.channels.size());

                    sendToChannels(client, configuration.channels, message);
                }
            }
            firstPass = false;
        }

        private List<ThreadInfo> getThreads() throws IOException {
            List<ThreadInfo> threads = new ArrayList<>();
            Document document = Jsoup.connect(uri.toString()).get();
            for (Element element : document.select(".all_threads_container .thread_content")) {
                if (element.hasClass("thread_separator")) { continue; }

                Element titleTag = element.select(".name > a").first();
                String replyCountText = element.select(".replycount").text().replaceAll("[^\\d]", "");
                String href = titleTag.attr("href");
                threads.add(new ThreadInfo(
                        Integer.parseInt(href.substring(href.indexOf('/') + 1, href.indexOf('-'))),
                        URI.create(titleTag.absUrl("href")),
                        titleTag.text(),
                        replyCountText.isEmpty() ? 0 : Integer.parseInt(replyCountText),
                        element.select(".topicstart > a").text()
                ));
            }
            return threads;
        }
    }

    @Value
    private static class ThreadInfo {
        private final int id;
        private final URI uri;
        private final String title;
        private final int replyCount;
        private final String author;
    }

    @Value
    public static class ForumConfiguration {
        private final List<String> channels;
        private final String messagePattern;
    }
}
