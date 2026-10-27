package com.audition.web;

import static com.audition.constant.ErrorMessage.POSITIVE_INTEGER_MESSAGE;
import static com.audition.constant.ValidationRules.MAX_FILTER_LENGTH;
import static com.audition.constant.ValidationRules.POSITIVE_INTEGER;

import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostWithComments;
import com.audition.model.Comment;
import com.audition.service.AuditionService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class AuditionController {

    private final AuditionService auditionService;

    @GetMapping(value = "/posts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditionPost> getPosts(@RequestParam(value = "userId", required = false) @Positive final Integer userId,
        @RequestParam(value = "title", required = false) @Size(max = MAX_FILTER_LENGTH) final String title,
        @RequestParam(value = "body", required = false) @Size(max = MAX_FILTER_LENGTH) final String body) {

        return auditionService.getPosts(userId, title, body);
    }

    @GetMapping(value = "/posts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPostById(
        @PathVariable("id") @Pattern(regexp = POSITIVE_INTEGER, message = POSITIVE_INTEGER_MESSAGE) final String postId) {

        return auditionService.getPostById(postId);
    }

    /**
     * The post with its comments embedded.
     */
    @GetMapping(value = "/posts/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditionPostWithComments getPostWithComments(
        @PathVariable("id") @Pattern(regexp = POSITIVE_INTEGER, message = POSITIVE_INTEGER_MESSAGE) final String postId) {

        return auditionService.getPostWithComments(postId);
    }

    /**
     * Comments for a post as a separate list.
     */
    @GetMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Comment> getCommentsByPostId(
        @RequestParam("postId") @Pattern(regexp = POSITIVE_INTEGER, message = POSITIVE_INTEGER_MESSAGE) final String postId) {

        return auditionService.getCommentsByPostId(postId);
    }
}
