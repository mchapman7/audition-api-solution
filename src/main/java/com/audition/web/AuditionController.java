package com.audition.web;

import static com.audition.constant.ErrorMessage.POSITIVE_INTEGER_MESSAGE;
import static com.audition.constant.ValidationRules.MAX_FILTER_LENGTH;
import static com.audition.constant.ValidationRules.POSITIVE_INTEGER;

import com.audition.model.AuditionPost;
import com.audition.model.AuditionPostWithComments;
import com.audition.model.Comment;
import com.audition.service.AuditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Audition", description = "Posts and their comments")
public class AuditionController {

    private static final String OK = "200";
    private static final String BAD_REQUEST = "400";
    private static final String NOT_FOUND = "404";
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String INVALID_INPUT = "Invalid input";
    private static final String POST_NOT_FOUND = "Post not found";
    private static final String POST_ID = "Id of the post";

    private final AuditionService auditionService;

    @Operation(summary = "List posts, optionally filtered")
    @ApiResponses({
        @ApiResponse(responseCode = OK, description = "The matching posts"),
        @ApiResponse(responseCode = BAD_REQUEST, description = INVALID_INPUT,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
    })
    @GetMapping(value = "/posts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditionPost> getPosts(@RequestParam(value = "userId", required = false) @Positive final Integer userId,
        @RequestParam(value = "title", required = false) @Size(max = MAX_FILTER_LENGTH) final String title,
        @RequestParam(value = "body", required = false) @Size(max = MAX_FILTER_LENGTH) final String body) {

        return auditionService.getPosts(userId, title, body);
    }

    @Operation(summary = "Get a post by id")
    @ApiResponses({
        @ApiResponse(responseCode = OK, description = "The post"),
        @ApiResponse(responseCode = BAD_REQUEST, description = INVALID_INPUT,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = NOT_FOUND, description = POST_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/posts/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody AuditionPost getPostById(
        @PathVariable("id") @Pattern(regexp = POSITIVE_INTEGER, message = POSITIVE_INTEGER_MESSAGE) final String postId) {

        return auditionService.getPostById(postId);
    }

    /**
     * The post with its comments embedded.
     */
    @Operation(summary = "Get a post with its comments embedded")
    @ApiResponses({
        @ApiResponse(responseCode = OK, description = "The post with its comments embedded"),
        @ApiResponse(responseCode = BAD_REQUEST, description = INVALID_INPUT,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = NOT_FOUND, description = POST_NOT_FOUND,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/posts/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditionPostWithComments getPostWithComments(
        @PathVariable("id") @Pattern(regexp = POSITIVE_INTEGER, message = POSITIVE_INTEGER_MESSAGE) final String postId) {

        return auditionService.getPostWithComments(postId);
    }

    /**
     * Comments for a post as a separate list.
     */
    @Operation(summary = "Get the comments of a post as a plain list")
    @ApiResponses({
        @ApiResponse(responseCode = OK, description = "The comments of the post"),
        @ApiResponse(responseCode = BAD_REQUEST, description = INVALID_INPUT,
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
    })
    @GetMapping(value = "/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Comment> getCommentsByPostId(
        @RequestParam("postId") @Pattern(regexp = POSITIVE_INTEGER, message = POSITIVE_INTEGER_MESSAGE) final String postId) {

        return auditionService.getCommentsByPostId(postId);
    }
}
