package codeassclient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KeycloakAuthenticatorTest {

    @Test
    public void extractFormActionWithEntities() {

        String html = """
                <html>
                <body>
                <form id="kc-form-login" onsubmit="login.disabled = true; return true;"
                      action="https://example.com"
                      method="post">
                    <input type="text" name="username"/>
                    <input type="password" name="password"/>
                </form>
                </body>
                </html>
                """;

        String actionUrl = KeycloakAuthenticator.extractFormAction(html);
        assertNotNull(actionUrl);
        assertEquals(
                "https://example.com",
                actionUrl
        );
    }

    @Test
    public void extractCookieValue() {

        String setCookieHeader = "am_session=s%3AZfW05b8...; Path=/; HttpOnly; Secure; SameSite=Lax";
        String value = KeycloakAuthenticator.extractCookieValue(setCookieHeader, "am_session");
        assertEquals("s%3AZfW05b8...", value);
    }
}
