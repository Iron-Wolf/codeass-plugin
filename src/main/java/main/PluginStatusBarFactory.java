package main;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;

public class PluginStatusBarFactory
        implements StatusBarWidgetFactory {

    public static final String WIDGET_ID = "CodeAssistUniqueId";

    @Override
    public @NotNull String getId() {

        return WIDGET_ID; // must be the same as declared in META-INF/plugin.xml
    }

    @Override
    public @Nls @NotNull String getDisplayName() {

        return "Info API Widget";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {

        return true;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {

        return new PluginStatusBar(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {

        Disposer.dispose(widget);
    }

    @Override
    public boolean canBeEnabledOn(@NotNull com.intellij.openapi.wm.StatusBar statusBar) {

        return true;
    }
}
