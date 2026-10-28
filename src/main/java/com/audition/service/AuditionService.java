package com.audition.service;

import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostWithComments;
import com.audition.model.Comment;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AuditionService {

    private AuditionIntegrationClient auditionIntegrationClient;

    public AuditionService(final AuditionIntegrationClient auditionIntegrationClient) {
        this.auditionIntegrationClient = auditionIntegrationClient;
    }


    public List<AuditionPost> getPosts(final Integer userId, final String title, final String body) {

        final String titleFilter = normalise(title);
        final String bodyFilter = normalise(body);
        return auditionIntegrationClient.getPosts().stream()
            .filter(post -> userId == null || post.getUserId() == userId)
            .filter(post -> containsIgnoreCase(post.getTitle(), titleFilter))
            .filter(post -> containsIgnoreCase(post.getBody(), bodyFilter))
            .toList();
    }

    public AuditionPost getPostById(final String postId) {
        return auditionIntegrationClient.getPostById(postId);
    }

    /**
     * Returns the post with its comments embedded. The post is fetched first, so a missing post produces a 404 instead
     * of an empty comment list (the upstream comments call returns 200 with [] for unknown posts).
     */
    public AuditionPostWithComments getPostWithComments(final String postId) {
        final AuditionPost post = auditionIntegrationClient.getPostById(postId);
        final List<Comment> comments = auditionIntegrationClient.getCommentsForPost(postId);
        return AuditionPostWithComments.of(post, comments);
    }

    /**
     * Comments for a post as a separate list (the /comments?postId= call).
     */
    public List<Comment> getCommentsByPostId(final String postId) {
        return auditionIntegrationClient.getCommentsByPostId(postId);
    }

    private static String normalise(final String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsIgnoreCase(final String field, final String filter) {
        return filter.isEmpty() || field != null && field.toLowerCase(Locale.ROOT).contains(filter);
    }
}
