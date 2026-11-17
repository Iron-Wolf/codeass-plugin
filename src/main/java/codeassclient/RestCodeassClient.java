package codeassclient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.project.Project;
import config.Config;
import main.ApiResponse;
import utils.HttpClientCustom;
import utils.PluginLogger;

/**
 * Client for retrieving spend quota via the legacy REST API.
 * @deprecated API access disabled. Keep the code for reference (or futur authorized access).
 */
@Deprecated
public class RestCodeassClient implements CodeassClient {

    private final Project project;
    private final HttpClient httpClient;

    public RestCodeassClient(@Nullable Project project) {

        this(project, HttpClientCustom.buildClientWithCert());
    }

    public RestCodeassClient(@Nullable Project project, @NotNull HttpClient httpClient) {

        this.project = project;
        this.httpClient = httpClient;
    }

    @Override
    public ApiResponse fetchAndGetResult() {

        ApiResponse apiResponse;

        try {
            var httpRequest = HttpRequest
                    .newBuilder()
                    .uri(URI.create(Config.API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Config.getApiToken())
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            apiResponse = parseApiResponse(httpResponse.statusCode(),
                                           httpResponse.body());

        } catch (InterruptedException | IOException e) {

            apiResponse = new ApiResponse.Error(0, e.getMessage());
            PluginLogger.notifyError(project, "Network Error",
                                     "API call error : " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        return apiResponse;
    }

    protected static ApiResponse parseApiResponse(int statusCode, String responseBody) {

        try {
            var json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (statusCode != 200) {
                var message = json.has("error")
                        ? json.getAsJsonObject("error").get("message").getAsString()
                        : "Unknown API error";
                return new ApiResponse.Error(statusCode, message);
            }

            if (json.has("info")) {
                var spend = json.getAsJsonObject("info").get("spend").getAsBigDecimal();
                return new ApiResponse.Success(spend);
            }
        } catch (JsonSyntaxException e) {
            PluginLogger.warn("Invalid JSON: " + e.getMessage());
            return new ApiResponse.Error(statusCode, "Invalid JSON Format from API");
        }

        return new ApiResponse.Error(statusCode, "Missing expected JSON");
    }
}
