package config;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/**
 * Persistent (non-secret) plugin settings, e.g. the refresh interval. Stored as plain XML
 * (unlike the API token, which goes through {@link SettingsSafe} / PasswordSafe).
 */
@Service(Service.Level.APP)
@State(name = "SettingsUnsafe", storages = @Storage("settingsUnsafe.xml"))
public final class SettingsUnsafe
        implements PersistentStateComponent<SettingsUnsafe.State> {

    public static final int DEFAULT_REFRESH_INTERVAL_MINUTES = 5;

    private State state = new State();

    public static SettingsUnsafe getInstance() {

        return ApplicationManager.getApplication().getService(SettingsUnsafe.class);
    }

    @Override
    public @NotNull State getState() {

        return state;
    }

    @Override
    public void loadState(@NotNull State state) {

        this.state = state;
    }

    public int getRefreshIntervalMinutes() {

        return state.refreshIntervalMinutes > 0 ? state.refreshIntervalMinutes : DEFAULT_REFRESH_INTERVAL_MINUTES;
    }

    public void setRefreshIntervalMinutes(int minutes) {

        state.refreshIntervalMinutes = minutes > 0 ? minutes : DEFAULT_REFRESH_INTERVAL_MINUTES;
    }

    public static class State {

        public int refreshIntervalMinutes = DEFAULT_REFRESH_INTERVAL_MINUTES;
    }
}
