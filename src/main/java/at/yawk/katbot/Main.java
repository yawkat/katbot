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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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

        Client client = connect(config);

        Module module = binder -> {
            binder.bind(ObjectMapper.class).toInstance(jsonMapper);
            binder.bind(Config.class).toInstance(config);
            binder.bind(Client.class).toInstance(client);
            binder.bind(ScheduledExecutorService.class).toInstance(Executors.newSingleThreadScheduledExecutor());
            binder.bind(HttpClient.class).toInstance(HttpClientBuilder.create().build());
        };

        Injector injector = Guice.createInjector(module);

        injector.getInstance(Karma.class).start();
        injector.getInstance(RssFeedListener.class).start();
    }

    private static Client connect(Config config) {
        Config.Server server = config.getServer();
        return Client.builder()
                .nick(config.getNick())
                .serverHost(server.getHost()).serverPort(server.getPort()).secure(server.isSecure())
                .serverPassword(server.getPassword())
                .build();
    }
}
