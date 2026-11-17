package codeassclient;

import java.math.BigDecimal;

import org.junit.Test;

import main.ApiResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DashboardCodeassClientTest {

    @Test
    public void parseDashboardApiResponse_Success() {

        String json = """
                {
                    "has_key": true,
                    "key": {
                        "key_alias": "alias",
                        "spend": 29.29757300749999,
                        "max_budget": 200.0,
                        "base_max_budget": 200.0,
                        "effective_budget": 200.0,
                        "is_augmented": false,
                        "is_blocked": false,
                        "created_at": "2026-10-14T21:20:57.932000Z",
                        "first_created_at": null,
                        "last_active": "2026-11-16T14:48:37.076000Z",
                        "expires": "2027-01-12T21:20:57.926000Z"
                    },
                    "current_usage": {
                        "cycle": "2026-11",
                        "by_model": [
                            {
                                "model": "gemini-3.8-flash",
                                "requests": 1545,
                                "input_tokens": 99548490,
                                "output_tokens": 682520,
                                "cache_read_tokens": 92711535,
                                "cache_creation_tokens": 0,
                                "total_tokens": 100231010,
                                "spend": 15.664931017499997
                            },
                            {
                                "model": "gemini-4-argon",
                                "requests": 272,
                                "input_tokens": 33524809,
                                "output_tokens": 234567,
                                "cache_read_tokens": 29975689,
                                "cache_creation_tokens": 0,
                                "total_tokens": 33759376,
                                "spend": 13.632641989999994
                            }
                        ],
                        "total_spend": 29.2976,
                        "total_requests": 1817,
                        "total_tokens": 133990386
                    },
                    "effective_budget": 200.0,
                    "is_augmented": false,
                    "history_3_months": [
                        {
                            "cycle": "2026-10",
                            "by_model": [
                                {
                                    "model": "claude-sonnet-5",
                                    "requests": 300,
                                    "input_tokens": 29209510,
                                    "output_tokens": 276732,
                                    "cache_read_tokens": 0,
                                    "cache_creation_tokens": 0,
                                    "total_tokens": 29486242,
                                    "spend": 67.26212680000002
                                },
                                {
                                    "model": "gemini-3.8-flash",
                                    "requests": 2536,
                                    "input_tokens": 73823842,
                                    "output_tokens": 784381,
                                    "cache_read_tokens": 60990233,
                                    "cache_creation_tokens": 0,
                                    "total_tokens": 74608223,
                                    "spend": 17.075957475000003
                                },
                                {
                                    "model": "claude-opus-5",
                                    "requests": 13,
                                    "input_tokens": 428674,
                                    "output_tokens": 19855,
                                    "cache_read_tokens": 0,
                                    "cache_creation_tokens": 0,
                                    "total_tokens": 448529,
                                    "spend": 2.9037195
                                }
                            ],
                            "total_spend": 87.2418,
                            "total_requests": 2849,
                            "total_tokens": 104542994
                        },
                        {
                            "cycle": "2026-09",
                            "by_model": [],
                            "total_spend": 0.0,
                            "total_requests": 0,
                            "total_tokens": 0
                        },
                        {
                            "cycle": "2026-08",
                            "by_model": [],
                            "total_spend": 0.0,
                            "total_requests": 0,
                            "total_tokens": 0
                        }
                    ]
                }
                """;
        var response = (ApiResponse.Success) DashboardCodeassClient.parseApiResponse(200, json);
        assertEquals(new BigDecimal("29.29757300749999"), response.spend());

        var details = response.details();
        assertNotNull(details);
        assertEquals(new BigDecimal("200.0"), details.maxBudget());
        assertEquals(1817, details.totalRequests());
        assertEquals(133990386L, details.totalTokens());
        assertEquals("2027-01-12T21:20:57.926000Z", details.expires());

        assertEquals(2, details.byModel().size());
        var firstModel = details.byModel().get(0);
        assertEquals("gemini-3.8-flash", firstModel.model());
        assertEquals(1545, firstModel.requests());
        assertEquals(new BigDecimal("15.664931017499997"), firstModel.spend());
        assertEquals("15.66", firstModel.spendStr());
        assertEquals(100231010L, firstModel.totalTokens());

        var secondModel = details.byModel().get(1);
        assertEquals("gemini-4-argon", secondModel.model());
        assertEquals(272, secondModel.requests());
        assertEquals(new BigDecimal("13.632641989999994"), secondModel.spend());
        assertEquals("13.63", secondModel.spendStr());
        assertEquals(33759376L, secondModel.totalTokens());

        assertNotNull(details.history());
        assertEquals(3, details.history().size());

        var octHistory = details.history().get(0);
        assertEquals("2026-10", octHistory.cycle());
        assertEquals(new BigDecimal("87.2418"), octHistory.spend());
        assertEquals("87.24", octHistory.spendStr());
        assertEquals(2849, octHistory.requests());
        assertEquals(104542994L, octHistory.totalTokens());
        assertEquals(3, octHistory.byModel().size());
        assertEquals("claude-sonnet-5", octHistory.byModel().get(0).model());
        assertEquals(300, octHistory.byModel().get(0).requests());

        var sepHistory = details.history().get(1);
        assertEquals("2026-09", sepHistory.cycle());
        assertEquals(new BigDecimal("0.0"), sepHistory.spend());
        assertEquals(0, sepHistory.requests());

        var augHistory = details.history().get(2);
        assertEquals("2026-08", augHistory.cycle());
        assertEquals(new BigDecimal("0.0"), augHistory.spend());
        assertEquals(0, augHistory.requests());
    }
}
