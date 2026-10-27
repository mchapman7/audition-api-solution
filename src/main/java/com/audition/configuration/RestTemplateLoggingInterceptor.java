package com.audition.configuration;

import com.audition.common.logging.AuditionLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateLoggingInterceptor.class);
    private static final int MAX_BODY_LENGTH = 1_000;

    private final AuditionLogger auditionLogger;

    public RestTemplateLoggingInterceptor(final AuditionLogger auditionLogger) {
        this.auditionLogger = auditionLogger;
    }

    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
        final ClientHttpRequestExecution execution) throws IOException {

        auditionLogger.info(LOG,
            String.format("Outgoing request: %s %s", request.getMethod(), request.getURI()));
        if (LOG.isDebugEnabled()) {
            auditionLogger.debug(LOG,
                "Request body: " + abbreviate(new String(body, StandardCharsets.UTF_8)));
        }

        final long start = System.nanoTime();
        final ClientHttpResponse response = execution.execute(request, body);
        final long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        auditionLogger.info(LOG, String.format("Response: %s %s -> %s in %d ms",
            request.getMethod(), request.getURI(), response.getStatusCode(), elapsedMs));
        if (LOG.isDebugEnabled()) {
            auditionLogger.debug(LOG, "Response body: "
                + abbreviate(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)));
        }
        return response;
    }

    private static String abbreviate(final String text) {
        return text.length() > MAX_BODY_LENGTH ? text.substring(0, MAX_BODY_LENGTH) + "..." : text;
    }

}
