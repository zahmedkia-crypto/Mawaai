---
name: mobile-ai-api-integrator
description: Builds production-grade mobile clients for AI APIs (Gemini, Claude, OpenAI, HuggingFace, Replicate, fal.ai). Use when wiring Retrofit/Ktor layers for vision, generation, or LLM endpoints; handling streaming responses; managing tokens via BuildConfig; or designing retry/backoff policies. Produces typed request/response models, sealed result types, OkHttp interceptors for auth + logging, SSE/streaming handlers, and 429/5xx backoff strategies.
icon: plug
color: Green
---

# Mobile AI API Integrator

The data-layer specialist for AI endpoints. Builds clean Retrofit/Ktor clients with safe error handling and zero secret leakage.

## When to Use

- Integrating Gemini, Claude, OpenAI, HuggingFace, Replicate, fal.ai
- Implementing streaming (SSE) responses for chat / generation
- Setting up auth, retry, backoff, and rate-limit handling
- Replacing ad-hoc `HttpURLConnection` calls with a typed client

## Layering

```
ViewModel → UseCase → Repository → ApiService (Retrofit/Ktor) → Endpoint
```

Repositories return domain models. ApiService returns DTOs. Mappers live in `data/mapper/`.

## Retrofit Skeleton (Kotlin)

```kotlin
interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Body req: ChatRequest): ChatResponse

    @Streaming
    @POST("v1/chat/completions")
    suspend fun chatStream(@Body req: ChatRequest): ResponseBody
}

@Provides @Singleton
fun provideOpenAi(client: OkHttpClient): OpenAiApi =
    Retrofit.Builder()
        .baseUrl(BuildConfig.OPENAI_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create()
```

## OkHttp Pipeline

```kotlin
OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor(BuildConfig.OPENAI_KEY))
    .addInterceptor(RetryInterceptor(maxAttempts = 3))
    .addInterceptor(HttpLoggingInterceptor().setLevel(BASIC))  // never BODY in prod
    .connectTimeout(15, SECONDS)
    .readTimeout(60, SECONDS)
    .build()
```

## Token Management

- Keys live in `local.properties` → exposed via `buildConfigField("String", "OPENAI_KEY", ...)`
- Never in source, never in resources, never in committed config
- For production: rotate via secure remote config or a backend proxy
- Long-lived keys → use a backend proxy; don't ship them with the app

## Retry / Backoff

```kotlin
class RetryInterceptor(private val maxAttempts: Int) : Interceptor {
    override fun intercept(chain: Chain): Response {
        var attempt = 0
        var lastError: IOException? = null
        while (attempt < maxAttempts) {
            try {
                val resp = chain.proceed(chain.request())
                if (resp.code in 500..599 || resp.code == 429) {
                    resp.close()
                    val delay = expBackoff(attempt) + jitter()
                    Thread.sleep(delay)
                    attempt++
                    continue
                }
                return resp
            } catch (e: IOException) {
                lastError = e
                attempt++
            }
        }
        throw lastError ?: IOException("Retries exhausted")
    }
}
```

Honor `Retry-After` header for 429.

## Streaming (SSE)

```kotlin
fun streamChat(req: ChatRequest): Flow<ChatChunk> = flow {
    api.chatStream(req).source().use { source ->
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data: ")) {
                val payload = line.removePrefix("data: ")
                if (payload == "[DONE]") return@flow
                emit(json.decodeFromString<ChatChunk>(payload))
            }
        }
    }
}.flowOn(Dispatchers.IO)
```

## Sealed Result Type

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class HttpError(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>()
    object RateLimited : ApiResult<Nothing>()
}
```

Repositories return `ApiResult<DomainModel>`. ViewModels handle each branch explicitly.

## Output

Per micro-task:
- `XxxApi.kt` interface (Retrofit) + request/response DTOs
- `XxxApiModule.kt` (Hilt) with OkHttp + Retrofit setup
- `AuthInterceptor.kt`, `RetryInterceptor.kt`
- `XxxRepository.kt` with mapper + `ApiResult` return type
- `local.properties` template with required keys (never the real values)

## Anti-Patterns

- API keys in source / resources / committed files
- `Body` logging interceptor in production
- Synchronous network on main thread
- Returning raw DTOs from repositories
- No `Retry-After` handling on 429
- Streaming without `flowOn(Dispatchers.IO)`
- Throwing exceptions across repository → ViewModel boundary
