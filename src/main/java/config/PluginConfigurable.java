package config;

import javax.swing.*;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.FormBuilder;

/**
 * Settings page letting the user configure the plugin, primarily the API token
 * used to authenticate against the API. The token is stored securely via
 * {@link SettingsSafe} (IntelliJ Platform PasswordSafe) rather than in plain XML.
 * It also exposes the refresh interval (persisted via {@link SettingsUnsafe}).
 * <p>
 * This class is also the natural place to add future, non-sensitive settings (e.g. JSON mapping
 * customization options).
 */
public class PluginConfigurable
        implements Configurable {

    private static final int MIN_REFRESH_INTERVAL_MINUTES = 1;
    private static final int MAX_REFRESH_INTERVAL_MINUTES = 1440;

    private JBPasswordField apiTokenField;
    private JSpinner refreshIntervalSpinner;
    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {

        return Config.PROJECT_NAME;
    }

    @Override
    public @Nullable JComponent createComponent() {

        apiTokenField = new JBPasswordField();
        apiTokenField.setColumns(40);
        apiTokenField.getEmptyText().setText("Paste your API token here (sk-...)");

        refreshIntervalSpinner = new JSpinner(new SpinnerNumberModel(
                SettingsUnsafe.DEFAULT_REFRESH_INTERVAL_MINUTES,
                MIN_REFRESH_INTERVAL_MINUTES,
                MAX_REFRESH_INTERVAL_MINUTES,
                1));

        mainPanel = FormBuilder.createFormBuilder()
                               .addLabeledComponent("API token:", apiTokenField)
                               .addLabeledComponent("Refresh interval (minutes):", refreshIntervalSpinner)
                               .addComponentFillVertically(new JPanel(), 0)
                               .getPanel();

        reset();

        return mainPanel;
    }

    @Override
    public boolean isModified() {

        String storedToken = SettingsSafe.getApiToken();
        String currentValue = new String(apiTokenField.getPassword());

        boolean tokenModified = storedToken == null ? !currentValue.isEmpty() : !storedToken.equals(currentValue);

        int storedInterval = SettingsUnsafe.getInstance().getRefreshIntervalMinutes();
        int currentInterval = (int) refreshIntervalSpinner.getValue();

        return tokenModified || storedInterval != currentInterval;
    }

    @Override
    public void apply() {

        String currentValue = new String(apiTokenField.getPassword());
        SettingsSafe.setApiToken(currentValue);

        SettingsUnsafe.getInstance().setRefreshIntervalMinutes((int) refreshIntervalSpinner.getValue());
    }

    @Override
    public void reset() {

        String storedToken = SettingsSafe.getApiToken();
        apiTokenField.setText(storedToken != null ? storedToken : "");

        refreshIntervalSpinner.setValue(SettingsUnsafe.getInstance().getRefreshIntervalMinutes());
    }

    @Override
    public void disposeUIResources() {

        apiTokenField = null;
        refreshIntervalSpinner = null;
        mainPanel = null;
    }
}
