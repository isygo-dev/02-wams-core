package eu.isygoit.ui.auth;

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

    public CompletableFuture<Result<UserContext>> resolveAuthContext(AuthenticationContextRequest request) {
        return submit(() -> authService.resolveAuthContext(request), response -> {
            if (isSuccessful(response) && response.getBody() != null) {
                return Result.success(response.getBody());
            }
            return Result.failure(mapFailure(response, Failure.SERVICE_ERROR));
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
            return Result.failure(mapFailure(response, fallback));
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
            return Result.failure(mapFailure(response, fallback));
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
            return Result.failure(mapFailure(response, Failure.SERVICE_ERROR));
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
            return Result.failure(mapFailure(response, Failure.INVALID_TOKEN));
        });
    }

    private <T> CompletableFuture<Result<T>> submit(
            Supplier<ResponseEntity<T>> request,
            Function<ResponseEntity<T>, Result<T>> responseMapper) {
        try {
            return CompletableFuture.supplyAsync(request, authExecutor).thenApply(responseMapper);
        } catch (RejectedExecutionException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private static boolean isSuccessful(ResponseEntity<?> response) {
        return response.getStatusCode().is2xxSuccessful();
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

    public record Result<T>(T value, Failure failure) {

        public static <T> Result<T> success(T value) {
            return new Result<>(value, null);
        }

        public static <T> Result<T> failure(Failure failure) {
            return new Result<>(null, failure);
        }

        public boolean succeeded() {
            return failure == null;
        }
    }
}
