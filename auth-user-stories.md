# Authentication hardening: user stories (DRAFT, not yet published)

Source: authentication audit (first report F-xx, second report X-xx, end-to-end review S-xx).
Effort: S <= 1 day, M <= 1 week, L > 1 week. Priority: P0 contain now, P1 core, P2 platform, P3 backlog.
Each story becomes one GitHub issue. Labels: `security`, `auth`, plus the epic and priority label shown.

---

## Epic A: Contain now (label `epic:contain`, `P0`)

**A1. Issue tokens only for valid credentials** (KMS) [S-01] Effort S
As a security owner, I want KMS to return tokens only when the credential check is `VALID`, so that nobody can obtain a token for another user by calling KMS directly.
- AC: `POST /api/v1/public/password/access` with a wrong password returns no `accessToken`, `refreshToken` or `authorityToken`.
- AC: BAD, EXPIRED, BROKEN, LOCKED, DEPRECATED statuses return the status only.
- AC: integration test for every status.

/*DONE*/

**A2. Take KMS public endpoints off the public network** (gateway, deployment) [S-03, S-15] Effort S
As a platform engineer, I want `/api/v1/public/password/**` reachable only from internal services, so that OTP, QR and token endpoints are not anonymous internet endpoints.
- AC: external request to the KMS public paths is refused.
- AC: gateway routes confirmed to load (YAML nesting fixed if not).

/*TO BE PLANNED*/

**A3. Rotate and externalise committed secrets** (config-service) [S-06] Effort M
As a security owner, I want all secrets out of git and rotated, so that a repository leak does not compromise tokens or databases.
- AC: JWT, jasypt, `shouldNotFilterKey`, DB and admin credentials rotated and read from env or a secret manager.
- AC: services refuse to start if a known default is in use.
- AC: old values purged from git history.

/*TO BE PLANNED*/

**A4. Remove the filter-bypass default key** (stack `AbstractJwtAuthFilter`) [S-02] Effort M
As a security owner, I want the HMAC filter bypass removed or hardened, so that a known key cannot open every private endpoint.
- AC: no default key; constant-time compare; timestamp, nonce, method and path bound; or the bypass is deleted in favour of service-to-service auth.
- AC: forged signature returns 401 on `/private/**`.

/*TO BE PLANNED*/

**A5. Replace `requestMatchers("**").permitAll()` with an allowlist** (IMS, KMS `SecurityConfig`) [F-13, S-02] Effort S
As a developer, I want explicit public paths only, so that `anyRequest().authenticated()` actually applies.
- AC: only `/api/v1/public/**`, swagger and health are public.
- AC: protected endpoint without JWT returns 401 in an integration test.

/*TO BE PLANNED*/

**A6. Stop logging credentials and secrets** (KMS) [S-07] Effort S
As a security owner, I want passwords, OTPs and tokens excluded from logs, so that log access never reveals credentials.
- AC: `@ToString.Exclude` on password fields; generated OTP/QRC/PWD masked; broken format strings fixed.
- AC: test captures log output for a login and finds no credential.

/*TO BE PLANNED*/

**A7. Require authentication on `switchAuthType`** (IMS) [X-02] Effort S
As a user, I want only I (when signed in) to change my authentication type, so that nobody can downgrade my second factor.
- AC: anonymous call returns 401; caller can change only their own account; re-authentication required.

/*DONE*/

**A8. Make the password matcher fail closed** (IMS `PasswordEncoderService`) [X-01] Effort S
As a security owner, I want only `VALID` to count as a match, so that a null or unexpected KMS response never logs anyone in.
- AC: null body, EXPIRED, LOCKED, DEPRECATED return false.

/*DONE*/

**A9. Interim edge rate limiting** (nginx / gateway) [F-01] Effort S
As a platform engineer, I want throttling on `/authenticate`, `/authType`, `/qr/*`, `/register` and `/password/*`, so that guessing and flooding are slowed while app-level lockout is built.
- AC: burst from one IP returns 429 with `Retry-After`; real client IP used.

/*DONE*/

---

