package com.cibc.cai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Wires the {@link RestClient} used to call the external identity provider.
 *
 * <p>The blocking {@code RestClient} is preferred over {@code WebClient} here
 * because the callout runs on virtual threads ({@code spring.threads.virtual})
 * from an {@code @Async} executor, so blocking I/O is cheap.
 */
@Configuration
@EnableConfigurationProperties(IdentityProviderProperties.class)
public class IdentityProviderConfig {

    @Bean
    public RestClient identityProviderRestClient(IdentityProviderProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAuthToken())
                .build();
    }
}
