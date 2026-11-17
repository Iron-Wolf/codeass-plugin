package codeassclient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.project.Project;

import config.Config;
import main.ApiResponse;
import utils.HttpClientCustom;
import utils.PluginLogger;

/**
 * Client for retrieving spend quota through the new Dashboard API route.
 */
public class DashboardCodeassClient
        implements CodeassClient {

    private final Project project;
    private final HttpClient httpClient;
    private final KeycloakAuthenticator authenticator;

    public DashboardCodeassClient(@Nullable Project project) {

        this.project = project;
        final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClientCustom.buildClientForDashboard(cookieManager);
        this.authenticator = new KeycloakAuthenticator(cookieManager);
    }

    @Override
    public ApiResponse fetchAndGetResult() {

        // Step 1: Ensure initial authentication
        if (!authenticator.isAuthenticated() && !authenticator.authenticate(httpClient)) {
            return new ApiResponse.Error(401, "Keycloak authentication failed for user: " + Config.getLogin());
        }

        try {
            var httpResponse = sendDashboardRequest();

            // Check if session expired or API returned an auth/gateway error
            var contentType = httpResponse.headers().firstValue("Content-Type").orElse("");
            if (shouldReauthenticate(httpResponse.statusCode(), contentType, httpResponse.body())) {
                authenticator.invalidateSession();
                if (authenticator.authenticate(httpClient)) {
                    httpResponse = sendDashboardRequest();
                } else {
                    return new ApiResponse.Error(401, "Keycloak re-authentication failed for user: " + Config.getLogin());
                }
            }

            return parseApiResponse(httpResponse.statusCode(), httpResponse.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            PluginLogger.error("Dashboard API call interrupted: " + e.getMessage());
            return new ApiResponse.Error(0, "Request interrupted: " + e.getMessage());
        } catch (IOException e) {
            try {
                authenticator.invalidateSession();
                if (authenticator.authenticate(httpClient)) {
                    HttpResponse<String> retryResponse = sendDashboardRequest();
                    return parseApiResponse(retryResponse.statusCode(), retryResponse.body());
                }
            } catch (Exception retryEx) {
                PluginLogger.error("Dashboard API retry failed after network error: " + retryEx.getMessage());
            }

            PluginLogger.notifyError(project, "Network Error", "Dashboard API call error: " + e.getMessage());
            return new ApiResponse.Error(0, e.getMessage());
        }
    }

    private HttpResponse<String> sendDashboardRequest()
            throws IOException, InterruptedException {

        var requestBuilder = HttpRequest
                .newBuilder()
                .uri(URI.create(Config.DASHBOARD_API_URL))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET();

        if (authenticator.getSessionCookie() != null) {
            requestBuilder.header("Cookie", authenticator.getSessionCookie());
        }

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    public static boolean shouldReauthenticate(int statusCode, @Nullable String contentType, @Nullable String body) {

        return switch (statusCode) {
            case 401, 403, 419,     // Auth or session expiration
                 302, 303, 307,     // Redirect to login page
                 500, 502, 503, 504 // Gateway error
                    -> true;
            default -> // HTML returned
                    (contentType != null && contentType.toLowerCase().contains("text/html")) ||
                    (body != null && body.trim().toLowerCase().startsWith("<!doctype"));
        };
    }

    protected static ApiResponse parseApiResponse(int statusCode, String responseBody) {

        if (statusCode == 200 && responseBody != null) {
            try {
                var json = JsonParser.parseString(responseBody).getAsJsonObject();
                if (json.has("key") && json.get("key").isJsonObject()) {
                    var keyObj = json.getAsJsonObject("key");
                    var spend = keyObj.has("spend") && !keyObj.get("spend").isJsonNull()
                            ? keyObj.get("spend").getAsBigDecimal()
                            : BigDecimal.ZERO;

                    BigDecimal maxBudget = null;
                    if (keyObj.has("effective_budget") && !keyObj.get("effective_budget").isJsonNull()) {
                        maxBudget = keyObj.get("effective_budget").getAsBigDecimal();
                    } else if (keyObj.has("max_budget") && !keyObj.get("max_budget").isJsonNull()) {
                        maxBudget = keyObj.get("max_budget").getAsBigDecimal();
                    } else if (json.has("effective_budget") && !json.get("effective_budget").isJsonNull()) {
                        maxBudget = json.get("effective_budget").getAsBigDecimal();
                    }

                    String expires = null;
                    if (keyObj.has("expires") && !keyObj.get("expires").isJsonNull()) {
                        expires = keyObj.get("expires").getAsString();
                    }

                    var totalRequests = 0;
                    var totalTokens = 0L;
                    var byModel = new ArrayList<ApiResponse.ModelUsage>();

                    if (json.has("current_usage") && json.get("current_usage").isJsonObject()) {
                        var usageObj = json.getAsJsonObject("current_usage");
                        if (usageObj.has("total_requests") && !usageObj.get("total_requests").isJsonNull()) {
                            totalRequests = usageObj.get("total_requests").getAsInt();
                        }
                        if (usageObj.has("total_tokens") && !usageObj.get("total_tokens").isJsonNull()) {
                            totalTokens = usageObj.get("total_tokens").getAsLong();
                        }
                        if (usageObj.has("by_model") && usageObj.get("by_model").isJsonArray()) {
                            byModel = parseModelUsages(usageObj.getAsJsonArray("by_model"));
                        }
                    }

                    var history = new ArrayList<ApiResponse.HistoryCycle>();
                    if (json.has("history_3_months") && json.get("history_3_months").isJsonArray()) {
                        for (var elem : json.getAsJsonArray("history_3_months")) {
                            if (elem.isJsonObject()) {
                                var cycleObj = elem.getAsJsonObject();
                                var cycle = cycleObj.has("cycle") && !cycleObj.get("cycle").isJsonNull()
                                        ? cycleObj.get("cycle").getAsString() : "";
                                var cycleSpend = BigDecimal.ZERO;
                                if (cycleObj.has("total_spend") && !cycleObj.get("total_spend").isJsonNull()) {
                                    cycleSpend = cycleObj.get("total_spend").getAsBigDecimal();
                                } else if (cycleObj.has("spend") && !cycleObj.get("spend").isJsonNull()) {
                                    cycleSpend = cycleObj.get("spend").getAsBigDecimal();
                                }
                                var cycleReqs = 0;
                                if (cycleObj.has("total_requests") && !cycleObj.get("total_requests").isJsonNull()) {
                                    cycleReqs = cycleObj.get("total_requests").getAsInt();
                                } else if (cycleObj.has("requests") && !cycleObj.get("requests").isJsonNull()) {
                                    cycleReqs = cycleObj.get("requests").getAsInt();
                                }
                                var cycleTokens = 0L;
                                if (cycleObj.has("total_tokens") && !cycleObj.get("total_tokens").isJsonNull()) {
                                    cycleTokens = cycleObj.get("total_tokens").getAsLong();
                                }
                                var cycleModels = new ArrayList<ApiResponse.ModelUsage>();
                                if (cycleObj.has("by_model") && cycleObj.get("by_model").isJsonArray()) {
                                    cycleModels = parseModelUsages(cycleObj.getAsJsonArray("by_model"));
                                }
                                history.add(new ApiResponse.HistoryCycle(cycle, cycleSpend, cycleReqs, cycleTokens, cycleModels));
                            }
                        }
                    }

                    var details = new ApiResponse.DashboardDetails(maxBudget, totalRequests, totalTokens, expires, byModel, history);
                    return new ApiResponse.Success(spend, details);
                }

                return new ApiResponse.Error(statusCode, responseBody);

            } catch (JsonSyntaxException e) {
                PluginLogger.warn("Could not parse dashboard response as JSON: " + e.getMessage());
                return new ApiResponse.Error(statusCode, "Invalid JSON from dashboard: " + responseBody);
            }
        }

        return new ApiResponse.Error(statusCode, responseBody != null ? responseBody : "Empty response");
    }

    private static ArrayList<ApiResponse.ModelUsage> parseModelUsages(JsonArray array) {

        var byModel = new ArrayList<ApiResponse.ModelUsage>();
        for (var elem : array) {
            if (elem.isJsonObject()) {
                var m = elem.getAsJsonObject();
                var modelName = m.has("model") && !m.get("model").isJsonNull()
                        ? m.get("model").getAsString() : "unknown";
                var reqs = m.has("requests") && !m.get("requests").isJsonNull()
                        ? m.get("requests").getAsInt() : 0;
                var modelSpend = m.has("spend") && !m.get("spend").isJsonNull()
                        ? m.get("spend").getAsBigDecimal() : BigDecimal.ZERO;
                var modelTokens = m.has("total_tokens") && !m.get("total_tokens").isJsonNull()
                        ? m.get("total_tokens").getAsLong() : 0L;
                byModel.add(new ApiResponse.ModelUsage(modelName, reqs, modelSpend, modelTokens));
            }
        }
        byModel.sort((a, b) -> b.spend().compareTo(a.spend()));
        return byModel;
    }
}
