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
 * OpenAPI/Swagger Configuration for KMS Service
 * Defines API documentation, security schemes, and server information
 *
 * @author Isygoit Team
 * @version 1.0
 * @since 2026-05-07
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "WAMS Key Management Service (KMS) API",
                version = "1.0.0",
                description = """
                        Comprehensive AWS KMS-compatible Key Management Service for enterprise-grade cryptographic operations.
                        
                        ## Features
                        - **Symmetric & Asymmetric Encryption:** AES-256, RSA-2048/4096, Elliptic Curve cryptography
                        - **Digital Signatures:** RSA-PSS, ECDSA with multiple hash algorithms
                        - **Envelope Encryption:** Data key generation for encrypting large datasets
                        - **Key Rotation:** Automatic and manual key rotation with version tracking
                        - **Multi-Region Replication:** Cross-region key synchronization for disaster recovery
                        - **Fine-Grained Access Control:** Policies and grants for principal-based permissions
                        - **Message Authentication Codes:** HMAC generation and verification
                        - **Key Material Import (BYOK):** Bring Your Own Key support for imported material
                        - **Custom Key Stores:** Integration with CloudHSM and external key management systems
                        - **Comprehensive Audit Trail:** All operations logged for compliance and security auditing
                        - **Multi-Tenant:** Complete tenant isolation at database, application, and audit levels
                        
                        ## AWS KMS Alignment
                        This service implements AWS KMS API specifications for seamless migration from cloud-native KMS.
                        Endpoints, request/response formats, error codes, and operational semantics align with AWS KMS.
                        
                        ## Use Cases
                        1. **Data at Rest Protection:** Encrypt databases, files, and backups
                        2. **API Security:** HMAC authentication for distributed systems
                        3. **Document Signing:** Sign certificates, contracts, and code
                        4. **Envelope Encryption:** Secure large files with data keys
                        5. **Compliance:** Meet encryption requirements for HIPAA, PCI-DSS, SOC 2, GDPR
                        6. **Key Lifecycle Management:** Centralized key management with rotation and versioning
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
                        url = "http://localhost:8080/api/v1/private",
                        description = "Local Development Server",
                        variables = {}
                ),
                @Server(
                        url = "https://kms.example.com/api/v1/private",
                        description = "Production Environment"
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

