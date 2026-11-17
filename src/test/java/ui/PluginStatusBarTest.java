package ui;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;

import codeassclient.DashboardCodeassClient;
import config.Config;
import main.ApiResponse;
import utils.PluginLogger;

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

    /**
     * Non-mocked test, used to call the live API.
     */
    //@Test
    public void fetchAndGetResult() {

        Config.API_TOKEN = "<TOKEN>";

        PluginStatusBar widget = new PluginStatusBar(getDummyProject());

        var liveApiResponse = widget.fetchAndGetResult();
        assertNotNull(liveApiResponse);

        var apiSuccess = (ApiResponse.Success) liveApiResponse;
        assertNotNull(apiSuccess.spend());
        assertTrue(apiSuccess.spend().compareTo(BigDecimal.ZERO) > 0);

    }

    /**
     * Non-mocked test, used to test PluginStatusBar with the new DashboardCodeassClient.
     */
    //@Test
    public void fetchAndGetResultWithDashboardClient() {

        Config.LOGIN = "<LOGIN>";
        Config.PASSWORD = "<PASSWORD>";

        DashboardCodeassClient dashboardClient = new DashboardCodeassClient(getDummyProject());
        PluginStatusBar widget = new PluginStatusBar(getDummyProject(), dashboardClient);

        ApiResponse liveApiResponse = widget.fetchAndGetResult();
        assertNotNull(liveApiResponse);
        PluginLogger.info("Dashboard API response: " + liveApiResponse);
    }
}
