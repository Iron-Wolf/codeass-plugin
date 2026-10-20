package config;

import org.jetbrains.annotations.Nullable;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

/**
 * Securely stores/retrieves plugin credentials (e.g. the API token) using the IntelliJ
 * Platform {@link PasswordSafe}, so the secret never ends up in plain XML config files.
 */
public final class SettingsSafe {

    private static final String SUBSYSTEM = "CodeAssistPlugin";
    private static final String API_TOKEN_KEY = "apiToken";

    private SettingsSafe() {

    }

    private static CredentialAttributes apiTokenAttributes() {

        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(SUBSYSTEM, API_TOKEN_KEY));
    }

    /**
     * @return the stored API token, or {@code null} if none is stored or no IDE application is running
     * (e.g. in plain unit tests).
     */
    public static @Nullable String getApiToken() {

        Application app = ApplicationManager.getApplication();
        if (app == null) {
            return null;
        }

        Credentials credentials = PasswordSafe.getInstance().get(apiTokenAttributes());
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    /**
     * Stores the given API token. Passing {@code null} or a blank value removes the stored token.
     */
    public static void setApiToken(@Nullable String token) {

        Application app = ApplicationManager.getApplication();
        if (app == null) {
            return;
        }

        CredentialAttributes attributes = apiTokenAttributes();
        if (token == null || token.isBlank()) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            PasswordSafe.getInstance().set(attributes, new Credentials(API_TOKEN_KEY, token));
        }
    }
}
