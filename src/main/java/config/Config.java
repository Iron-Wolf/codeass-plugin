package config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.intellij.openapi.application.ApplicationManager;

public class Config {

    private Config() {

    }

    public static final String PROJECT_NAME = "Code Assist";
    public static final String API_URL = "https://example.com/key/info";
    @Deprecated
    public static String API_TOKEN = null;

    public static final String DASHBOARD_API_URL = "https://example.com/api-manager/dashboard/key-holders/me";
    public static final String KEYCLOAK_HOST = "https://example.com";
    public static final String CLIENT_ID = "PR-SSO-APP4631-GENAPI%40example.com%231";
    public static final String CALLBACK_URL = "https://example.com/api-manager/auth/callback";
    public static final String KEYCLOAK_AUTH_URL =
            "%s/auth/realms/SAFIN-EXTRANET/protocol/openid-connect/auth?client_id=%s&redirect_uri=%s&response_type=code&scope=openid"
                    .formatted(KEYCLOAK_HOST, CLIENT_ID, URLEncoder.encode(CALLBACK_URL, StandardCharsets.UTF_8));
    public static final String[] PROBE_URLS = new String[]{
            Config.DASHBOARD_API_URL,
            "https://example.com/api-manager/auth/login",
            "https://example.com/api-manager/dashboard/"
    };
    public static String LOGIN = null;
    public static String PASSWORD = null;

    public static String getLogin() {

        if (LOGIN != null) {
            return LOGIN;
        }

        var storedLogin = SettingsSafe.getLogin();
        if (storedLogin != null && !storedLogin.isBlank()) {
            return storedLogin;
        }

        return System.getenv().getOrDefault("CODEASS_LOGIN", "");
    }

    public static String getPassword() {

        if (PASSWORD != null) {
            return PASSWORD;
        }

        var storedPassword = SettingsSafe.getPassword();
        if (storedPassword != null && !storedPassword.isBlank()) {
            return storedPassword;
        }

        return System.getenv().getOrDefault("CODEASS_PASSWORD", "");
    }

    /**
     * @deprecated Legacy REST API token.
     */
    @Deprecated
    public static String getApiToken() {

        if (API_TOKEN != null) {
            return API_TOKEN;
        }

        var storedToken = SettingsSafe.getApiToken();
        if (storedToken != null && !storedToken.isBlank()) {
            return storedToken;
        }

        return System.getenv().getOrDefault("CODEASS_API_TOKEN", "none");
    }

    public static long getRefreshIntervalMinutes() {

        var app = ApplicationManager.getApplication();
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