## Epic B: Authentication core (label `epic:auth-core`, `P1`)

**B1. Account lockout and attempt counting** (KMS/IMS) [S-04, F-01] Effort M
As a security owner, I want failed attempts counted per account and IP with progressive delay and temporary lock, so that online brute force is impractical.
- AC: threshold configurable per tenant; `TEM_LOCKED` set and unlock job runs; BAD status is not sticky bypass.

**B2. Login audit trail** (KMS/IMS) [S-20] Effort M
As an auditor, I want a structured event for every login attempt (hashed user, tenant, outcome, reason, IP), so that attacks and incidents can be investigated.
- AC: success and failure both recorded; retention job; no raw credentials.

**B3. Redesign OTP** (KMS) [S-05] Effort M
As a user, I want one-time codes that cannot be guessed or reused, so that my account is safe when OTP is my factor.
- AC: `SecureRandom`; at least 6 digits; hashed; single-use; 5-attempt cap; resend cooldown; shared key-generator buffer removed.

/*DONE*/

**B4. Rebuild the token model** (KMS/IMS) [S-08] Effort L
As a security owner, I want revocable, short-lived access tokens and rotating refresh tokens, so that stolen tokens have limited value.
- AC: tokens identified by SHA-256 `jti` (no CRC); access 10-15 min; refresh rotating with reuse detection; refresh endpoint exists; logout revokes the family.

**B5. Remove `authType=TOKEN` as a login path** (KMS/IMS/FE) [S-09] Effort M
As a security owner, I want an access token to stop being exchangeable for new tokens forever, so that a stolen token cannot be extended without the password.
- AC: replaced by the refresh flow; React `?accessToken=` flow removed.

**B6. Strict JWT validation** (stack `JwtService`, filter) [S-10] Effort M
As a developer, I want the algorithm pinned to configuration and `exp`, `iss`, `aud` and token type required, so that token confusion is impossible.
- AC: swapped `alg`, wrong audience, wrong type, missing `exp` all rejected; no claim read from unsigned payloads for decisions.

**B7. Secure forgot-password** (KMS) [S-12] Effort M
As a user, I want password reset links that work once and do not reveal whether an account exists.
- AC: identical response for unknown and known users; throttled; token single-use; signature verified before reading the subject; password policy enforced on change; operator-precedence bug fixed.

**B8. Fix IMS `AuthService` flow** (IMS) [F-02, F-05, F-06, F-09, X-04, X-05, X-09] Effort M
As a developer, I want the login service to propagate real account states, hold no DB connection during remote calls, and fail uniformly, so that errors are correct and the service cannot be exhausted.
- AC: inner catch narrowed to transport errors; no class-level `@Transactional`; tenant and app checks after verification; uniform 401; account and tenant resolved before tokens are returned.

**B9. Validate and normalise login input once** (IMS DTO, shared util) [F-09, F-10] Effort S
As a developer, I want one `Locale.ROOT`/NFKC normaliser and strict DTO constraints, so that login and registration agree and passwords are never altered.
- AC: `@NotBlank`, `@Size`, no `@` in tenant/user; password never trimmed; register uses the same normaliser.

**B10. Constant-time login responses** (IMS) [X-03] Effort M
As a security owner, I want unknown, locked and wrong-password cases indistinguishable in status, body and timing, so that usernames cannot be enumerated.
- AC: statistical timing test passes; `resolveAuthContext` and `getTenantByName` stop acting as oracles.

**B11. Secure registration** (IMS/KMS) [S-19] Effort M
As a tenant admin, I want sign-ups to be verified and unable to set privileged fields.
- AC: client cannot set `status`, `tenant` or `origin`; unique constraints fixed; email verification; set-password link instead of emailing a password.

**B12. Authenticated service-to-service calls** (stack, KMS, IMS) [S-13] Effort M
As a platform engineer, I want mTLS or signed service tokens between IMS and KMS, validated on receipt.
- AC: call without credentials refused; `X-API-KEY` header either validated or removed.

