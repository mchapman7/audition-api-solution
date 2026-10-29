package com.audition.web.doc;

import static com.audition.constant.ApiDocs.BAD_REQUEST;
import static com.audition.constant.ApiDocs.INVALID_INPUT;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(responseCode = BAD_REQUEST, description = INVALID_INPUT,
    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
        schema = @Schema(implementation = ProblemDetail.class)))
public @interface InvalidInputResponse {

}
