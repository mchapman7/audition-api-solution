package com.audition.service;

import com.audition.integration.AuditionIntegrationClient;
import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostWithComments;
import com.audition.model.Comment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditionService {

    @Autowired
    private AuditionIntegrationClient auditionIntegrationClient;


    public List<AuditionPost> getPosts() {
        return auditionIntegrationClient.getPosts();
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
}
