package com.audition.web;

import static com.audition.constant.ApiDocs.OK;
import static com.audition.constant.ErrorMessage.POSITIVE_INTEGER_MESSAGE;
import static com.audition.constant.ValidationRules.MAX_FILTER_LENGTH;
import static com.audition.constant.ValidationRules.POSITIVE_INTEGER;

import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostWithComments;
import com.audition.model.Comment;
import com.audition.service.AuditionService;
import com.audition.web.doc.InvalidInputResponse;
import com.audition.web.doc.PostNotFoundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Audition", description = "Posts and their comments")
public class AuditionController {

    private final AuditionService auditionService;

    @Operation(summary = "List posts, optionally filtered")
    @ApiResponse(responseCode = OK, description = "The matching posts")
    @InvalidInputResponse
    @GetMapping(value = "/posts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditionPost> getPosts(
        @Parameter(description = "Only posts by this author") @RequestParam(value = "userId", required = false)
        @Positive final Integer userId,
        @Parameter(description = "Text the title contains, ignoring case") @RequestParam(value = "title", required = false)
        @Size(max = MAX_FILTER_LENGTH) final String title,
        @Parameter(description = "Text the body contains, ignoring case") @RequestParam(value = "body", required = false)
        @Size(max = MAX_FILTER_LENGTH) final String body) {

        return auditionService.getPosts(userId, title, body);
    }

    @Operation(summary = "Get a post by id")
    @ApiResponse(responseCode = OK, description = "The post")
    @InvalidInputResponse
    @PostNotFoundResponse
    @GetMapping(value = "/posts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditionPost getPostById(
        @PathVariable("id") @Pattern(regexp = POSITIVE_INTEGER, message = POSITIVE_INTEGER_MESSAGE) final String postId) {

        return auditionService.getPostById(postId);
    }

    /**
     * The post with its comments embedded.
     */
    @Operation(summary = "Get a post with its comments embedded")
    @ApiResponse(responseCode = OK, description = "The post with its comments embedded")
    @InvalidInputResponse
    @PostNotFoundResponse
    @GetMapping(value = "/posts/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditionPostWithComments getPostWithComments(
        @PathVariable("id") @Pattern(regexp = POSITIVE_INTEGER, message = POSITIVE_INTEGER_MESSAGE) final String postId) {

        return auditionService.getPostWithComments(postId);
    }

    /**
     * Comments for a post as a separate list.
     */
    @Operation(summary = "Get the comments of a post as a plain list")
    @ApiResponse(responseCode = OK, description = "The comments of the post")
    @InvalidInputResponse
    @GetMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Comment> getCommentsByPostId(
        @RequestParam("postId") @Pattern(regexp = POSITIVE_INTEGER, message = POSITIVE_INTEGER_MESSAGE) final String postId) {

        return auditionService.getCommentsByPostId(postId);
    }
}
