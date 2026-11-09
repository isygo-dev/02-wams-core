package eu.isygoit.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Configuration for the Plan Service.
 * Defines API documentation, security schemes, and server information.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "isygoPlan API",
                version = "1.0.0",
                description = """
                        Generic planning and scheduling API.

                        ## Features
                        - **Asynchronous planning jobs:** submit a planning problem, poll status, fetch the solution
                        - **Pluggable solvers:** CP-SAT and Timefold engines
                        - **Multi-tenant:** jobs and logs are scoped to the caller tenant
                        """,
                contact = @Contact(
                        name = "Isygoit Support",
                        email = "support@isygo-it.eu",
                        url = "https://isygo-it.eu"
                ),
                license = @License(
                        name = "Copyright © 2026 Isygoit. All Rights Reserved.",
                        url = "https://isygo-it.eu/license"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:40404",
                        description = "Local Development Server",
                        variables = {}
                )
        },
        security = {
                @SecurityRequirement(name = "Bearer Token"),
                @SecurityRequirement(name = "API Key")
        }
)
@SecurityScheme(
        name = "Bearer Token",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer token for authentication. Include in Authorization header: Authorization: Bearer <token>",
        in = SecuritySchemeIn.HEADER
)
@SecurityScheme(
        name = "API Key",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key",
        description = "API Key for service-to-service authentication"
)
public class OpenApiConfiguration {
    // Configuration class for OpenAPI documentation
}
