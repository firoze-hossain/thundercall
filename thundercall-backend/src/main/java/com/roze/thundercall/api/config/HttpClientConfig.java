package com.roze.thundercall.api.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP client used to execute the user's API requests.
 *
 * IMPORTANT: remove the existing restTemplate() @Bean from WebSecurityConfig
 * (lines ~50-52), otherwise Spring will fail with a duplicate-bean error.
 *
 * Why this exists:
 *  - Timeouts: the default RestTemplate waits forever. One request to a
 *    hanging server would block a worker thread indefinitely.
 *  - No-throw error handler: Postman shows you 404/500 responses with their
 *    body and headers. The default handler throws exceptions on 4xx/5xx,
 *    which loses response headers and complicates the flow. With this
 *    handler, every response (2xx or 5xx) travels the same happy path in
 *    RequestServiceImpl.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();

        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false; // never treat any status as an error
            }

            @Override
            public void handleError(ClientHttpResponse response) {
                // no-op
            }
        });

        return restTemplate;
    }
}
