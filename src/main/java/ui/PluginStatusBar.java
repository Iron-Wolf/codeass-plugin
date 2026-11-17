package ui;

import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.AppExecutorUtil;

import codeassclient.CodeassClient;
import codeassclient.DashboardCodeassClient;
import config.Config;
import main.ApiResponse;
import main.PluginStatusBarFactory;
import utils.PluginLogger;

public class PluginStatusBar
        implements StatusBarWidget, StatusBarWidget.TextPresentation {

    private ScheduledFuture<?> scheduledFuture;
    private volatile boolean disposed;

    private StatusBar myStatusBar;
    private final Project project;
    private final CodeassClient client;
    private ApiResponse lastApiResponse = new ApiResponse.Initial();

    public PluginStatusBar(@NotNull Project project) {

        this(project, new DashboardCodeassClient(project));
    }

    public PluginStatusBar(@NotNull Project project, @NotNull CodeassClient client) {

        this.project = project;
        this.client = client;
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
            case ApiResponse.Success s -> "%s &euro;".formatted(s.spend());
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

        if (lastApiResponse instanceof final ApiResponse.Success s) {
            var message = buildSuccessNotificationMessage(s);
            var d = s.details();
            if (d != null && d.history() != null && !d.history().isEmpty()) {
                var historyAction = createHistoryAction(d.history(), d.maxBudget());
                PluginLogger.notifyInfo(project, Config.PROJECT_NAME + " - " + s.updateTimeStr(), message, historyAction);
            } else {
                PluginLogger.notifyInfo(project, Config.PROJECT_NAME + " - " + s.updateTimeStr(), message);
            }
        }
        if (lastApiResponse instanceof final ApiResponse.Error e) {
            var message = """
                    <html>
                        <font color='#e06c75'>
                            <b>Last error :</b><br>%s
                        </font>
                    </html>""".formatted(
                    e.message());
            PluginLogger.notifyError(project, Config.PROJECT_NAME + " - " + e.updateTimeStr(), message);
        }

    }

    private NotificationAction createHistoryAction(@NotNull List<ApiResponse.HistoryCycle> history,
                                                   @Nullable BigDecimal maxBudget) {

        return new NotificationAction("History") {
            {
                getTemplatePresentation().setIcon(AllIcons.Vcs.History);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {

                var p = project != null ? project : e.getProject();
                HistoryPopup.showPopup(p, history, maxBudget);
            }
        };
    }

    static @NotNull String buildSuccessNotificationMessage(@NotNull ApiResponse.Success s) {

        var d = s.details();
        if (d == null) {
            return """
                    <html>
                        <b>Spend :</b> %s &euro;<br>
                    </html>""".formatted(s.spendStr());
        }

        var sb = new StringBuilder("<html>");

        var pct = s.spend()
                   .multiply(BigDecimal.valueOf(100))
                   .divide(d.maxBudget(), 1, RoundingMode.HALF_UP);
        sb.append("<b>Budget:</b> %s &euro; (<b>%s%%</b>)<br>".formatted(s.spendStr(), pct));

        var remaining = d.maxBudget()
                         .subtract(s.spend())
                         .max(BigDecimal.ZERO);
        sb.append("<b>Remaining:</b> %s &euro;<br>".formatted(
                remaining.setScale(2, RoundingMode.HALF_UP).toPlainString()));

        sb.append(String.format("<b>Usage:</b> %,d req &bull; %s tokens<br>",
                                d.totalRequests(), formatTokens(d.totalTokens())));

        if (d.byModel() != null && !d.byModel().isEmpty()) {
            sb.append("<hr style='border: 0; border-top: 1px solid #555;'>");
            sb.append("<b>By model:</b><br>");
            for (var m : d.byModel()) {
                sb.append(String.format("&bull; <b>%s:</b> %s &euro; <font color='gray'>(%,d req)</font><br>",
                                        m.model(), m.spendStr(), m.requests()));
            }
        }

        if (d.expires() != null && !d.expires().isBlank()) {
            sb.append("<font color='gray' size='-1'>Key expires: %s</font>".formatted(
                    formatExpirationDate(d.expires())));
        }

        sb.append("</html>");
        return sb.toString();
    }

    public static String formatTokens(long tokens) {

        if (tokens >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fB", tokens / 1_000_000_000.0);
        } else if (tokens >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", tokens / 1_000_000.0);
        } else if (tokens >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fK", tokens / 1_000.0);
        }
        return String.valueOf(tokens);
    }

    private static String formatExpirationDate(String expires) {

        if (expires.length() >= 10) {
            var date = LocalDate.parse(expires.substring(0, 10));
            return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
        }
        return expires;
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {

        return event -> fetchDataImmediately();
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

        return client.fetchAndGetResult();
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
