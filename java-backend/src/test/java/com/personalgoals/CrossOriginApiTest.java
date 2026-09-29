package com.personalgoals;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Exercises the API over a real socket so the CORS contract the Astro frontend depends on
 * is covered. MockMvc bypasses it entirely.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CrossOriginApiTest {

    private static final String ORIGIN = "http://localhost:4321";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void preflightAllowsTheAstroOriginAndTheAuthorizationHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(ORIGIN);
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.setAccessControlRequestHeaders(List.of("Authorization", "Content-Type"));

        ResponseEntity<Void> response =
                rest.exchange(url("/api/auth/login"), HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ORIGIN);
        assertThat(response.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.POST);
        assertThat(response.getHeaders().getAccessControlAllowHeaders())
                .contains("Authorization", "Content-Type");
    }

    @Test
    void loginFromTheAstroOriginEchoesTheOriginHeader() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(ORIGIN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("email", uniqueEmail(), "password", "s3cret-pass");
        ResponseEntity<String> response =
                rest.exchange(url("/api/auth/login"), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ORIGIN);
        assertThat(response.getBody()).contains("Invalid email or password");
    }

    @Test
    void unauthorizedResponsesAreReadableJsonForTheClient() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(ORIGIN);

        ResponseEntity<String> response =
                rest.exchange(url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        JsonNode error = objectMapper.readTree(response.getBody());
        assertThat(error.get("status").asInt()).isEqualTo(401);
        assertThat(error.get("message").asText()).isEqualTo("Authentication required");
        assertThat(error.get("fieldErrors").isObject()).isTrue();
    }

    @Test
    void refreshTokenFromTheAstroOriginIsAcceptedAndRotated() throws Exception {
        String email = uniqueEmail();

        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(ORIGIN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> registered = rest.exchange(
                url("/api/auth/register"),
                HttpMethod.POST,
                new HttpEntity<>(json(Map.of("email", email, "password", "s3cret-pass", "displayName", "Dhruv")), headers),
                String.class);

        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ORIGIN);

        JsonNode session = objectMapper.readTree(registered.getBody());
        assertThat(session.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(session.get("token").asText()).isNotBlank();
        assertThat(session.get("refreshToken").asText()).isNotBlank();

        ResponseEntity<String> refreshed = rest.exchange(
                url("/api/auth/refresh"),
                HttpMethod.POST,
                new HttpEntity<>(json(Map.of("refreshToken", session.get("refreshToken").asText())), headers),
                String.class);

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode rotated = objectMapper.readTree(refreshed.getBody());
        assertThat(rotated.get("refreshToken").asText()).isNotEqualTo(session.get("refreshToken").asText());

        // The rotated access token authenticates a protected route over the real socket.
        HttpHeaders authorized = new HttpHeaders();
        authorized.setOrigin(ORIGIN);
        authorized.setBearerAuth(rotated.get("token").asText());

        ResponseEntity<String> me =
                rest.exchange(url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(authorized), String.class);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(objectMapper.readTree(me.getBody()).get("email").asText()).isEqualTo(email);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String uniqueEmail() {
        return "cors-" + System.nanoTime() + "@example.com";
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
