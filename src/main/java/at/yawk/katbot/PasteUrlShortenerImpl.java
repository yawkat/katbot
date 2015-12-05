package at.yawk.katbot;

import at.yawk.paste.client.PasteClient;
import at.yawk.paste.model.URLPasteData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Singleton
class PasteUrlShortenerImpl implements UrlShortener {
    private final PasteClient pasteClient;

    @Inject
    public PasteUrlShortenerImpl(Config config, ObjectMapper objectMapper) {
        pasteClient = new PasteClient(config.getPaste(), objectMapper);
    }

    @Override
    public URI shorten(URI uri) throws Exception {
        URLPasteData pasteData = new URLPasteData();
        pasteData.setUrl(uri.toURL());
        return new URI(pasteClient.save(pasteData));
    }
}
