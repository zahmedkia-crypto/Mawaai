# HTTP Error Translation Table

Maps every common HTTP response code from AI providers to the typed gateway error hierarchy. Use as the canonical reference when writing a new adapter's `translateError()` function.

## The Hierarchy

```
Throwable
├── ProviderRecoverableError  ──── FallbackChain tries the next provider
│   ├── NotFound              ──── 404 (model deprecated, endpoint moved)
│   ├── RateLimited           ──── 429
│   ├── ServiceUnavailable    ──── 500-599, network errors
│   ├── Timeout               ──── socket timeout, request timeout
│   └── QuotaExhausted        ──── 402 (Stripe), provider-specific
└── ProviderFatalError        ──── FallbackChain stops and propagates
    ├── InvalidKey            ──── 401, 403
    ├── MalformedRequest      ──── 400 (other than safety), 422
    └── SafetyBlock           ──── content-policy block (provider-specific)
```

## Mapping Table

| HTTP Code | Symptom (typical) | Gateway Error | Rationale |
|---:|---|---|---|
| 200 | success | (none) | Result.success |
| 400 | bad request | `MalformedRequest` (fatal) | The request is malformed; retrying with another provider won't help. EXCEPTION: some providers return 400 for safety blocks → check body, return `SafetyBlock` if matched |
| 401 | unauthorized | `InvalidKey` (fatal) | User must fix their key. Don't waste another provider on this. |
| 402 | payment required | `QuotaExhausted` (recoverable) | Try another provider (likely on a different billing account). |
| 403 | forbidden | `InvalidKey` (fatal) | Usually an auth issue. Don't burn quota. |
| 404 | not found | `NotFound` (recoverable) | Model deprecated or endpoint moved. Try next provider. |
| 408 | request timeout | `Timeout` (recoverable) | Retry with another provider. |
| 413 | payload too large | `MalformedRequest` (fatal) | Image is too big; user must downscale. |
| 422 | unprocessable | `MalformedRequest` (fatal) | Schema mismatch or invalid input. |
| 429 | too many requests | `RateLimited` (recoverable) | Try next provider. |
| 499 | client closed | `Timeout` (recoverable) | Treat like timeout. |
| 500 | internal server error | `ServiceUnavailable` (recoverable) | Try next. |
| 502 | bad gateway | `ServiceUnavailable` (recoverable) | Provider's infra issue. |
| 503 | service unavailable | `ServiceUnavailable` (recoverable) | Try next. |
| 504 | gateway timeout | `Timeout` (recoverable) | Try next. |

## Non-HTTP Errors

| Exception | Gateway Error |
|---|---|
| `java.net.SocketTimeoutException` | `Timeout` |
| `java.net.UnknownHostException` | `ServiceUnavailable` (DNS) |
| `java.net.ConnectException` | `ServiceUnavailable` |
| `java.io.IOException` (generic) | `ServiceUnavailable` |
| `javax.net.ssl.SSLException` | `ServiceUnavailable` (TLS) |
| Gson `JsonSyntaxException` | `MalformedRequest` (response body malformed) |

## Provider-Specific Quirks

### Gemini
- 404 body contains `"models/<name> is not found"` → confirm with ListModels and update the constant
- 429 body contains `quota` → `RateLimited`; body contains `billing` → `QuotaExhausted`
- 400 body containing `"SAFETY"` or `"RECITATION"` → `SafetyBlock`

### Groq
- Returns OpenAI-compatible errors
- Body shape: `{"error": {"message": "...", "type": "..."}}`
- 429 type=`rate_limit_exceeded` is recoverable
- 400 type=`invalid_request_error` is fatal

### Cloudflare Workers AI
- Body shape: `{"success": false, "errors": [{"code": N, "message": "..."}]}`
- Internal error codes 1000+ map mostly to fatal/MalformedRequest
- Status 429 returned as `code: 7003`

### OpenRouter
- Body shape: `{"error": {"message": "...", "code": "..."}}`
- 402 → user account out of credits → `QuotaExhausted`
- Special: when chain falls back internally inside OpenRouter, returns 200 — treat as success

### HuggingFace Inference
- 503 with body `"loading"` means the model is cold-starting → retry after 20s OR fall back
- Long-polling pattern is unique to HF — most other providers don't have this

## Adapter Template (translateError function)

```kotlin
private fun translateError(e: Throwable, providerName: String): Throwable {
    val httpCode = (e as? retrofit2.HttpException)?.code()
    val body = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()?.lowercase().orEmpty()

    // Provider-specific safety block detection BEFORE generic 400
    if (httpCode == 400 && ("safety" in body || "policy" in body)) {
        return ProviderFatalError.SafetyBlock("$providerName: content blocked")
    }

    return when (httpCode) {
        404 -> ProviderRecoverableError.NotFound("$providerName: model deprecated")
        429 -> ProviderRecoverableError.RateLimited("$providerName: rate limited")
        402 -> ProviderRecoverableError.QuotaExhausted("$providerName: credits exhausted")
        408, 504 -> ProviderRecoverableError.Timeout("$providerName: timeout")
        in 500..599 -> ProviderRecoverableError.ServiceUnavailable("$providerName: HTTP $httpCode")
        401, 403 -> ProviderFatalError.InvalidKey("$providerName: auth failed")
        400, 422 -> ProviderFatalError.MalformedRequest("$providerName: HTTP $httpCode")
        413 -> ProviderFatalError.MalformedRequest("$providerName: payload too large")
        null -> when (e) {
            is java.net.SocketTimeoutException -> ProviderRecoverableError.Timeout("$providerName: socket timeout")
            is java.net.UnknownHostException -> ProviderRecoverableError.ServiceUnavailable("$providerName: DNS failure")
            is java.io.IOException -> ProviderRecoverableError.ServiceUnavailable("$providerName: network: ${e.message}")
            is com.google.gson.JsonSyntaxException -> ProviderFatalError.MalformedRequest("$providerName: bad response shape")
            else -> ProviderRecoverableError.ServiceUnavailable("$providerName: ${e.javaClass.simpleName}")
        }
        else -> ProviderRecoverableError.ServiceUnavailable("$providerName: HTTP $httpCode")
    }
}
```

Customize the safety-block detection per provider (the `"safety" in body || "policy" in body` heuristic catches Gemini + most others, but Groq uses different keys — verify in the provider's docs).
