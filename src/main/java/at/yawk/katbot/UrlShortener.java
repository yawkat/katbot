package at.yawk.katbot;

import com.google.inject.ImplementedBy;
import java.net.URI;

/**
 * @author yawkat
 */
@ImplementedBy(PasteUrlShortenerImpl.class)
public interface UrlShortener {
    URI shorten(URI uri) throws Exception;
}
