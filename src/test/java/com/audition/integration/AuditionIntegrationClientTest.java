package com.audition.integration;

import static com.audition.constant.ErrorMessage.HTTP_CLIENT_ERROR;
import static com.audition.constant.ErrorMessage.RESOURCE_NOT_FOUND;
import static com.audition.constant.ErrorMessage.UNEXPECTED_ERROR;
import static com.audition.constant.ErrorMessage.UPSTREAM_ERROR;
import static com.audition.constant.ErrorMessage.UPSTREAM_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.audition.common.exception.SystemException;
import com.audition.model.AuditionPost;
import com.audition.model.Comment;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class AuditionIntegrationClientTest {

    private static final String BASE = "http://upstream.test";
    private static final String POST_JSON = "{\"userId\":1,\"id\":1,\"title\":\"first\",\"body\":\"first body\"}";
    private static final String POSTS_JSON = "[" + POST_JSON
        + ",{\"userId\":2,\"id\":2,\"title\":\"second\",\"body\":\"second body\"}]";
    private static final String COMMENTS_JSON =
        "[{\"postId\":1,\"id\":10,\"name\":\"n1\",\"email\":\"a@b.c\",\"body\":\"c1\"},"
            + "{\"postId\":1,\"id\":11,\"name\":\"n2\",\"email\":\"d@e.f\",\"body\":\"c2\"}]";

    private MockRestServiceServer server;
    private AuditionIntegrationClient client;

    @BeforeEach
    void setUp() {
        final RestTemplate restTemplate = new RestTemplateBuilder().rootUri(BASE).build();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new AuditionIntegrationClient(restTemplate);
    }

    @Test
    void getPosts_returnsTheParsedList() {
        server.expect(requestTo(BASE + "/posts")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(POSTS_JSON, MediaType.APPLICATION_JSON));

        final List<AuditionPost> posts = client.getPosts();

        assertThat(posts).extracting(AuditionPost::getId).containsExactly(1, 2);
        assertThat(posts.get(0).getTitle()).isEqualTo("first");
        server.verify();
    }

    @Test
    void getPostById_returnsTheParsedPost() {
        server.expect(requestTo(BASE + "/posts/1")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(POST_JSON, MediaType.APPLICATION_JSON));

        final AuditionPost post = client.getPostById("1");

        assertThat(post.getId()).isEqualTo(1);
        assertThat(post.getUserId()).isEqualTo(1);
        assertThat(post.getBody()).isEqualTo("first body");
        server.verify();
    }

    @Test
    void getCommentsForPost_callsThePostsCommentsPath() {
        server.expect(requestTo(BASE + "/posts/1/comments")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(COMMENTS_JSON, MediaType.APPLICATION_JSON));

        final List<Comment> comments = client.getCommentsForPost("1");

        assertThat(comments).extracting(Comment::getId).containsExactly(10, 11);
        assertThat(comments.get(0).getEmail()).isEqualTo("a@b.c");
        server.verify();
    }

    @Test
    void getCommentsByPostId_callsTheCommentsPathWithTheQueryParameter() {
        server.expect(requestTo(BASE + "/comments?postId=1")).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(COMMENTS_JSON, MediaType.APPLICATION_JSON));

        assertThat(client.getCommentsByPostId("1")).hasSize(2);
        server.verify();
    }

    @Test
    void emptyListsAreReturnedAsEmptyLists() {
        server.expect(requestTo(BASE + "/comments?postId=1"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.getCommentsByPostId("1")).isEmpty();
    }

    @Test
    void anEmptyBodyForAListIsTreatedAsAnEmptyList() {
        server.expect(requestTo(BASE + "/posts")).andRespond(withSuccess());

        assertThat(client.getPosts()).isEmpty();
    }

    // ---- upstream failures ----

    @Test
    void upstream404BecomesNotFoundAndKeepsTheOriginalCause() {
        server.expect(requestTo(BASE + "/posts/9999")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getPostById("9999"))
            .isInstanceOfSatisfying(SystemException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(404);
                assertThat(e.getTitle()).isEqualTo(RESOURCE_NOT_FOUND);
                assertThat(e.getMessage()).contains("post with id 9999");
                assertThat(e.getCause()).isInstanceOf(HttpClientErrorException.class);
            });
    }

    @Test
    void upstream400KeepsItsStatusAsAClientError() {
        server.expect(requestTo(BASE + "/posts/1")).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.getPostById("1"))
            .isInstanceOfSatisfying(SystemException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(400);
                assertThat(e.getTitle()).isEqualTo(HTTP_CLIENT_ERROR);
            });
    }

    @Test
    void upstream429KeepsItsStatusAsAClientError() {
        server.expect(requestTo(BASE + "/posts/1")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.getPostById("1"))
            .isInstanceOfSatisfying(SystemException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(429);
                assertThat(e.getTitle()).isEqualTo(HTTP_CLIENT_ERROR);
            });
    }

    @Test
    void upstream500BecomesBadGateway() {
        server.expect(requestTo(BASE + "/posts")).andRespond(withServerError());

        assertThatThrownBy(() -> client.getPosts())
            .isInstanceOfSatisfying(SystemException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(502);
                assertThat(e.getTitle()).isEqualTo(UPSTREAM_ERROR);
                assertThat(e.getCause()).isInstanceOf(HttpServerErrorException.class);
            });
    }

    @Test
    void upstream503IsStillReportedAsBadGateway() {
        server.expect(requestTo(BASE + "/posts/1/comments")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getCommentsForPost("1"))
            .isInstanceOfSatisfying(SystemException.class, e -> assertThat(e.getStatusCode()).isEqualTo(502));
    }

    @Test
    void connectionFailureBecomesServiceUnavailable() {
        server.expect(requestTo(BASE + "/comments?postId=1"))
            .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> client.getCommentsByPostId("1"))
            .isInstanceOfSatisfying(SystemException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(503);
                assertThat(e.getTitle()).isEqualTo(UPSTREAM_UNAVAILABLE);
                assertThat(e.getCause()).isInstanceOf(ResourceAccessException.class);
            });
    }

    @Test
    void malformedJsonBecomesAnUnexpectedError() {
        server.expect(requestTo(BASE + "/posts/1"))
            .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getPostById("1"))
            .isInstanceOfSatisfying(SystemException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(500);
                assertThat(e.getTitle()).isEqualTo(UNEXPECTED_ERROR);
            });
    }

    @Test
    void anEmptyBodyForASinglePostBecomesBadGateway() {
        server.expect(requestTo(BASE + "/posts/1")).andRespond(withSuccess());

        assertThatThrownBy(() -> client.getPostById("1"))
            .isInstanceOfSatisfying(SystemException.class, e -> {
                assertThat(e.getStatusCode()).isEqualTo(502);
                assertThat(e.getTitle()).isEqualTo(UPSTREAM_ERROR);
            });
    }

}
