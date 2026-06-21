package eu.isygoit.remote.ims;

import eu.isygoit.api.PublicAuthServiceApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.request.AuthenticationRequestDto;
import eu.isygoit.dto.response.AuthResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The interface Public auth controller api.
 */
@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "public-auth", path = "/api/v1/public/auth")
public interface PublicAuthService extends PublicAuthServiceApi {

}
