package utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

import ui.PluginConfigurable;
import lombok.extern.slf4j.Slf4j;

/**
 * Log and notification middleware.
 */
@Slf4j
public final class PluginLogger {

    public static final String NOTIFICATION_GROUP_ID = "CodeAssistPlugin";

    private PluginLogger() {

    }

    public static void info(@NotNull String message) {

        log.info(message);
    }

    public static void warn(@NotNull String message) {

        log.warn(message);
    }

    public static void error(@NotNull String message) {

        log.error(message);
    }

    public static void error(@NotNull String message, @Nullable Throwable throwable) {

        log.error(message, throwable);
    }

    public static void notifyError(@Nullable Project project, @NotNull String title, @NotNull String message) {

        log.warn("{}: {}", title, message);
        notify(project, title, message, NotificationType.ERROR);
    }

    public static void notifyInfo(@Nullable Project project, @NotNull String title, @NotNull String message) {

        notifyInfo(project, title, message, null);
    }

    public static void notifyInfo(@Nullable Project project,
                                  @NotNull String title,
                                  @NotNull String message,
                                  @Nullable NotificationAction extraAction) {

        log.info("{}: {}", title, message);
        notify(project, title, message, NotificationType.INFORMATION, extraAction);
    }

    private static void notify(@Nullable Project project,
                               @NotNull String title,
                               @NotNull String content,
                               @NotNull NotificationType type) {

        notify(project, title, content, type, null);
    }

    private static void notify(@Nullable Project project,
                               @NotNull String title,
                               @NotNull String content,
                               @NotNull NotificationType type,
                               @Nullable NotificationAction extraAction) {

        Application app = ApplicationManager.getApplication();
        if (app == null) {
            return;
        }

        app.invokeLater(
                () -> {
                    NotificationGroup group = NotificationGroupManager.getInstance()
                                                                      .getNotificationGroup(NOTIFICATION_GROUP_ID);
                    Notification notification;
                    if (group != null) {
                        notification = group.createNotification(title, content, type);
                    } else {
                        notification = new Notification(NOTIFICATION_GROUP_ID, title, content, type);
                    }
                    if (extraAction != null) {
                        notification.addAction(extraAction);
                    }
                    notification.addAction(createOpenSettingsAction());
                    notification.notify(project);
                },
                ModalityState.any());
    }

    /**
     * Builds the "open plugin settings" action displayed as a gear icon in the notification's
     * action bar (bottom-right area), so it never overlaps the notification content.
     */
    private static NotificationAction createOpenSettingsAction() {

        return new NotificationAction("Open settings") {

            {
                getTemplatePresentation().setIcon(AllIcons.General.Settings);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {

                ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), PluginConfigurable.class);
            }
        };
    }
}
