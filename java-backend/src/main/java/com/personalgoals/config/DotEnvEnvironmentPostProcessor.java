package com.personalgoals.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * Makes the imported {@code .env} file behave the way a dotenv file is expected to: Spring
 * reads {@code .env} as a Java properties file, so surrounding quotes would otherwise stay
 * part of the value.
 *
 * <p>The value is otherwise used verbatim. {@code DATABASE_URL} must be a full JDBC URL,
 * including the {@code jdbc:} prefix - no prefix is added, and a {@code user:password@}
 * userinfo section is not stripped.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String ENV_FILE = ".env";

    private static final String NORMALIZED_ENV_FILE = "dotEnvNormalized";

    /** Name Spring Boot gives the source it attaches first; its constant is not public. */
    private static final String ATTACHED_SOURCE = "configurationProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MutablePropertySources sources = environment.getPropertySources();
        PropertySource<?> dotEnv = findDotEnv(sources);
        if (dotEnv == null) {
            return;
        }
        Map<String, Object> normalized = normalize(environment, dotEnv);
        if (normalized.isEmpty()) {
            return;
        }
        sources.addBefore(dotEnv.getName(), new MapPropertySource(NORMALIZED_ENV_FILE, normalized));
        // ConfigurationPropertySources caches resolved properties against the sources it saw
        // when the environment was prepared, and it is attached before any post processor
        // runs. Rebuilding it is what makes the source added above visible to @ConfigurationProperties.
        sources.remove(ATTACHED_SOURCE);
        ConfigurationPropertySources.attach(environment);
    }

    /**
     * Runs after {@code ConfigDataEnvironmentPostProcessor} has imported {@code .env}, so the
     * source is present in the environment by the time this runs.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private PropertySource<?> findDotEnv(MutablePropertySources sources) {
        for (PropertySource<?> source : sources) {
            if (source.getName().contains(ENV_FILE) && source.getSource() instanceof Map) {
                return source;
            }
        }
        return null;
    }

    private Map<String, Object> normalize(ConfigurableEnvironment environment, PropertySource<?> dotEnv) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (!(dotEnv instanceof EnumerablePropertySource<?> enumerable)) {
            return normalized;
        }
        for (String key : enumerable.getPropertyNames()) {
            Object raw = dotEnv.getProperty(key);
            Object value = normalizeValue(raw);
            if (value == raw || isShadowed(environment, key, raw)) {
                continue;
            }
            normalized.put(key, value);
        }
        return normalized;
    }

    /**
     * A real environment variable or JVM argument still wins: only values that come from
     * {@code .env} itself are rewritten.
     */
    private boolean isShadowed(ConfigurableEnvironment environment, String key, Object raw) {
        Object effective = environment.getProperty(key);
        return effective != null && !effective.equals(raw);
    }

    private Object normalizeValue(Object value) {
        if (!(value instanceof String text)) {
            return value;
        }
        return stripQuotes(text.trim());
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && isMatchingQuote(value.charAt(0), value.charAt(value.length() - 1))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private boolean isMatchingQuote(char first, char last) {
        return (first == '"' && last == '"') || (first == '\'' && last == '\'');
    }
}
