package ui;

import javax.swing.*;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import config.Config;
import config.SettingsSafe;
import config.SettingsUnsafe;

/**
 * Settings page.
 * Sensitive data is stored securely via {@link SettingsSafe} (IntelliJ Platform PasswordSafe)
 * rather than in plain XML. Non-sensitive settings are persisted via {@link SettingsUnsafe}.
 */
public class PluginConfigurable
        implements Configurable {

    private static final int MIN_REFRESH_INTERVAL_MINUTES = 1;
    private static final int MAX_REFRESH_INTERVAL_MINUTES = 1440;

    private JBTextField loginField;
    private JBPasswordField passwordField;
    private JSpinner refreshIntervalSpinner;
    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {

        return Config.PROJECT_NAME;
    }

    @Override
    public @Nullable JComponent createComponent() {

        loginField = new JBTextField();
        loginField.setColumns(30);
        loginField.getEmptyText().setText("Username (e.g. u123456)");

        passwordField = new JBPasswordField();
        passwordField.setColumns(30);
        passwordField.getEmptyText().setText("Password");

        refreshIntervalSpinner = new JSpinner(new SpinnerNumberModel(
                SettingsUnsafe.DEFAULT_REFRESH_INTERVAL_MINUTES,
                MIN_REFRESH_INTERVAL_MINUTES,
                MAX_REFRESH_INTERVAL_MINUTES,
                1));

        mainPanel = FormBuilder.createFormBuilder()
                               .addLabeledComponent("Login:", loginField)
                               .addLabeledComponent("Password:", passwordField)
                               .addLabeledComponent("Refresh interval (minutes):", refreshIntervalSpinner)
                               .addSeparator()
                               .addComponentFillVertically(new JPanel(), 0)
                               .getPanel();

        reset();

        return mainPanel;
    }

    @Override
    public boolean isModified() {

        var storedLogin = SettingsSafe.getLogin();
        var currentLogin = loginField != null ? loginField.getText().trim() : "";
        var loginModified = storedLogin == null ? !currentLogin.isEmpty() : !storedLogin.equals(currentLogin);

        var storedPassword = SettingsSafe.getPassword();
        var currentPassword = passwordField != null ? new String(passwordField.getPassword()) : "";
        var passwordModified = storedPassword == null ? !currentPassword.isEmpty() : !storedPassword.equals(currentPassword);

        var storedInterval = SettingsUnsafe.getInstance().getRefreshIntervalMinutes();
        var currentInterval = refreshIntervalSpinner != null ? (int) refreshIntervalSpinner.getValue() : storedInterval;

        return loginModified || passwordModified || storedInterval != currentInterval;
    }

    @Override
    public void apply() {

        if (loginField != null && passwordField != null) {
            var currentLogin = loginField.getText().trim();
            var currentPassword = new String(passwordField.getPassword());
            SettingsSafe.setCredentials(currentLogin, currentPassword);
        }

        if (refreshIntervalSpinner != null) {
            SettingsUnsafe.getInstance().setRefreshIntervalMinutes((int) refreshIntervalSpinner.getValue());
        }
    }

    @Override
    public void reset() {

        var storedLogin = SettingsSafe.getLogin();
        if (loginField != null) {
            loginField.setText(storedLogin != null ? storedLogin : "");
        }

        var storedPassword = SettingsSafe.getPassword();
        if (passwordField != null) {
            passwordField.setText(storedPassword != null ? storedPassword : "");
        }

        if (refreshIntervalSpinner != null) {
            refreshIntervalSpinner.setValue(SettingsUnsafe.getInstance().getRefreshIntervalMinutes());
        }
    }

    @Override
    public void disposeUIResources() {

        loginField = null;
        passwordField = null;
        refreshIntervalSpinner = null;
        mainPanel = null;
    }
}
