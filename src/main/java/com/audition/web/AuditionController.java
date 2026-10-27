package com.audition.web;

import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostWithComments;
import com.audition.model.Comment;
import com.audition.service.AuditionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuditionController {

    private final AuditionService auditionService;

    // TODO Add a query param that allows data filtering. The intent of the filter is at developers discretion.
    @GetMapping(value = "/posts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditionPost> getPosts() {

        // TODO Add logic that filters response data based on the query param

        return auditionService.getPosts();
    }

    @GetMapping(value = "/posts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPostById(@PathVariable("id") final String postId) {

        // TODO Add input validation

        return auditionService.getPostById(postId);
    }

    // TODO Add additional methods to return comments for each post. Hint: Check https://jsonplaceholder.typicode.com/

    /**
     * The post with its comments embedded.
     */
    @GetMapping(value = "/posts/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditionPostWithComments getPostWithComments(@PathVariable("id") final String postId) {
        return auditionService.getPostWithComments(postId);
    }

    /**
     * Comments for a post as a separate list.
     */
    @GetMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Comment> getCommentsByPostId(@RequestParam("postId") final String postId) {
        return auditionService.getCommentsByPostId(postId);
    }
}
