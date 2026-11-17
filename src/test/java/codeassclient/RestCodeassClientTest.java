package codeassclient;

import java.math.BigDecimal;

import org.junit.Test;

import main.ApiResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestCodeassClientTest {

    @Test
    public void parseApiResponse_Success() {

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
        var apiResponse = (ApiResponse.Success) RestCodeassClient.parseApiResponse(200, json);

        assertEquals(new BigDecimal("7.710799649999997"), apiResponse.spend());
        assertEquals("7.71", apiResponse.spendStr());
    }

    @Test
    public void parseApiResponseInvalidJson() {

        var apiResponse = (ApiResponse.Error) RestCodeassClient.parseApiResponse(200, "not a json");

        assertEquals(200, apiResponse.httpStatus());
        assertTrue(apiResponse.message().contains("Invalid JSON"));
    }
}
