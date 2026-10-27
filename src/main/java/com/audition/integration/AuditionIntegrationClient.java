package com.audition.integration;

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

    public static final String HTTP_CLIENT_ERROR = "HTTP Client Error";
    public static final String RESOURCE_NOT_FOUND = "Resource Not Found";
    public static final String UPSTREAM_ERROR = "Upstream Service Error";
    public static final String UPSTREAM_UNAVAILABLE = "Upstream Service Unavailable";
    public static final String UNEXPECTED_ERROR = "Unexpected Error";

    private static final ParameterizedTypeReference<List<AuditionPost>> POST_LIST =
        new ParameterizedTypeReference<>() {
        };
    private static final ParameterizedTypeReference<List<Comment>> COMMENT_LIST =
        new ParameterizedTypeReference<>() {
        };
    private final RestTemplate restTemplate;


    public List<AuditionPost> getPosts() {
        // TODO make RestTemplate call to get Posts from https://jsonplaceholder.typicode.com/posts
        return execute("fetching posts", () -> orEmpty(
            restTemplate.exchange("/posts", HttpMethod.GET, null, POST_LIST).getBody()));
    }

    public AuditionPost getPostById(final String id) {
        // TODO get post by post ID call from https://jsonplaceholder.typicode.com/posts/
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
        // TODO Write a method GET comments for a post from https://jsonplaceholder.typicode.com/posts/{postId}/comments - the comments must be returned as part of the post.
        return execute("fetching comments for post with id " + postId, () -> orEmpty(
            restTemplate.exchange("/posts/{postId}/comments", HttpMethod.GET, null, COMMENT_LIST, postId)
                .getBody()));
    }

    /**
     * Comments via /comments?postId={postId}, returned as a separate list.
     */
    public List<Comment> getCommentsByPostId(final String postId) {
        // TODO write a method. GET comments for a particular Post from https://jsonplaceholder.typicode.com/comments?postId={postId}.
        // The comments are a separate list that needs to be returned to the API consumers. Hint: this is not part of the AuditionPost pojo.
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
            final boolean notFound = HttpStatus.NOT_FOUND.equals(e.getStatusCode());
            throw new SystemException("Error " + action + ": " + e.getMessage(),
                notFound ? RESOURCE_NOT_FOUND : HTTP_CLIENT_ERROR, e.getStatusCode().value(), e);
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
