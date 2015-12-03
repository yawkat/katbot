package at.yawk.katbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler;

/**
 * @author yawkat
 */
@Slf4j
public class Karma {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern MANIPULATE_PATTERN = Pattern.compile("~([\\w öäü]*)(\\+\\+|--)(:? .*)?",
                                                                      Pattern.CASE_INSENSITIVE);

    private final Path karmaFilePath = Paths.get("karma.json");
    private Holder holder;

    public void loadKarma() throws IOException {
        if (Files.exists(karmaFilePath)) {
            try (InputStream in = Files.newInputStream(karmaFilePath)) {
                holder = OBJECT_MAPPER.readValue(in, Holder.class);
            }
        } else {
            holder = new Holder();
        }
    }

    private void saveKarma() throws IOException {
        try (OutputStream out = Files.newOutputStream(karmaFilePath)) {
            OBJECT_MAPPER.writeValue(out, holder);
        }
    }

    @Handler
    public void onPublicMessage(ChannelMessageEvent event) throws IOException {
        Matcher matcher = MANIPULATE_PATTERN.matcher(event.getMessage());
        if (matcher.matches()) {
            String subject = matcher.group(1).trim();
            int delta = matcher.group(2).equals("++") ? 1 : -1;
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
    }

    @Data
    private static final class Holder {
        private Map<String, Integer> karma = new HashMap<>();
    }
}
