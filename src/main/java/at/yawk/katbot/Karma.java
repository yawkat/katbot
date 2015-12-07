package at.yawk.katbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class Karma {
    private static final String NAME_PATTERN = "([\\w\\-` öäü]*)";

    private static final Pattern MANIPULATE_PATTERN =
            Pattern.compile('~' + NAME_PATTERN + "(\\+\\+|--)(:? .*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern VIEW_PATTERN =
            Pattern.compile('~' + NAME_PATTERN + "(:? .*)?", Pattern.CASE_INSENSITIVE);

    @Inject Client client;
    @Inject ObjectMapper objectMapper;

    private static final Clock CLOCK = Clock.systemUTC();

    private final Path karmaFilePath = Paths.get("karma.json");
    private Holder holder;

    private final Map<String, MessageThrottle> userThrottles = Collections.synchronizedMap(new HashMap<>());

    public void start() throws IOException {
        loadKarma();
        client.getEventManager().registerEventListener(this);
    }

    private void loadKarma() throws IOException {
        if (Files.exists(karmaFilePath)) {
            try (InputStream in = Files.newInputStream(karmaFilePath)) {
                holder = objectMapper.readValue(in, Holder.class);
            }
        } else {
            holder = new Holder();
        }
    }

    private void saveKarma() throws IOException {
        try (OutputStream out = Files.newOutputStream(karmaFilePath)) {
            objectMapper.writeValue(out, holder);
        }
    }

    @Handler
    public void onPublicMessage(ChannelMessageEvent event) throws IOException {
        Matcher manipulateMatcher = MANIPULATE_PATTERN.matcher(event.getMessage());
        if (manipulateMatcher.matches()) {
            String subject = manipulateMatcher.group(1).trim();
            if (!subject.isEmpty()) {
                MessageThrottle throttle = userThrottles
                        .computeIfAbsent(event.getActor().getNick(), s -> new MessageThrottle(CLOCK));

                if (!throttle.trySend()) { return; }

                int delta = manipulateMatcher.group(2).equals("++") ? 1 : -1;
                int newValue = holder.getKarma()
                        .compute(subject, (k, v) -> (v == null ? 0 : v) + delta);
                log.info("{} has changed the karma level for {} by {} to {}",
                         event.getActor().getNick(),
                         subject,
                         delta,
                         newValue);
                event.getChannel().sendMessage(
                        subject + " has a karma level of " + newValue + ", " + event.getActor().getNick());

                saveKarma();
            }
        } else {
            Matcher viewMatcher = VIEW_PATTERN.matcher(event.getMessage());
            if (viewMatcher.matches()) {
                String subject = viewMatcher.group(1).trim();
                if (!subject.isEmpty()) {
                    int value = holder.getKarma().getOrDefault(subject, 0);
                    event.getChannel().sendMessage(
                            subject + " has a karma level of " + value + ", " + event.getActor().getNick());
                }
            }
        }
    }

    @Data
    private static final class Holder {
        private Map<String, Integer> karma = new HashMap<>();
    }
}
