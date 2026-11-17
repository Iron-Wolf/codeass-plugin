package codeassclient;

import main.ApiResponse;

/**
 * Common client interface to retrieve spend quota data.
 */
public interface CodeassClient {

    ApiResponse fetchAndGetResult();
}
