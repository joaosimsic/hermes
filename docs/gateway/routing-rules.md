# Gateway Routing & Security

The API Gateway (Spring Cloud Gateway) handles cross-cutting concerns before requests reach the microservices.

## Authentication Filter
* **Logic**: Intercepts requests, extracts `access_token` from cookies, and validates against JWKS.
* **Public Paths**: `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, and `/api/auth/**`.
* **Header Injection**: Validated requests are decorated with `X-User-Id` and `X-User-Email`.

## Resilience & Traffic Control
* **Rate Limiting**:
    * **Authenticated**: 100 req/s (Bucket: 150).
    * **Anonymous**: 5 req/s (Bucket: 10).
* **Circuit Breaker**: 
    * Targets: `user-service`, `auth-service`.
    * Threshold: 50% failure rate.
    * Fallback: Returns `503 Service Unavailable` with a custom JSON message.
