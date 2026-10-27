package com.audition.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebServiceConfiguration implements WebMvcConfigurer {

    private static final String YEAR_MONTH_DAY_PATTERN = "yyyy-MM-dd";

    @Bean
    public ObjectMapper objectMapper() {

        return Jackson2ObjectMapperBuilder.json()
            .simpleDateFormat(YEAR_MONTH_DAY_PATTERN)
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .serializationInclusion(JsonInclude.Include.NON_EMPTY)
            .featuresToDisable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
        // TODO configure Jackson Object mapper that
        //  1. allows for date format as yyyy-MM-dd
        //  2. Does not fail on unknown properties
        //  3. maps to camelCase
        //  4. Does not include null values or empty values
        //  5. does not write datas as timestamps.
        //return new ObjectMapper();
    }

    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder builder,
        final ObjectMapper objectMapper,
        @Value("${audition.client.base-url}") final String baseUrl,
        @Value("${audition.client.connect-timeout-ms}") final int connectTimeoutMs,
        @Value("${audition.client.read-timeout-ms}") final int readTimeoutMs) {

//        final RestTemplate restTemplate = new RestTemplate(
//            new BufferingClientHttpRequestFactory(createClientFactory()));

        return builder
            .rootUri(baseUrl)
            .requestFactory(() ->
                new BufferingClientHttpRequestFactory(createClientFactory(connectTimeoutMs, readTimeoutMs)))
            .messageConverters(
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
        // TODO use object mapper
        // TODO create a logging interceptor that logs request/response for rest template calls.

        //return restTemplate;
    }

    private SimpleClientHttpRequestFactory createClientFactory(final int connectTimeoutMs,
        final int readTimeoutMs) {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setOutputStreaming(false);
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return requestFactory;
    }
}
