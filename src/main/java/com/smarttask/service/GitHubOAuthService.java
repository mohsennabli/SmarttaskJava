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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubOAuthService {
    private static final String AUTH_ENDPOINT = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_ENDPOINT = "https://github.com/login/oauth/access_token";
    private static final String USER_ENDPOINT = "https://api.github.com/user";
    private static final String EMAILS_ENDPOINT = "https://api.github.com/user/emails";

    private static final String CLIENT_ID = readConfig("SMARTTASK_GITHUB_CLIENT_ID", "Ov23liEk3AZXEH5Rnlz0");
    private static final String CLIENT_SECRET = readConfig("SMARTTASK_GITHUB_CLIENT_SECRET", "d63ed9147841eea4962cf7e1db2462037303aea1");
    private static final String REDIRECT_URI = readConfig(
            "SMARTTASK_GITHUB_REDIRECT_URI",
            "http://127.0.0.1:8766/oauth/github/callback"
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
        callbackServer.createContext("/oauth/github/url", new AuthUrlHandler(state));
        callbackServer.createContext("/oauth/github/callback", new CallbackHandler(state));
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
        URI localUrl = URI.create("http://" + redirect.getHost() + ":" + redirect.getPort() + "/oauth/github/url");

        HttpRequest request = HttpRequest.newBuilder(localUrl)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Unable to read local GitHub auth URL endpoint.");
        }

        String authUrl = extractJsonStringValue(response.body(), "authUrl");
        if (authUrl == null || authUrl.isBlank()) {
            throw new IOException("Local GitHub auth URL response is invalid.");
        }
        return authUrl;
    }

    public GitHubUserInfo authenticateWithCode(String code) throws IOException, InterruptedException {
        String accessToken = exchangeCodeForAccessToken(code);
        if (accessToken == null || accessToken.isBlank()) {
            throw new IOException("GitHub token response did not include access_token.");
        }

        GitHubUserInfo userInfo = fetchUserInfo(accessToken);
        String email = userInfo.getEmail();
        if (email == null || email.isBlank()) {
            email = fetchPrimaryEmail(accessToken);
        }

        if (email == null || email.isBlank()) {
            throw new IOException("GitHub account does not expose an email address.");
        }

        return new GitHubUserInfo(userInfo.getId(), userInfo.getLogin(), userInfo.getName(), userInfo.getAvatarUrl(), email);
    }

    public String generateState() {
        return UUID.randomUUID().toString();
    }

    public String buildAuthorizationUrl(String state) {
        return AUTH_ENDPOINT
                + "?client_id=" + encode(CLIENT_ID)
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&response_type=code"
                + "&scope=" + encode("read:user user:email")
                + "&state=" + encode(state);
    }

    private String exchangeCodeForAccessToken(String code) throws IOException, InterruptedException {
        String requestBody = "code=" + encode(code)
                + "&client_id=" + encode(CLIENT_ID)
                + "&client_secret=" + encode(CLIENT_SECRET)
                + "&redirect_uri=" + encode(REDIRECT_URI);

        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub token exchange failed with status " + response.statusCode());
        }

        return extractJsonStringValue(response.body(), "access_token");
    }

    private GitHubUserInfo fetchUserInfo(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(USER_ENDPOINT))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub user request failed with status " + response.statusCode());
        }

        String id = extractIdValue(response.body());
        String login = extractJsonStringValue(response.body(), "login");
        String name = extractJsonStringValue(response.body(), "name");
        String avatarUrl = extractJsonStringValue(response.body(), "avatar_url");
        String email = extractJsonStringValue(response.body(), "email");

        if (id == null || id.isBlank() || login == null || login.isBlank()) {
            throw new IOException("GitHub profile response is missing required fields.");
        }

        return new GitHubUserInfo(id, login, name, avatarUrl, email);
    }

    private String fetchPrimaryEmail(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(EMAILS_ENDPOINT))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub emails request failed with status " + response.statusCode());
        }

        List<GitHubEmailRecord> emailRecords = parseEmailRecords(response.body());
        for (GitHubEmailRecord record : emailRecords) {
            if (record.primary && record.verified && record.email != null && !record.email.isBlank()) {
                return record.email;
            }
        }
        for (GitHubEmailRecord record : emailRecords) {
            if (record.verified && record.email != null && !record.email.isBlank()) {
                return record.email;
            }
        }
        for (GitHubEmailRecord record : emailRecords) {
            if (record.email != null && !record.email.isBlank()) {
                return record.email;
            }
        }
        return null;
    }

    private static List<GitHubEmailRecord> parseEmailRecords(String json) {
        List<GitHubEmailRecord> records = new ArrayList<>();
        Matcher objectMatcher = Pattern.compile("\\{[^{}]*}").matcher(json);
        while (objectMatcher.find()) {
            String objectJson = objectMatcher.group();
            String email = extractJsonStringValue(objectJson, "email");
            boolean primary = extractBooleanValue(objectJson, "primary");
            boolean verified = extractBooleanValue(objectJson, "verified");
            if (email != null) {
                records.add(new GitHubEmailRecord(email, primary, verified));
            }
        }
        return records;
    }

    private static String extractJsonStringValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return unescapeJsonString(matcher.group(1));
    }

    private static String extractIdValue(String json) {
        Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private static boolean extractBooleanValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
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

    public static final class GitHubUserInfo {
        private final String id;
        private final String login;
        private final String name;
        private final String avatarUrl;
        private final String email;

        public GitHubUserInfo(String id, String login, String name, String avatarUrl, String email) {
            this.id = id;
            this.login = login;
            this.name = name;
            this.avatarUrl = avatarUrl;
            this.email = email;
        }

        public String getId() {
            return id;
        }

        public String getLogin() {
            return login;
        }

        public String getName() {
            return name;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public String getEmail() {
            return email;
        }
    }

    private static final class GitHubEmailRecord {
        private final String email;
        private final boolean primary;
        private final boolean verified;

        private GitHubEmailRecord(String email, boolean primary, boolean verified) {
            this.email = email;
            this.primary = primary;
            this.verified = verified;
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
                callbackFuture.completeExceptionally(new IOException("GitHub OAuth canceled or failed: " + error));
                sendText(exchange, 400,
                        "<html><body><h3>GitHub sign-in was canceled.</h3>You can close this window.</body></html>",
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
                    "<html><body><h3>GitHub sign-in completed.</h3>You can close this window.</body></html>",
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




