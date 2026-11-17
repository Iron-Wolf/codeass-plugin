package config;

import org.jetbrains.annotations.Nullable;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

/**
 * Securely stores/retrieves plugin credentials using the IntelliJ
 * Platform {@link PasswordSafe}, so secrets never end up in plain XML config files.
 */
public final class SettingsSafe {

    private static final String SUBSYSTEM = "CodeAssistPlugin";
    private static final String API_TOKEN_KEY = "apiToken";
    private static final String KEYCLOAK_KEY = "keycloakCredentials";

    private SettingsSafe() {

    }

    private static CredentialAttributes apiTokenAttributes() {

        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(SUBSYSTEM, API_TOKEN_KEY));
    }

    private static CredentialAttributes keycloakAttributes() {

        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(SUBSYSTEM, KEYCLOAK_KEY));
    }

    public static @Nullable String getLogin() {

        Application app = ApplicationManager.getApplication();
        if (app == null) {
            return null;
        }

        Credentials credentials = PasswordSafe.getInstance().get(keycloakAttributes());
        return credentials != null ? credentials.getUserName() : null;
    }

    public static @Nullable String getPassword() {

        var app = ApplicationManager.getApplication();
        if (app == null) {
            return null;
        }

        var credentials = PasswordSafe.getInstance().get(keycloakAttributes());
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    public static void setCredentials(@Nullable String login, @Nullable String password) {

        var app = ApplicationManager.getApplication();
        if (app == null) {
            return;
        }

        var attributes = keycloakAttributes();
        if ((login == null || login.isBlank()) && (password == null || password.isBlank())) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            PasswordSafe.getInstance().set(attributes, new Credentials(login, password));
        }
    }

    /**
     * @return the stored API token, or {@code null} if none is stored or no IDE application is running
     * (e.g. in plain unit tests).
     *
     * @deprecated Legacy REST API token. Use login/password credentials instead.
     */
    @Deprecated
    public static @Nullable String getApiToken() {

        var app = ApplicationManager.getApplication();
        if (app == null) {
            return null;
        }

        var credentials = PasswordSafe.getInstance().get(apiTokenAttributes());
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    /**
     * Stores the given API token. Passing {@code null} or a blank value removes the stored token.
     *
     * @deprecated Legacy REST API token. Use login/password credentials instead.
     */
    @Deprecated
    public static void setApiToken(@Nullable String token) {

        var app = ApplicationManager.getApplication();
        if (app == null) {
            return;
        }

        var attributes = apiTokenAttributes();
        if (token == null || token.isBlank()) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            PasswordSafe.getInstance().set(attributes, new Credentials(API_TOKEN_KEY, token));
        }
    }
}
