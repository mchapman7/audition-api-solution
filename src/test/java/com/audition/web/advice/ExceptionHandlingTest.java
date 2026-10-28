package com.audition.web.advice;

import static com.audition.constant.ErrorMessage.DEFAULT_MESSAGE;
import static com.audition.constant.ErrorMessage.DEFAULT_TITLE;
import static com.audition.constant.ErrorMessage.HTTP_CLIENT_ERROR;
import static com.audition.constant.ErrorMessage.POSITIVE_INTEGER_MESSAGE;
import static com.audition.constant.ErrorMessage.RESOURCE_NOT_FOUND;
import static com.audition.constant.ErrorMessage.UPSTREAM_UNAVAILABLE;
import static com.audition.constant.ErrorMessage.VALIDATION_TITLE;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import com.audition.configuration.WebServiceConfiguration;
import com.audition.service.AuditionService;
import com.audition.web.AuditionController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuditionController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = WebServiceConfiguration.class))
@Import(AuditionLogger.class)
public class ExceptionHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditionService auditionService;

    // ---- validation: bad input is rejected before the service is called ----

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "1.5", "1234567890", " "})
    void invalidPostIdReturns400AndNeverCallsTheService(final String id) throws Exception {

        mockMvc.perform(get("/posts/{id}", id))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value(VALIDATION_TITLE))
            .andExpect(jsonPath("$.detail").value(containsString(POSITIVE_INTEGER_MESSAGE)));
        verifyNoInteractions(auditionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1"})
    void invalidPostIdOnTheCommentEndpointsReturns400(final String id) throws Exception {
        mockMvc.perform(get("/posts/{id}/comments", id))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value(VALIDATION_TITLE));
        mockMvc.perform(get("/comments").param("postId", id))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value(VALIDATION_TITLE))
            .andExpect(jsonPath("$.detail").value(containsString("postId")));
        verifyNoInteractions(auditionService);
    }

    @Test
    void userIdOfZeroReturns400() throws Exception {
        mockMvc.perform(get("/posts").param("userId", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value(VALIDATION_TITLE))
            .andExpect(jsonPath("$.detail").value(containsString("userId")));
        verifyNoInteractions(auditionService);
    }

    @Test
    void aNegativeUserIdReturns400() throws Exception {
        mockMvc.perform(get("/posts").param("userId", "-5")).andExpect(status().isBadRequest());
    }

    @Test
    void aNonNumericUserIdReturns400() throws Exception {
        mockMvc.perform(get("/posts").param("userId", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void aTooLongTitleFilterReturns400() throws Exception {
        mockMvc.perform(get("/posts").param("title", "x".repeat(101)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("title")));
    }

    @Test
    void aTooLongBodyFilterReturns400() throws Exception {
        mockMvc.perform(get("/posts").param("body", "x".repeat(101)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("body")));
    }

    // ---- standard MVC errors, handled by the parent class ----

    @Test
    void aMissingPostIdParameterReturns400() throws Exception {
        mockMvc.perform(get("/comments"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void anUnsupportedMethodReturns405() throws Exception {
        mockMvc.perform(post("/posts"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405));
        mockMvc.perform(delete("/posts/1")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void anUnknownPathReturns404() throws Exception {
        mockMvc.perform(get("/does-not-exist")).andExpect(status().isNotFound());
    }

    // ---- SystemException mapping ----

    @Test
    void aNotFoundSystemExceptionIsReturnedWithItsStatusTitleAndMessage() throws Exception {
        when(auditionService.getPostById("9999")).thenThrow(new SystemException(
            "Resource not found while fetching post with id 9999", RESOURCE_NOT_FOUND, 404));

        mockMvc.perform(get("/posts/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value(RESOURCE_NOT_FOUND))
            .andExpect(jsonPath("$.detail").value(containsString("post with id 9999")));
    }

    @Test
    void aClientErrorSystemExceptionKeepsItsStatusAndMessage() throws Exception {
        when(auditionService.getPostById("1")).thenThrow(
            new SystemException("Error fetching post: 429 Too Many Requests", HTTP_CLIENT_ERROR, 429));

        mockMvc.perform(get("/posts/1"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.title").value(HTTP_CLIENT_ERROR))
            .andExpect(jsonPath("$.detail").value(containsString("429")));
    }

    @Test
    void aServerErrorSystemException_hidesInternalDetailFromTheCaller() throws Exception {
        when(auditionService.getPostById("1")).thenThrow(new SystemException(
            "I/O error on GET request for http://internal-host:5432", UPSTREAM_UNAVAILABLE, 503,
            new RuntimeException("socket closed")));

        mockMvc.perform(get("/posts/1"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.title").value(UPSTREAM_UNAVAILABLE))
            .andExpect(jsonPath("$.detail").value(DEFAULT_MESSAGE))
            .andExpect(content().string(not(containsString("internal-host"))));
    }

    @Test
    void aSystemExceptionWithoutATitle_getsTheDefaultTitle() throws Exception {
        when(auditionService.getPostById("1")).thenThrow(new SystemException("no title", (String) null, 400));

        mockMvc.perform(get("/posts/1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE));
    }

    @Test
    void aSystemExceptionWithAnInvalidStatusCodeFallsBackTo500() throws Exception {
        // Spring accepts three-digit codes only, so 42 cannot be mapped to a status.
        when(auditionService.getPostById("1")).thenThrow(new SystemException("bad code", "Some Title", 42));

        mockMvc.perform(get("/posts/1"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.detail").value(DEFAULT_MESSAGE));
    }

    // ---- unexpected exceptions ----

    @Test
    void anUnexpectedExceptionReturnsAGeneric500WithoutLeakingItsMessage() throws Exception {
        when(auditionService.getPostById("1")).thenThrow(new IllegalStateException("boom: internal detail"));

        mockMvc.perform(get("/posts/1"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.detail").value(DEFAULT_MESSAGE))
            .andExpect(content().string(not(containsString("boom"))));
    }

    @Test
    void aClientErrorWithANullMessageUsesTheDefaultMessage() throws Exception {
        when(auditionService.getPostById("1")).thenThrow(new SystemException(null, "Some Title", 400));

        mockMvc.perform(get("/posts/1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Some Title"))
            .andExpect(jsonPath("$.detail").value(DEFAULT_MESSAGE));
    }

    @Test
    void aClientErrorWithABlankMessageUsesTheDefaultMessage() throws Exception {
        when(auditionService.getPostById("1")).thenThrow(new SystemException("   ", "Some Title", 400));

        mockMvc.perform(get("/posts/1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(DEFAULT_MESSAGE));
    }
}