**B13. Trusted identity headers only** (gateway, `RequestContextService`) [S-14, F-07] Effort M
As a security owner, I want the gateway to strip inbound identity headers and re-inject them from a verified JWT, so that tenant, user and admin context cannot be spoofed.
- AC: spoofed `X-SENTER-*`, `X-IS-ADMIN`, `X-Tenant-ID` ignored; real authorities populated in the security context; trusted-proxy handling for client IP.

**B14. Throttle OTP and QR side effects** (IMS) [F-04] Effort M
As a user, I want others unable to flood my inbox or create QR challenges for my account.
- AC: per-account OTP cooldown and hourly cap; challenge creation throttled; cleanup moved to a scheduled job.

---

## Epic C: Data and tenancy (label `epic:data`, `P2`)

**C1. Enforce tenant isolation in the data layer** (JPA modules) [S-18] Effort L
As a tenant admin, I want queries automatically scoped to my tenant, with a database backstop.
- AC: Hibernate `@TenantId` or filters active for IMS and KMS; Postgres RLS on auth tables; decision recorded on global versus per-tenant `code` uniqueness.

**C2. Real migrations** (IMS, KMS) [S-21] Effort M
As a developer, I want Flyway enabled with baseline scripts and `ddl-auto: validate`.
- AC: clean database builds from scripts; indexes on lookup columns.

**C3. Connection tracking without unbounded growth** (IMS) [X-07, S-20] Effort M
As an operator, I want logins to cost the same on day 1 and day 1000.
- AC: insert-only repository; ip/device/browser length caps; retention job.

**C4. Modern password hashing** (KMS) [S-23] Effort M
As a security owner, I want Argon2id with rehash on login, and secrets in `PEBConfig` encrypted.
- AC: `DelegatingPasswordEncoder`; old hashes upgraded on next login; `Account.getPassword()` removed.

**C5. De-duplicate the KMS code across Edyra modules** [S-22] Effort L
As a developer, I want one shared auth library, so that a fix lands once.
- AC: six copies replaced by one dependency.

**C6. `findByTenantAndUserName` returns `Optional`** (IMS) [X-08] Effort M
As a developer, I want a non-nullable lookup contract and a case-insensitive unique index.
- AC: all callers migrated; no NPE paths; constraint added.

---

## Epic D: Clients and edge (label `epic:clients`, `P2`)

**D1. React: tokens out of JavaScript** (`05-wams-fe`) [S-16] Effort M
As a user, I want my session protected from script theft.
- AC: HttpOnly SameSite cookies or BFF; no `localStorage` tokens; no token in URL; correct endpoint and field names; server-side logout.

**D2. Vaadin: real session handling** (`00-wams-starter-ui`) [S-17] Effort M
As a user, I want sessions that expire, refresh silently and end on logout.
- AC: session timeout set; token expiry checked; refresh used; logout revokes; secure cookie flags.

**D3. Edge hardening** (gateway, nginx) [S-15] Effort M
As a platform engineer, I want TLS, security headers, an origin allowlist for CORS and JWT validation at the gateway.
- AC: HSTS, CSP, X-Frame-Options; CORS allowlist; gateway rejects unauthenticated private calls.

**D4. Spring Security 6 and error model** (all services) [F-06] Effort M
As a developer, I want lambda DSL config, method security and `ProblemDetail` errors with one `@RestControllerAdvice`.
- AC: no `catch (Throwable)` in controllers; standardised error bodies.

---

## Epic E: Modernisation (label `epic:modernisation`, `P3`)

**E1. OIDC identity provider** Effort L: Authorization Code + PKCE via Keycloak or Spring Authorization Server (decision pending), asymmetric signing, JWKS, local verification in each service.
**E2. Stronger factors** Effort L: TOTP and WebAuthn/passkeys; step-up for sensitive actions.
**E3. QR login redesign** Effort L: initiator-bound challenge, device and IP shown on approval, approving client built.
**E4. Auth observability** Effort M: metrics, alerts and a SIEM-ready audit stream.
**E5. Security CI** Effort M: secret scanning, dependency checks, automated auth test matrices.
