package eu.isygoit.ui.auth;

import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.request.AuthenticationContextRequest;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.request.QrLoginChallengeRequest;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.dto.response.QrLoginStatusDto;
import eu.isygoit.dto.response.UserContext;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.remote.ims.PublicAuthService;
import org.springframework.beans.factory.annotation.Qualifier;
import feign.FeignException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class AuthServiceFacade {

    private final PublicAuthService authService;
    private final Executor authExecutor;

    public AuthServiceFacade(PublicAuthService authService,
                             @Qualifier("authExecutor") Executor authExecutor) {
        this.authService = authService;
        this.authExecutor = authExecutor;
    }

    private static boolean isSuccessful(ResponseEntity<?> response) {
        return response.getStatusCode().is2xxSuccessful();
    }

    private static final long MAX_RETRY_AFTER_SECONDS = 3600;

    /**
     * Reads Retry-After (delta-seconds). Returns 0 when absent or unparsable, so the UI falls back to a generic
     * message instead of inventing a wait time.
     */
    static long retryAfterSeconds(ResponseEntity<?> response) {
        String header = response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (header == null) {
            return 0;
        }
        try {
            long seconds = Long.parseLong(header.trim());
            return seconds <= 0 ? 0 : Math.min(seconds, MAX_RETRY_AFTER_SECONDS);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static <T> Result<T> failureResult(ResponseEntity<?> response, Failure fallback) {
        Failure failure = mapFailure(response, fallback);
        return failure == Failure.RATE_LIMITED
                ? Result.rateLimited(retryAfterSeconds(response))
                : Result.failure(failure);
    }

    /**
     * Feign throws on any non-2xx status, which would hide 4xx statuses (429, 423, 410, 401...) behind a generic
     * "service unavailable". 4xx responses are converted back to a ResponseEntity so {@link #mapFailure} and the
     * Retry-After header can be used. 5xx and transport errors keep propagating as exceptions.
     */
    private static <T> Supplier<ResponseEntity<T>> clientErrorsAsResponses(Supplier<ResponseEntity<T>> request) {
        return () -> {
            try {
                return request.get();
            } catch (FeignException exception) {
                if (exception.status() < 400 || exception.status() > 499) {
                    throw exception;
                }
                HttpHeaders headers = new HttpHeaders();
                exception.responseHeaders().forEach((name, values) ->
                        values.forEach(value -> headers.add(name, value)));
                return ResponseEntity.<T>status(exception.status()).headers(headers).build();
            }
        };
    }

    private static Failure mapFailure(ResponseEntity<?> response, Failure fallback) {
        int status = response.getStatusCode().value();
        if (status == 429) {
            return Failure.RATE_LIMITED;
        }
        if (status == 423) {
            return Failure.LOCKED;
        }
        if (status == 410) {
            return Failure.EXPIRED;
        }
        if (status == 503) {
            return Failure.OFFLINE;
        }
        if (status == 401 || status == 403) {
            return fallback == Failure.INVALID_TOKEN ? Failure.INVALID_TOKEN : Failure.INVALID_CREDENTIALS;
        }
        return fallback;
    }

    public CompletableFuture<Result<UserContext>> resolveAuthContext(AuthenticationContextRequest request) {
        return submit(() -> authService.resolveAuthContext(request), response -> {
            if (isSuccessful(response) && response.getBody() != null) {
                return Result.success(response.getBody());
            }
            return failureResult(response, Failure.SERVICE_ERROR);
        });
    }

    public CompletableFuture<Result<AuthResponseDto>> authenticate(AuthenticationRequestDto request) {
        Failure fallback = request.getAuthType() == IEnumAuth.Types.QRC
                ? Failure.INVALID_TOKEN
                : Failure.INVALID_CREDENTIALS;
        return submit(() -> authService.authenticate(request), response -> {
            AuthResponseDto body = response.getBody();
            if (isSuccessful(response) && body != null
                    && body.getAccessToken() != null && !body.getAccessToken().isBlank()) {
                return Result.success(body);
            }
            return failureResult(response, fallback);
        });
    }

    public CompletableFuture<Result<Boolean>> registerUser(RegisteredUserDto request) {
        return submit(() -> authService.registerUser(request), response -> {
            if (isSuccessful(response) && Boolean.TRUE.equals(response.getBody())) {
                return Result.success(Boolean.TRUE);
            }
            Failure fallback = Boolean.FALSE.equals(response.getBody())
                    || response.getStatusCode().is4xxClientError()
                    ? Failure.REGISTRATION_REJECTED
                    : Failure.SERVICE_ERROR;
            return failureResult(response, fallback);
        });
    }

    public CompletableFuture<Result<QrLoginStatusDto>> getQrLoginStatus(String challengeId) {
        QrLoginChallengeRequest request = QrLoginChallengeRequest.builder()
                .challengeId(challengeId)
                .build();
        return submit(() -> authService.getQrLoginStatus(request), response -> {
            if (isSuccessful(response) && response.getBody() != null
                    && response.getBody().getStatus() != null) {
                return Result.success(response.getBody());
            }
            return failureResult(response, Failure.SERVICE_ERROR);
        });
    }

    public CompletableFuture<Result<AuthResponseDto>> completeQrLogin(String challengeId) {
        QrLoginChallengeRequest request = QrLoginChallengeRequest.builder()
                .challengeId(challengeId)
                .build();
        return submit(() -> authService.completeQrLogin(request), response -> {
            AuthResponseDto body = response.getBody();
            if (isSuccessful(response) && body != null
                    && body.getAccessToken() != null && !body.getAccessToken().isBlank()) {
                return Result.success(body);
            }
            return failureResult(response, Failure.INVALID_TOKEN);
        });
    }

    private <T> CompletableFuture<Result<T>> submit(
            Supplier<ResponseEntity<T>> request,
            Function<ResponseEntity<T>, Result<T>> responseMapper) {
        try {
            return ContextAwareAsync.supplyAsync(clientErrorsAsResponses(request), authExecutor)
                    .thenApply(responseMapper);
        } catch (RejectedExecutionException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    public enum Failure {
        INVALID_CREDENTIALS,
        INVALID_TOKEN,
        SERVICE_ERROR,
        OFFLINE,
        RATE_LIMITED,
        LOCKED,
        EXPIRED,
        REGISTRATION_REJECTED
    }

    /**
     * @param retryAfterSeconds wait time announced by the server for RATE_LIMITED, 0 when unknown
     */
    public record Result<T>(T value, Failure failure, long retryAfterSeconds) {

        public Result(T value, Failure failure) {
            this(value, failure, 0);
        }

        public static <T> Result<T> success(T value) {
            return new Result<>(value, null, 0);
        }

        public static <T> Result<T> failure(Failure failure) {
            return new Result<>(null, failure, 0);
        }

        public static <T> Result<T> rateLimited(long retryAfterSeconds) {
            return new Result<>(null, Failure.RATE_LIMITED, retryAfterSeconds);
        }

        public boolean succeeded() {
            return failure == null;
        }
    }
}
