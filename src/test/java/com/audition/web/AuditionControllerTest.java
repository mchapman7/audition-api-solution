package com.audition.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.audition.common.logging.AuditionLogger;
import com.audition.configuration.WebServiceConfiguration;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostWithComments;
import com.audition.model.Comment;
import com.audition.service.AuditionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuditionController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = WebServiceConfiguration.class))
@Import(AuditionLogger.class)
public class AuditionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditionService auditionService;

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
        comment.setName("name");
        comment.setEmail("user@example.com");
        comment.setBody("comment body");
        return comment;
    }

    @Test
    void getPostsWithoutFiltersReturnsTheList() throws Exception {
        when(auditionService.getPosts(null, null, null)).thenReturn(List.of(post(1, 1, "title one", "body one")));

        mockMvc.perform(get("/posts"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].userId").value(1))
            .andExpect(jsonPath("$[0].title").value("title one"));
    }

    @Test
    void getPostsPassesTheFiltersToTheService() throws Exception {
        when(auditionService.getPosts(1, "qui", "est")).thenReturn(List.of(post(1, 2, "qui", "est")));

        mockMvc.perform(get("/posts").param("userId", "1").param("title", "qui").param("body", "est"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
        verify(auditionService).getPosts(1, "qui", "est");
    }

    @Test
    void getPostByIdReturnsThePost() throws Exception {
        when(auditionService.getPostById("1")).thenReturn(post(3, 1, "the title", "the body"));

        mockMvc.perform(get("/posts/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(3))
            .andExpect(jsonPath("$.title").value("the title"));
    }

    @Test
    void getPostByIdAcceptsTheLargestValidId() throws Exception {
        when(auditionService.getPostById("999999999")).thenReturn(post(1, 1, "t", "b"));

        mockMvc.perform(get("/posts/999999999")).andExpect(status().isOk());
    }

    @Test
    void getPostWithComments_returnsThePostWithItsCommentsEmbedded() throws Exception {
        when(auditionService.getPostWithComments("1"))
            .thenReturn(AuditionPostWithComments.of(post(1, 1, "t", "b"), List.of(comment(1, 5))));

        mockMvc.perform(get("/posts/1/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.comments", hasSize(1)))
            .andExpect(jsonPath("$.comments[0].id").value(5))
            .andExpect(jsonPath("$.comments[0].email").value("user@example.com"));
    }

    @Test
    void getCommentsByPostId_returnsAPlainList() throws Exception {
        when(auditionService.getCommentsByPostId("1")).thenReturn(List.of(comment(1, 1), comment(1, 2)));

        mockMvc.perform(get("/comments").param("postId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].postId").value(1));
    }

}
