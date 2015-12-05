package at.yawk.katbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.io.IOException;
import java.io.InputStream;
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
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        Config config;
        try (InputStream in = Files.newInputStream(Paths.get("config.yml"))) {
            config = yamlMapper.readValue(in, Config.class);
        }

        Module module = binder -> {
            binder.bind(ObjectMapper.class).toInstance(jsonMapper);
            binder.bind(Config.class).toInstance(config);
        };

        Injector injector = Guice.createInjector(module);
        injector.getInstance(Karma.class).loadKarma();

        Config.Server server = config.getServer();
        Client client = Client.builder()
                .nick(config.getNick())
                .serverHost(server.getHost()).serverPort(server.getPort()).secure(server.isSecure())
                .serverPassword(server.getPassword())
                .build();
        client.getEventManager()
                .registerEventListener(injector.getInstance(Karma.class));
    }
}
