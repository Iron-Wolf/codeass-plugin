package codeassclient;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import config.Config;
import lombok.SneakyThrows;
import utils.PluginLogger;

/**
 * Handles Keycloak OpenID Connect authentication to acquire the "am_session" cookie
 * required by the new Dashboard API.
 */
public class KeycloakAuthenticator {

    private final CookieManager cookieManager;
    private String sessionCookie;
    private String bearerToken;

    public KeycloakAuthenticator(@Nullable CookieManager cookieManager) {

        this.cookieManager = cookieManager;
    }

    public boolean isAuthenticated() {

        return (sessionCookie != null && !sessionCookie.isBlank())
               || (bearerToken != null && !bearerToken.isBlank());
    }

    public void invalidateSession() {

        this.sessionCookie = null;
        this.bearerToken = null;
        if (cookieManager != null) {
            cookieManager.getCookieStore().removeAll();
        }
    }

    /**
     * Executes the Keycloak authentication flow:
     * 1. Initiates the OIDC auth session by requesting the login page.
     * 2. Extracts the dynamic form action URL from the HTML.
     * 3. POSTs credentials (username, password, credentialId) to Keycloak.
     * 4. Follows the redirect to the auth callback URL.
     * 5. Captures and stores the resulting "am_session" cookie.
     */
    public boolean authenticate(@NotNull HttpClient client) {

        var user = Config.getLogin();
        var pass = Config.getPassword();

        if (user == null || user.isBlank() || pass == null || pass.isBlank()) {
            PluginLogger.warn("Cannot authenticate with Keycloak: login or password is empty");
            return false;
        }

        try {
            // Step 1: Request the login page to initiate session and extract form action URL
            var loginPageUrl = resolveLoginPageUrl(client);

            var loginPageReq = HttpRequest.newBuilder()
                                          .uri(URI.create(loginPageUrl))
                                          .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                          .timeout(Duration.ofSeconds(10))
                                          .GET()
                                          .build();

            var loginPageResp = client.send(loginPageReq, HttpResponse.BodyHandlers.ofString());
            extractAndStoreSessionCookie(loginPageResp);

            var formActionUrl = extractFormAction(loginPageResp.body());
            if (formActionUrl == null) {
                PluginLogger.warn("Failed to extract Keycloak login form action URL from HTML (status " + loginPageResp.statusCode() + ")");
                return false;
            }

            // Step 2: POST credentials to Keycloak
            var formData = "username=" + URLEncoder.encode(user, StandardCharsets.UTF_8)
                           + "&password=" + URLEncoder.encode(pass, StandardCharsets.UTF_8)
                           + "&credentialId=";

            var postReq = HttpRequest.newBuilder()
                                     .uri(URI.create(formActionUrl))
                                     .header("Content-Type", "application/x-www-form-urlencoded")
                                     .header("Origin", Config.KEYCLOAK_HOST)
                                     .header("Referer", loginPageUrl)
                                     .timeout(Duration.ofSeconds(10))
                                     .POST(HttpRequest.BodyPublishers.ofString(formData))
                                     .build();

            var postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString());
            var postStatus = postResp.statusCode();
            extractAndStoreSessionCookie(postResp);

            String callbackUrl = null;
            if (postStatus == 302 || postStatus == 303 || postStatus == 307) {
                callbackUrl = postResp.headers().firstValue("Location").orElse(null);
            }

            if (callbackUrl == null) {
                PluginLogger.warn("Keycloak authentication failed: no redirect Location returned (status " + postStatus + ")");
                return false;
            }

            // Step 3: GET the callback URL
            var callbackReq = HttpRequest.newBuilder()
                                         .uri(URI.create(callbackUrl))
                                         .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                         .timeout(Duration.ofSeconds(10))
                                         .GET()
                                         .build();

            var callbackResp = client.send(callbackReq, HttpResponse.BodyHandlers.ofString());
            extractAndStoreSessionCookie(callbackResp);

            if (isAuthenticated()) {
                return true;
            }

            PluginLogger.warn("Auth callback completed but am_session cookie was not found.");
            return false;

        } catch (IOException | InterruptedException e) {
            PluginLogger.error("Error during Keycloak authentication: " + e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @SneakyThrows
    private @NotNull String resolveLoginPageUrl(@NotNull HttpClient client) {

        for (var url : Config.PROBE_URLS) {
            var probeReq = HttpRequest.newBuilder()
                                      .uri(URI.create(url))
                                      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                      .timeout(Duration.ofSeconds(5))
                                      .GET()
                                      .build();

            var resp = client.send(probeReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 302 || resp.statusCode() == 303 || resp.statusCode() == 307) {
                var location = resp.headers().firstValue("Location").orElse(null);
                if (location != null && location.contains("safin.private.sfr.com")) {
                    return location;
                }
            }

        }

        return Config.KEYCLOAK_AUTH_URL + "&state=" + UUID.randomUUID().toString().replace("-", "");
    }

    public static @Nullable String extractFormAction(@Nullable String html) {

        if (html == null || html.isBlank()) {
            return null;
        }

        var pattern = Pattern.compile("<form[^>]+action=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        var matcher = pattern.matcher(html);
        if (matcher.find()) {
            var url = matcher.group(1).trim();
            url = url.replace("&amp;", "&");
            if (url.startsWith("/")) {
                url = Config.KEYCLOAK_HOST + url;
            }
            return url;
        }
        return null;
    }

    private void extractAndStoreSessionCookie(HttpResponse<?> response) {

        for (var setCookie : response.headers().allValues("Set-Cookie")) {
            if (setCookie.contains("am_session=")) {
                var cookieValue = extractCookieValue(setCookie, "am_session");
                if (cookieValue != null) {
                    this.sessionCookie = "am_session=" + cookieValue;
                }
            }
        }

        if (cookieManager != null) {
            for (var cookie : cookieManager.getCookieStore().getCookies()) {
                if ("am_session".equalsIgnoreCase(cookie.getName())) {
                    this.sessionCookie = "am_session=" + cookie.getValue();
                }
            }
        }
    }

    public static @Nullable String extractCookieValue(@NotNull String setCookieHeader, @NotNull String cookieName) {

        var prefix = cookieName + "=";
        var start = setCookieHeader.indexOf(prefix);
        if (start != -1) {
            var valStart = start + prefix.length();
            var valEnd = setCookieHeader.indexOf(';', valStart);
            return valEnd == -1 ? setCookieHeader.substring(valStart).trim() : setCookieHeader.substring(valStart, valEnd).trim();
        }
        return null;
    }

    public @Nullable String getSessionCookie() {

        return sessionCookie;
    }
}
