package com.audition.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI auditionOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Audition API")
            .version("0.0.1")
            .description("Posts and their comments, retrieved from JSONPlaceholder. Errors are returned as "
                + "RFC 7807 problem details; a failing upstream is reported as 502 (error) or 503 (unreachable)."));
    }

}
