package com.smarttask.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleOAuthService {
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";

    private static final String CLIENT_ID = readConfig("SMARTTASK_GOOGLE_CLIENT_ID", "886391608156-fc05m6jvhgmp19k6liandj2ccrrrehjg.apps.googleusercontent.com");
    private static final String CLIENT_SECRET = readConfig("SMARTTASK_GOOGLE_CLIENT_SECRET", "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");
    private static final String REDIRECT_URI = readConfig(
            "SMARTTASK_GOOGLE_REDIRECT_URI",
            "http://127.0.0.1:8765/oauth/google/callback"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private HttpServer callbackServer;
    private CompletableFuture<OAuthCallbackData> callbackFuture;

    public void startLocalOAuthEndpoints(String state) throws IOException {
        stopLocalOAuthEndpoints();

        URI redirect = URI.create(REDIRECT_URI);
        int port = redirect.getPort();
        if (port <= 0) {
            throw new IOException("REDIRECT_URI must include an explicit localhost port.");
        }
        String host = redirect.getHost();
        if (host == null || host.isBlank()) {
            throw new IOException("REDIRECT_URI host is required.");
        }

        callbackFuture = new CompletableFuture<>();
        callbackServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        callbackServer.createContext("/oauth/google/url", new AuthUrlHandler(state));
        callbackServer.createContext("/oauth/google/callback", new CallbackHandler(state));
        callbackServer.setExecutor(Executors.newSingleThreadExecutor());
        callbackServer.start();
    }

    public void stopLocalOAuthEndpoints() {
        if (callbackServer != null) {
            callbackServer.stop(0);
            callbackServer = null;
        }
    }

    public CompletableFuture<OAuthCallbackData> getCallbackFuture() {
        return callbackFuture;
    }

    public String fetchLocalAuthorizationUrl() throws IOException, InterruptedException {
        URI redirect = URI.create(REDIRECT_URI);
        URI localUrl = URI.create("http://" + redirect.getHost() + ":" + redirect.getPort() + "/oauth/google/url");

        HttpRequest request = HttpRequest.newBuilder(localUrl)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Unable to read local Google auth URL endpoint.");
        }

        String authUrl = extractJsonValue(response.body(), "authUrl");
        if (authUrl == null || authUrl.isBlank()) {
            throw new IOException("Local Google auth URL response is invalid.");
        }
        return authUrl;
    }

    public GoogleUserInfo authenticateWithCode(String code) throws IOException, InterruptedException {
        GoogleTokenResponse tokenResponse = exchangeCodeForTokens(code);
        if (tokenResponse.accessToken == null || tokenResponse.accessToken.isBlank()) {
            throw new IOException("Google token response did not include access_token.");
        }
        return fetchUserInfo(tokenResponse.accessToken);
    }

    public String generateState() {
        return UUID.randomUUID().toString();
    }

    public String buildAuthorizationUrl(String state) {
        return AUTH_ENDPOINT
                + "?client_id=" + encode(CLIENT_ID)
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&access_type=offline"
                + "&prompt=" + encode("consent")
                + "&state=" + encode(state);
    }

    private GoogleTokenResponse exchangeCodeForTokens(String code) throws IOException, InterruptedException {
        String requestBody = "code=" + encode(code)
                + "&client_id=" + encode(CLIENT_ID)
                + "&client_secret=" + encode(CLIENT_SECRET)
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Google token exchange failed with status " + response.statusCode());
        }

        String accessToken = extractJsonValue(response.body(), "access_token");
        return new GoogleTokenResponse(accessToken);
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(USERINFO_ENDPOINT))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Google userinfo request failed with status " + response.statusCode());
        }

        String sub = extractJsonValue(response.body(), "sub");
        String email = extractJsonValue(response.body(), "email");
        String name = extractJsonValue(response.body(), "name");
        String picture = extractJsonValue(response.body(), "picture");

        if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
            throw new IOException("Google userinfo response is missing required fields.");
        }

        return new GoogleUserInfo(sub, email, name, picture);
    }

    private static String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJsonString(matcher.group(1));
    }

    private static String unescapeJsonString(String value) {
        return value
                .replace("\\\\/", "/")
                .replace("\\\\\"", "\"")
                .replace("\\\\n", "\n")
                .replace("\\\\r", "\r")
                .replace("\\\\t", "\t")
                .replace("\\\\\\\\", "\\");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String readConfig(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value;
        }

        return defaultValue;
    }

    public static boolean isConfigurationReady() {
        return !CLIENT_ID.startsWith("YOUR_")
                && !CLIENT_SECRET.startsWith("YOUR_")
                && REDIRECT_URI.startsWith("http://");
    }

    public static final class OAuthCallbackData {
        private final String code;
        private final String state;

        public OAuthCallbackData(String code, String state) {
            this.code = code;
            this.state = state;
        }

        public String getCode() {
            return code;
        }

        public String getState() {
            return state;
        }
    }

    public static final class GoogleUserInfo {
        private final String sub;
        private final String email;
        private final String name;
        private final String picture;

        public GoogleUserInfo(String sub, String email, String name, String picture) {
            this.sub = sub;
            this.email = email;
            this.name = name;
            this.picture = picture;
        }

        public String getSub() {
            return sub;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public String getPicture() {
            return picture;
        }
    }

    private static final class GoogleTokenResponse {
        private final String accessToken;

        private GoogleTokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    private final class AuthUrlHandler implements HttpHandler {
        private final String state;

        private AuthUrlHandler(String state) {
            this.state = state;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            String response = "{\"authUrl\":\"" + buildAuthorizationUrl(state).replace("\"", "\\\"") + "\"}";
            sendText(exchange, 200, response, "application/json; charset=UTF-8");
        }
    }

    private final class CallbackHandler implements HttpHandler {
        private final String expectedState;

        private CallbackHandler(String expectedState) {
            this.expectedState = expectedState;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getRawQuery());
            String code = queryParams.get("code");
            String state = queryParams.get("state");
            String error = queryParams.get("error");

            if (error != null) {
                callbackFuture.completeExceptionally(new IOException("Google OAuth canceled or failed: " + error));
                sendText(exchange, 400,
                        "<html><body><h3>Google sign-in was canceled.</h3>You can close this window.</body></html>",
                        "text/html; charset=UTF-8");
                return;
            }

            if (code == null || code.isBlank() || !expectedState.equals(state)) {
                callbackFuture.completeExceptionally(new IOException("Invalid OAuth callback state/code."));
                sendText(exchange, 400,
                        "<html><body><h3>Invalid OAuth callback.</h3>You can close this window.</body></html>",
                        "text/html; charset=UTF-8");
                return;
            }

            callbackFuture.complete(new OAuthCallbackData(code, state));
            sendText(exchange, 200,
                    "<html><body><h3>Google sign-in completed.</h3>You can close this window.</body></html>",
                    "text/html; charset=UTF-8");
        }
    }

    private static void sendText(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }

        String[] parts = rawQuery.split("&");
        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                String key = decode(keyValue[0]);
                String value = decode(keyValue[1]);
                params.put(key, value);
            }
        }
        return params;
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}


