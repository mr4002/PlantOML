package com.plantoml.plantomlserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for REST client settings.
 * <p>
 * This class defines beans and configuration settings used across the application,
 * specifically related to RESTful web services.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates and configures a {@link RestTemplate} bean.
     * <p>
     * The {@link RestTemplate} is used for making HTTP requests to external services.
     * This bean can be autowired into other components where RESTful requests are needed.
     *
     * @return A {@link RestTemplate} instance.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
