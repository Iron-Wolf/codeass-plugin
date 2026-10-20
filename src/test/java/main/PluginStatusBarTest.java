package main;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import com.intellij.openapi.project.Project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PluginStatusBarTest {

    private static @NotNull Project getDummyProject() {

        return (Project) Proxy.newProxyInstance(
                Project.class.getClassLoader(),
                new Class<?>[]{ Project.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "DummyProject";
                    case "getName" -> "DummyProject";
                    case "hashCode" -> 42;
                    case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
                    default -> null;
                }
        );
    }

    @Test
    public void parseApiResponse() {

        final String json = """
                {
                "key":"b140c055a14e08a7efa47d3f707b05bf8208eda4847c37455470af6dc319c68a",
                "info":{
                    "key_name":"<TOKEN>",
                    "key_alias":"user-name",
                    "soft_budget_cooldown":false,
                    "spend":7.710799649999997,
                    "expires":"2027-01-12T21:20:57.926000+00:00",
                    "models":[]
                    }
                }
                """;
        var apiResponse = (ApiResponse.Success) PluginStatusBar.parseApiResponse(200, json);

        assertEquals(new BigDecimal("7.710799649999997"), apiResponse.spend());
        assertEquals("7.71", apiResponse.spendStr());

    }

    /**
     * Non-mocked test, used to call the live API.
     */
    //@Test
    public void fetchAndGetResult() {

        PluginStatusBar widget = new PluginStatusBar(getDummyProject());

        var liveApiResponse = widget.fetchAndGetResult();
        assertNotNull(liveApiResponse);

        var apiSuccess = (ApiResponse.Success) liveApiResponse;
        assertNotNull(apiSuccess.spend());
        assertTrue(apiSuccess.spend().compareTo(BigDecimal.ZERO) > 0);

    }
}
