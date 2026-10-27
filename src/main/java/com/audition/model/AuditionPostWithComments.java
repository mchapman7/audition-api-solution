package com.audition.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditionPostWithComments extends AuditionPost {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<Comment> comments = new ArrayList<>();

    public static AuditionPostWithComments of(final AuditionPost post,
        final List<Comment> comments) {

        final AuditionPostWithComments result = new AuditionPostWithComments();
        result.setUserId(post.getUserId());
        result.setId(post.getId());
        result.setTitle(post.getTitle());
        result.setBody(post.getBody());
        result.setComments(new ArrayList<>(comments));
        return result;
    }
}
