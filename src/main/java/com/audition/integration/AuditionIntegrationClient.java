package com.audition.integration;

import static com.audition.constant.ErrorMessage.HTTP_CLIENT_ERROR;
import static com.audition.constant.ErrorMessage.RESOURCE_NOT_FOUND;
import static com.audition.constant.ErrorMessage.UNEXPECTED_ERROR;
import static com.audition.constant.ErrorMessage.UPSTREAM_ERROR;
import static com.audition.constant.ErrorMessage.UPSTREAM_UNAVAILABLE;

import com.audition.common.exception.SystemException;
import com.audition.model.AuditionPost;
import com.audition.model.Comment;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AuditionIntegrationClient {

    private static final ParameterizedTypeReference<List<AuditionPost>> POST_LIST =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<List<Comment>> COMMENT_LIST =
        new ParameterizedTypeReference<>() {
        };
    private final RestTemplate restTemplate;


    public List<AuditionPost> getPosts() {

        return execute("fetching posts", () -> orEmpty(
            restTemplate.exchange("/posts", HttpMethod.GET, null, POST_LIST).getBody()));
    }

    public AuditionPost getPostById(final String id) {

        final AuditionPost post = execute("fetching post with id " + id,
            () -> restTemplate.getForObject("/posts/{id}", AuditionPost.class, id));
        if (post == null) {
            throw new SystemException("Empty response fetching post with id " + id, UPSTREAM_ERROR,
                HttpStatus.BAD_GATEWAY.value());
        }
        return post;
    }

    /**
     * Comments via /posts/{postId}/comments (the service embeds them in the post).
     */
    public List<Comment> getCommentsForPost(final String postId) {

        return execute("fetching comments for post with id " + postId, () -> orEmpty(
            restTemplate.exchange("/posts/{postId}/comments", HttpMethod.GET, null, COMMENT_LIST, postId)
                .getBody()));
    }

    /**
     * Comments via /comments?postId={postId}, returned as a separate list.
     */
    public List<Comment> getCommentsByPostId(final String postId) {

        return execute("fetching comments with postId " + postId, () -> orEmpty(
            restTemplate.exchange("/comments?postId={postId}", HttpMethod.GET, null, COMMENT_LIST, postId)
                .getBody()));
    }

    /**
     * Runs an upstream call and maps failures to SystemException, keeping the message and the cause.
     */
    private <T> T execute(final String action, final Supplier<T> call) {
        try {
            return call.get();
        } catch (final HttpClientErrorException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new SystemException("Resource not found while " + action, RESOURCE_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value(), e);
            }
            throw new SystemException("Error " + action + ": " + e.getMessage(), HTTP_CLIENT_ERROR,
                e.getStatusCode().value(), e);
        } catch (final HttpServerErrorException e) {
            throw new SystemException("Upstream failure " + action + ": " + e.getMessage(),
                UPSTREAM_ERROR, HttpStatus.BAD_GATEWAY.value(), e);
        } catch (final ResourceAccessException e) {
            throw new SystemException("Upstream unreachable " + action + ": " + e.getMessage(),
                UPSTREAM_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        } catch (final RestClientException e) {
            throw new SystemException("Unexpected error " + action + ": " + e.getMessage(),
                UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    private static <T> List<T> orEmpty(final List<T> list) {
        return Optional.ofNullable(list).orElse(Collections.emptyList());
    }
}
