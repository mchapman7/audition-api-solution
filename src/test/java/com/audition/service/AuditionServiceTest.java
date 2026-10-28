package com.audition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audition.common.exception.SystemException;
import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostWithComments;
import com.audition.model.Comment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditionServiceTest {

    private static final String TITLE_1 = "title1";
    private static final String TITLE_2 = "title2";
    private static final String BODY_1 = "body1";
    private static final String BODY_2 = "body2";

    @Mock
    private AuditionIntegrationClient client;

    private AuditionService service;
    private AuditionPost post1;
    private AuditionPost post2;

    @BeforeEach
    void setUp() {
        service = new AuditionService(client);
        post1 = post(1, 1, TITLE_1, BODY_1);
        post2 = post(2, 2, TITLE_2, BODY_2);
    }

    private static AuditionPost post(final int userId, final int id, final String title, final String body) {
        final AuditionPost post = new AuditionPost();
        post.setUserId(userId);
        post.setId(id);
        post.setTitle(title);
        post.setBody(body);

        return post;
    }

    private static Comment comment(final int postId, final int id) {
        final Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setId(id);
        comment.setName("name " + id);
        comment.setEmail("user" + id + "@example.com");
        comment.setBody("comment body " + id);

        return comment;
    }

    private void givenUpstreamPosts(final AuditionPost... posts) {
        when(client.getPosts()).thenReturn(List.of(posts));
    }

    // ---- filtering ----
    @Test
    void getPostsWithoutFiltersReturnsAllPosts() {
        givenUpstreamPosts(post1, post2);

        assertThat(service.getPosts(null, null, null)).containsExactly(post1, post2);
    }

    @Test
    void getPostsFiltersByUserId() {
        givenUpstreamPosts(post1, post2);

        assertThat(service.getPosts(1, null, null)).containsExactly(post1);
    }

    @Test
    void getPostsFiltersByTitle() {
        givenUpstreamPosts(post1, post2);

        assertThat(service.getPosts(null, TITLE_1, null)).containsExactly(post1);
    }

    @Test
    void getPostsTitleMatchIsCaseInsensitiveTrimmedAndContains() {
        givenUpstreamPosts(post1, post2);

        assertThat(service.getPosts(null, "  TITLE ", null)).containsExactly(post1, post2);
    }

    @Test
    void getPostsFiltersByBody() {
        givenUpstreamPosts(post1, post2);

        assertThat(service.getPosts(null, null, BODY_2)).containsExactly(post2);
    }

    @Test
    void getPostsCombinesFiltersWithAnd() {
        givenUpstreamPosts(post1, post2);

        assertThat(service.getPosts(2, TITLE_2, BODY_2)).containsExactly(post2);
        assertThat(service.getPosts(1, TITLE_2, null)).isEmpty();
    }

    @Test
    void getPostsReturnsEmptyListWhenNothingMatches() {
        givenUpstreamPosts(post1, post2);

        assertThat(service.getPosts(null, "no such title", null)).isEmpty();
    }

    @Test
    void getPostsIgnoresBlankFilters() {
        givenUpstreamPosts(post1, post2);

        assertThat(service.getPosts(null, "   ", "")).containsExactly(post1, post2);
    }

    @Test
    void getPostsExcludesPostsWithNullTextInsteadOfFailing() {
        final AuditionPost noText = post(3, 3, null, null);
        givenUpstreamPosts(post1, noText);

        assertThat(service.getPosts(null, TITLE_1, null)).containsExactly(post1);
        assertThat(service.getPosts(null, null, BODY_1)).containsExactly(post1);
    }

    @Test
    void getPostsReturnsEmptyListWhenTheUpstreamHasNoPosts() {
        givenUpstreamPosts();

        assertThat(service.getPosts(null, null, null)).isEmpty();
    }

    // ---- single post ----
    @Test
    void getPostByIdReturnsThePostFromTheClient() {
        when(client.getPostById("1")).thenReturn(post1);

        assertThat(service.getPostById("1")).isSameAs(post1);
    }

    @Test
    void getPostByIdPropagatesClientFailures() {
        when(client.getPostById("9999")).thenThrow(new SystemException("not found", "Resource Not Found", 404));

        assertThatThrownBy(() -> service.getPostById("9999"))
            .isInstanceOfSatisfying(SystemException.class, e -> assertThat(e.getStatusCode()).isEqualTo(404));
    }

    // ---- comments ----

    @Test
    void getPostWithCommentsEmbedsTheCommentsInThePost() {
        when(client.getPostById("1")).thenReturn(post1);
        when(client.getCommentsForPost("1")).thenReturn(List.of(comment(1, 10), comment(1, 11)));

        final AuditionPostWithComments result = service.getPostWithComments("1");

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getTitle()).isEqualTo(TITLE_1);
        assertThat(result.getComments()).extracting(Comment::getId).containsExactly(10, 11);
    }

    @Test
    void getPostWithCommentsWithNoCommentsReturnsAnEmptyList() {
        when(client.getPostById("1")).thenReturn(post1);
        when(client.getCommentsForPost("1")).thenReturn(List.of());

        assertThat(service.getPostWithComments("1").getComments()).isEmpty();
    }

    @Test
    void getPostWithCommentsWhenThePostIsMissingFailsWithoutFetchingComments() {
        when(client.getPostById("9999")).thenThrow(new SystemException("not found", "Resource Not Found", 404));

        assertThatThrownBy(() -> service.getPostWithComments("9999")).isInstanceOf(SystemException.class);
        verify(client, never()).getCommentsForPost("9999");
    }

    @Test
    void getCommentsByPostIdReturnsTheClientsList() {
        final List<Comment> expected = List.of(comment(1, 1));
        when(client.getCommentsByPostId("1")).thenReturn(expected);

        assertThat(service.getCommentsByPostId("1")).isSameAs(expected);
    }
}
