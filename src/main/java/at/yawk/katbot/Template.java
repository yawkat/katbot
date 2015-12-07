package at.yawk.katbot;

import java.util.function.Supplier;

/**
 * Helper class to substitute properties like {@code ${val}} in strings.
 *
 * @author yawkat
 */
public class Template {
    private final String data;

    private Template(String data) {
        this.data = data;
    }

    public static Template parse(String template) {
        return new Template(template);
    }

    public Template set(String property, String value) {
        return new Template(data.replace("${" + property + '}', value));
    }

    public Template set(String property, Supplier<String> value) {
        if (data.contains("${" + property + '}')) {
            return set(property, value.get());
        } else {
            return this;
        }
    }

    public String finish() {
        return data;
    }
}
