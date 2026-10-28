package com.audition.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.audition.common.logging.AuditionLogger;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostWithComments;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class WebServiceConfigurationTest {

    private final ObjectMapper mapper = new WebServiceConfiguration().objectMapper();

    @Test
    void omitsNullValues() throws Exception {
        final AuditionPost post = new AuditionPost();
        post.setId(1);
        post.setUserId(2);

        final String json = mapper.writeValueAsString(post);

        assertThat(json).contains("\"userId\":2").contains("\"id\":1")
            .doesNotContain("title").doesNotContain("body");
    }

    @Test
    void alwaysWritesTheCommentsFieldEvenWhenEmpty() throws Exception {
        final String json = mapper.writeValueAsString(AuditionPostWithComments.of(new AuditionPost(), List.of()));

        assertThat(json).contains("\"comments\":[]");
    }

    @Test
    void ignoresUnknownPropertiesWhenReading() throws Exception {
        final AuditionPost post = mapper.readValue("{\"id\":5,\"somethingNew\":true}", AuditionPost.class);

        assertThat(post.getId()).isEqualTo(5);
    }

    @Test
    void writesDatesAsTextNotTimestamps() throws Exception {
        final String json = mapper.writeValueAsString(Map.of("date", LocalDate.of(2024, 1, 15)));

        assertThat(json).isEqualTo("{\"date\":\"2024-01-15\"}");
    }

    @Test
    void restTemplateUsesTheBaseUrlTheLoggingInterceptorAndTheConfiguredMapper() {
        final RestTemplateLoggingInterceptor interceptor =
            new RestTemplateLoggingInterceptor(new AuditionLogger());
        final RestTemplate restTemplate = new WebServiceConfiguration().restTemplate(
            new RestTemplateBuilder(), mapper, interceptor, "http://upstream.test", 1_000, 2_000);

        assertThat(restTemplate.getInterceptors()).contains(interceptor);

        final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://upstream.test/posts/1"))
            .andRespond(withSuccess("{\"id\":1,\"userId\":2,\"unknownField\":true}", MediaType.APPLICATION_JSON));

        final AuditionPost post = restTemplate.getForObject("/posts/1", AuditionPost.class);

        assertThat(post.getId()).isEqualTo(1);
        assertThat(post.getUserId()).isEqualTo(2);
        server.verify();
    }
}
