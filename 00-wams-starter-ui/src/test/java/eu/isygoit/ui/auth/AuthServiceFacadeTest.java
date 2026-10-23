package eu.isygoit.ui.auth;

import eu.isygoit.ui.auth.AuthServiceFacade.Failure;
import eu.isygoit.ui.auth.AuthServiceFacade.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceFacadeTest {

    private static ResponseEntity<Void> tooManyRequests(String retryAfter) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(429);
        if (retryAfter != null) {
            builder.header(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return builder.build();
    }

    @Test
    void retryAfterIsReadInSeconds() {
        assertEquals(45, AuthServiceFacade.retryAfterSeconds(tooManyRequests("45")));
        assertEquals(45, AuthServiceFacade.retryAfterSeconds(tooManyRequests(" 45 ")));
    }

    @Test
    void missingOrInvalidRetryAfterMeansUnknown() {
        assertEquals(0, AuthServiceFacade.retryAfterSeconds(tooManyRequests(null)));
        assertEquals(0, AuthServiceFacade.retryAfterSeconds(tooManyRequests("soon")));
        assertEquals(0, AuthServiceFacade.retryAfterSeconds(tooManyRequests("Wed, 21 Oct 2026 07:28:00 GMT")));
        assertEquals(0, AuthServiceFacade.retryAfterSeconds(tooManyRequests("0")));
        assertEquals(0, AuthServiceFacade.retryAfterSeconds(tooManyRequests("-5")));
    }

    @Test
    void retryAfterIsCappedToOneHour() {
        assertEquals(3600, AuthServiceFacade.retryAfterSeconds(tooManyRequests("999999")));
    }

    @Test
    void rateLimitedResultCarriesTheWaitTime() {
        Result<String> result = Result.rateLimited(30);

        assertFalse(result.succeeded());
        assertEquals(Failure.RATE_LIMITED, result.failure());
        assertEquals(30, result.retryAfterSeconds());
    }

    @Test
    void otherResultsHaveNoWaitTime() {
        assertEquals(0, Result.failure(Failure.INVALID_CREDENTIALS).retryAfterSeconds());
        assertEquals(0, Result.success("ok").retryAfterSeconds());
        assertTrue(Result.success("ok").succeeded());
    }
}
