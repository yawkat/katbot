package at.yawk.katbot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.kitteh.irc.client.library.Client;

/**
 * @author yawkat
 */
@UtilityClass
@Slf4j
public class Main {
    public static void main(String[] args) throws IOException {
        Karma karma = new Karma();
        karma.loadKarma();

        Client client = Client.builder()
                .nick("katbot")
                .serverHost("ps.yawk.at").serverPort(5000).secure(false)
                .serverPassword(new String(Files.readAllBytes(Paths.get("password")), StandardCharsets.UTF_8).trim())
                .build();
        client.getEventManager().registerEventListener(karma);
    }
}
