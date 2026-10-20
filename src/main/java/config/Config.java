package config;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

public class Config {

    private Config() {

    }

    public static final String PROJECT_NAME = "Code Assist";
    public static final String API_URL = "https://exemple.com/info";

    /**
     * Resolves the API token to use, in order of priority:
     * <ol>
     *     <li>the value configured by the user in the plugin Settings (stored securely via {@link SettingsSafe})</li>
     *     <li>the {@code API_TOKEN} environment variable (useful for local development / tests)</li>
     *     <li>the literal {@code "none"} fallback</li>
     * </ol>
     */
    public static String getApiToken() {

        String storedToken = SettingsSafe.getApiToken();
        if (storedToken != null && !storedToken.isBlank()) {
            return storedToken;
        }

        return System.getenv().getOrDefault("API_TOKEN", "none");
    }

    /**
     * Resolves the refresh interval (in minutes) to use, in order of priority:
     * <ol>
     *     <li>the value configured by the user in the plugin Settings (persisted via {@link SettingsUnsafe})</li>
     *     <li>the default value ({@value SettingsUnsafe#DEFAULT_REFRESH_INTERVAL_MINUTES} minutes), used when no
     *     IDE application is running (e.g. in plain unit tests)</li>
     * </ol>
     */
    public static long getRefreshIntervalMinutes() {

        Application app = ApplicationManager.getApplication();
        if (app == null) {
            return SettingsUnsafe.DEFAULT_REFRESH_INTERVAL_MINUTES;
        }

        return SettingsUnsafe.getInstance().getRefreshIntervalMinutes();
    }

    public static final String HARDCODED_CERT_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIEmzCCA4OgAwIBAgIRAKrdC9KEqcb1AWcLduEDE1QwDQYJKoZIhvcNAQELBQAw
            VjELMAkGA1UEBhMCRlIxMjAwBgNVBAoTKVNPQ0lFVEUgRlJBTkNBSVNFIERVIFJB
            RElPVEVMRVBIT05FIC0gU0ZSMRMwEQYDVQQDEwpTRlIgQUMgU1NMMB4XDTI2MDkx
            NzIwNTg0OFoXDTI3MDkxNzIwNTg0OFowgawxCzAJBgNVBAYTAkZSMQ4wDAYDVQQI
            EwVQQVJJUzEOMAwGA1UEBxMFUEFSSVMxNTAzBgNVBAoTLFNPQ0lFVEUgRlJBTkNB
            SVNFIERVIFJBRElPVEVMRVBIT05FIC0gU0ZSIFNBMRkwFwYDVQQLDBB0ZXN0X2lu
            dGVncmF0aW9uMSswKQYDVQQDEyJpbXBhY3QtY29kZS1hc3Npc3QucHJpdmF0ZS5z
            ZnIuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2RPHJ3giD1U7
            V++A7+bEoDAmQ6HSjBHF23t2ReERTC9nI7T8DggPYrRqlZ/FfXaJEl1xJCbpkLje
            aVm08iVad8wTAjmbhhS6tJ09TjsiiJm4Q13Lp9sJObIozOIll+m8m8HxK8sRttLH
            PYZmxa4ugm4yBEyXJ6pUzi5i5c8Rza3wAFRorVR41pX4deats41hoaopkqTfVluz
            SdIRadI78JhAVKBn7eJ4W1EucpNCzFxTjfa/R0ZGUqbAA1iN+dq67SMCrlqESdzc
            yUpfMXevUcb5DhQgMkmLTqE3XH+Zykp29bpEG1SdZpT/LgT4JftPnNnZX2wcwmAm
            SGt6xxgPVwIDAQABo4IBCzCCAQcwDgYDVR0PAQH/BAQDAgWgMB0GA1UdDgQWBBTg
            0s+IiNzqwvxqP2dd6COlxqUrejAzBgNVHR8ELDAqMCigJqAkhiJodHRwOi8vY3Js
            LnNmci5mci9zZnJfc3NsXzIwMjAuY3JsMCcGA1UdIAQgMB4wDQYLKoF6ASMZAgEL
            AQEwDQYLKoF6ASMZAgELAgEwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMC
            MAkGA1UdEwQCMAAwLQYDVR0RBCYwJIIiaW1wYWN0LWNvZGUtYXNzaXN0LnByaXZh
            dGUuc2ZyLmNvbTAfBgNVHSMEGDAWgBQK8L5OyOCN8p1TMIJ/HGD5AutcEzANBgkq
            hkiG9w0BAQsFAAOCAQEApnGMJzYsAJDN6feWYFF+m2BSI8W8XMw9ANfto1HoeINd
            9s/dd93OswePr4+IYQWFbaNJMrCCRJS2nW6S2+TgqicUEN6JppB3A+D4uQ8uQQfi
            Tdr+/MiXXdcyXTs8YvS/3sQVND7vU+/eur2gPMNrh01FXJkLGsapX6EPhOmYFghE
            eNTWc7yFli31nNaJkyoLvt6ZAs4/+epQ2eJZVA348XmhqBs7pJUjS6pqGceFpstf
            fndDWR4r2GEstwKkj4z/59xLf0tB3B6oH0ifCUvY3mr5rWFfVyxmIoRUFK9hMyzF
            dhZQJ0Cnu2Uhya8tvZDhSlFV6PCppSOiPzU5fpUUeQ==
            -----END CERTIFICATE-----
            """;

}
