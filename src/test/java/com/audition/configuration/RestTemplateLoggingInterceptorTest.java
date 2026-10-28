package com.audition.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import com.audition.common.logging.AuditionLogger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

@ExtendWith(MockitoExtension.class)
class RestTemplateLoggingInterceptorTest {

    private static final URI URL = URI.create("http://upstream.test/posts/1");

    @Mock
    private AuditionLogger auditionLogger;
    @Mock
    private HttpRequest request;
    @Mock
    private ClientHttpRequestExecution execution;
    @Mock
    private ClientHttpResponse response;

    private RestTemplateLoggingInterceptor interceptor() {
        return new RestTemplateLoggingInterceptor(auditionLogger);
    }

    @Test
    void logsTheRequestAndTheResponseStatusAndReturnsTheResponseUnchanged() throws IOException {
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URL);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        final byte[] body = new byte[0];
        when(execution.execute(request, body)).thenReturn(response);

        final ClientHttpResponse result = interceptor().intercept(request, body, execution);

        assertThat(result).isSameAs(response);
        final ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(auditionLogger, atLeastOnce()).info(org.mockito.ArgumentMatchers.any(), messages.capture());
        assertThat(messages.getAllValues())
            .anyMatch(m -> m.contains("Outgoing request") && m.contains("GET") && m.contains(URL.toString()))
            .anyMatch(m -> m.contains("Response") && m.contains("200") && m.contains(" ms"));
    }

    @Test
    void doesNotLogBodiesAtInfoLevel() throws IOException {
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(URL);
        when(response.getStatusCode()).thenReturn(HttpStatus.CREATED);
        final byte[] body = "secret-payload".getBytes(StandardCharsets.UTF_8);
        when(execution.execute(request, body)).thenReturn(response);

        interceptor().intercept(request, body, execution);

        final ArgumentCaptor<String> infoMessages = ArgumentCaptor.forClass(String.class);
        verify(auditionLogger, atLeastOnce()).info(org.mockito.ArgumentMatchers.any(), infoMessages.capture());
        assertThat(infoMessages.getAllValues()).noneMatch(message -> message.contains("secret-payload"));
    }

    @Test
    void logsTruncatedBodiesAtDebugLevel() throws IOException {
        final ch.qos.logback.classic.Logger interceptorLogger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RestTemplateLoggingInterceptor.class);
        final Level original = interceptorLogger.getLevel();
        interceptorLogger.setLevel(Level.DEBUG);
        try {
            final byte[] requestBody = "request-payload".getBytes(StandardCharsets.UTF_8);
            final String longResponse = "x".repeat(2_000);
            when(request.getMethod()).thenReturn(HttpMethod.POST);
            when(request.getURI()).thenReturn(URL);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            when(response.getBody()).thenReturn(
                new ByteArrayInputStream(longResponse.getBytes(StandardCharsets.UTF_8)));
            when(execution.execute(request, requestBody)).thenReturn(response);

            interceptor().intercept(request, requestBody, execution);

            final ArgumentCaptor<String> debug = ArgumentCaptor.forClass(String.class);
            verify(auditionLogger, atLeastOnce()).debug(org.mockito.ArgumentMatchers.any(), debug.capture());
            assertThat(debug.getAllValues())
                .anyMatch(m -> m.contains("request-payload"))
                .anyMatch(m -> m.startsWith("Response body: ") && m.endsWith("...") && m.length() < 1_100);
        } finally {
            interceptorLogger.setLevel(original);
        }
    }

    @Test
    void propagatesFailuresFromTheCallWithoutLoggingAResponse() throws IOException {
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URL);
        final byte[] body = new byte[0];
        when(execution.execute(request, body)).thenThrow(new IOException("Connection refused"));

        assertThatThrownBy(() -> interceptor().intercept(request, body, execution))
            .isInstanceOf(IOException.class)
            .hasMessage("Connection refused");

        final ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(auditionLogger, atLeastOnce()).info(org.mockito.ArgumentMatchers.any(), messages.capture());
        assertThat(messages.getAllValues()).noneMatch(m -> m.startsWith("Response:"));
    }
}
