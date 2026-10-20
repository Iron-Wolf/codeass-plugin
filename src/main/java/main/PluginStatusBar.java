package main;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.AppExecutorUtil;

import config.Config;
import utils.HttpClientCustom;
import utils.PluginLogger;

public class PluginStatusBar
        implements StatusBarWidget, StatusBarWidget.TextPresentation {

    private static final Logger LOG = Logger.getInstance(PluginStatusBar.class);
    private ScheduledFuture<?> scheduledFuture;
    private volatile boolean disposed;

    private StatusBar myStatusBar;
    private final Project project;
    private final HttpClient httpClient;
    private ApiResponse lastApiResponse = new ApiResponse.Initial();

    public PluginStatusBar(@NotNull Project project) {

        this.project = project;
        this.httpClient = HttpClientCustom.buildClientWithCert();
        LOG.info("Init plugin...");
    }

    @Override
    public @NotNull String ID() {

        return PluginStatusBarFactory.WIDGET_ID;
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {

        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {

        this.myStatusBar = statusBar;
        LOG.info("Plugin installed in task bar");
        initScheduler();
    }

    @Override
    public synchronized void dispose() {

        disposed = true;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        myStatusBar = null;
        LOG.info(Config.PROJECT_NAME + " disposed");
    }

    @Override
    public @NotNull String getText() {

        return switch (lastApiResponse) {
            case ApiResponse.Success s -> "%s €".formatted(s.spendStr());
            case ApiResponse.Initial ignored -> "init...";
            case ApiResponse.Error ignored -> "error";
        };
    }

    @Override
    public String getTooltipText() {

        var message = switch (lastApiResponse) {
            case ApiResponse.Success s -> "%s €".formatted(s.spend());
            case ApiResponse.Initial ignored -> "init...";
            case ApiResponse.Error e -> e.message();
        };

        return """
                <html>
                    %s
                    <br>
                    <i>Clic to reload</i>
                </html>
                """.formatted(message);
    }

    public void showStatusNotification() {

        var message = "";
        if (lastApiResponse instanceof final ApiResponse.Success s) {
            message = """
                    <html>
                        <b>Spend :</b> %s<br>
                    </html>""".formatted(
                    s.spend());
            PluginLogger.notifyInfo(project, Config.PROJECT_NAME + " - " + s.updateTimeStr(), message);
        }
        if (lastApiResponse instanceof final ApiResponse.Error e) {
            message = """
                    <html>
                        <font color='#e06c75'>
                            <b>Last error :</b><br>%s
                        </font>
                    </html>""".formatted(
                    e.message());
            PluginLogger.notifyError(project, Config.PROJECT_NAME + " - " + e.updateTimeStr(), message);
        }

    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {

        return event -> {
            LOG.info("Mouse click detected ! (refresh)");
            fetchDataImmediately();
        };
    }

    @Override
    public float getAlignment() {

        return 0.5f;
    }

    private synchronized void initScheduler() {

        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            return;
        }

        Runnable task = () -> {
            if (disposed || (project != null && project.isDisposed())) {
                synchronized (PluginStatusBar.this) {
                    if (scheduledFuture != null) {
                        scheduledFuture.cancel(true);
                        scheduledFuture = null;
                    }
                }
                return;
            }
            doFetch(false);
        };

        scheduledFuture = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                task,
                0,
                Config.getRefreshIntervalMinutes(),
                TimeUnit.MINUTES
        );
    }

    public void fetchDataImmediately() {

        var app = ApplicationManager.getApplication();
        if (app != null) {
            app.executeOnPooledThread(() -> doFetch(true));
        } else {
            doFetch(true);
        }
    }

    private void doFetch(boolean showNotif) {

        if (disposed || (project != null && project.isDisposed())) {
            return;
        }
        lastApiResponse = fetchAndGetResult();
        refreshOnEdt();

        // display notification after the API call
        if (showNotif) {
            showStatusNotification();
        }
    }

    protected ApiResponse fetchAndGetResult() {

        LOG.info("Start HTTP GET request " + Config.API_URL);
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
            LOG.warn(e);
            return new ApiResponse.Error(statusCode, "Invalid JSON Format from API");
        }

        return new ApiResponse.Error(statusCode, "Missing expected JSON");
    }

    private void refreshOnEdt() {

        var app = ApplicationManager.getApplication();
        if (app != null) {
            app.invokeLater(
                    () -> {
                        var bar = myStatusBar;
                        if (bar != null) {
                            bar.updateWidget(ID());
                        }
                    },
                    ModalityState.any(),
                    o -> disposed || (project != null && project.isDisposed()));
        }
    }
}
