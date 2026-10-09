package com.personalgoals.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DotEnvEnvironmentPostProcessorTest {

    private static final String DOT_ENV_SOURCE =
            "Config resource 'file [.env]' via location 'optional:file:.env[.properties]'";

    private final DotEnvEnvironmentPostProcessor postProcessor = new DotEnvEnvironmentPostProcessor();

    @Test
    void leavesTheUrlVerbatim() {
        StandardEnvironment environment = environmentWith(Map.of("DATABASE_URL",
                "jdbc:postgresql://ep-foo.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require"));

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("DATABASE_URL"))
                .isEqualTo("jdbc:postgresql://ep-foo.us-east-2.aws.neon.tech/neondb"
                        + "?sslmode=require&channel_binding=require");
    }

    @Test
    void stripsQuotesFromEveryValue() {
        StandardEnvironment environment = environmentWith(Map.of(
                "DATABASE_PASSWORD", "'p@ss word'",
                "JWT_SECRET", "\"a-secret\"" ));

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("DATABASE_PASSWORD")).isEqualTo("p@ss word");
        assertThat(environment.getProperty("JWT_SECRET")).isEqualTo("a-secret");
    }

    @Test
    void letsAHigherPrecedenceSourceWin() {
        StandardEnvironment environment = environmentWith(Map.of("JWT_SECRET", "\"from-dot-env\""));
        environment.getPropertySources()
                .addFirst(new MapPropertySource("override", Map.of("JWT_SECRET", "from-environment")));

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("JWT_SECRET")).isEqualTo("from-environment");
    }

    @Test
    void doesNothingWithoutAnEnvFile() {
        StandardEnvironment environment = new StandardEnvironment();

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getPropertySources().contains("dotEnvNormalized")).isFalse();
    }

    private StandardEnvironment environmentWith(Map<String, Object> values) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource(DOT_ENV_SOURCE, values));
        return environment;
    }
}
