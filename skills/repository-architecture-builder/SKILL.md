---
name: repository-architecture-builder
description: Builds clean MVVM/MVI data layers for Android (Kotlin), Flutter, and RN apps. Use for architecture refactors, data-layer cleanup, dependency injection setup, use-case definition, and enforcing layering boundaries. Produces Repository interfaces, UseCases, domain models, mappers, DI modules (Hilt/Koin), and StateFlow/Riverpod/Redux state contracts. Refuses leaking UI types into the data layer or framework types into ViewModels.
icon: layers-3
color: Green
---

# Repository Architecture Builder

Owns the layering. Enforces strict boundaries between UI ↔ ViewModel ↔ UseCase ↔ Repository ↔ Data Source.

## When to Use

- Architecture refactor (existing code is tangled)
- Setting up a clean data layer from scratch
- Adding a new feature that needs a repository + use-case
- DI module reorganization
- Migrating from one state library to another

## Canonical Layering

```
ui/ (Composables, Fragments)
  ↓ observes
viewmodel/ (StateFlow<UiState>)
  ↓ calls
usecase/ (single-method classes, business rules)
  ↓ calls
repository/ (interface in domain/, impl in data/)
  ↓ calls
datasource/ (remote, local, cache)
  ↓
api/ + db/ + memory/
```

## Hard Boundaries

| Layer | Allowed | Forbidden |
|---|---|---|
| UI | Composables, UiState read | Repository calls, framework references in ViewModel |
| ViewModel | UseCases, UiState write, `viewModelScope` | Direct API calls, `Context` ownership, framework imports |
| UseCase | Repositories, domain models | DTOs, network types, Android types |
| Repository (impl) | DataSources, mappers | UI types, ViewModel references |
| DataSource | One source (API or DB) | Cross-source calls |

## Repository Pattern

```kotlin
// domain/repository/TemplateRepository.kt
interface TemplateRepository {
    suspend fun list(): ApiResult<List<Template>>
    suspend fun byId(id: TemplateId): ApiResult<Template>
    fun observe(): Flow<List<Template>>
}

// data/repository/TemplateRepositoryImpl.kt
class TemplateRepositoryImpl @Inject constructor(
    private val remote: TemplateRemoteDataSource,
    private val local: TemplateLocalDataSource,
    private val mapper: TemplateMapper,
) : TemplateRepository {
    override suspend fun list(): ApiResult<List<Template>> = withContext(Dispatchers.IO) {
        when (val r = remote.list()) {
            is ApiResult.Success -> {
                local.upsertAll(r.data)
                ApiResult.Success(r.data.map(mapper::toDomain))
            }
            is ApiResult.HttpError -> local.list()
                ?.let { ApiResult.Success(it.map(mapper::toDomain)) }
                ?: r
            else -> r
        }
    }
    // ...
}
```

## UseCase Pattern

One responsibility per class. `operator fun invoke` for ergonomics.

```kotlin
class GenerateDesignUseCase @Inject constructor(
    private val vision: VisionAnalyzer,
    private val templates: TemplateRepository,
    private val pipeline: AiPipeline,
) {
    suspend operator fun invoke(sketch: Bitmap, hint: UserHint): ApiResult<FinalComposite> {
        val analysis = vision.analyze(sketch, hint).getOrElse { return ApiResult.NetworkError(it) }
        val match = templates.match(analysis).getOrElse { return it.asError() }
        return pipeline.run(analysis, match)
    }
}
```

## ViewModel Pattern (MVI-ish StateFlow)

```kotlin
@HiltViewModel
class DesignViewModel @Inject constructor(
    private val generate: GenerateDesignUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(DesignUiState())
    val state: StateFlow<DesignUiState> = _state.asStateFlow()

    fun onSketchSelected(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            when (val r = generate(bitmap, _state.value.hint)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, result = r.data) }
                else -> _state.update { it.copy(loading = false, error = r.asMessage()) }
            }
        }
    }
}

data class DesignUiState(
    val loading: Boolean = false,
    val result: FinalComposite? = null,
    val error: String? = null,
    val hint: UserHint = UserHint.Default,
)
```

## Hilt Module Pattern

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class TemplateModule {
    @Binds @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository
}
```

One module per feature or per layer. Never a god module.

## Mappers

```kotlin
class TemplateMapper @Inject constructor() {
    fun toDomain(dto: TemplateDto): Template = ...
    fun toEntity(domain: Template): TemplateEntity = ...
}
```

Mappers are pure functions, fully unit tested.

## Output

Per micro-task:
- `domain/repository/XxxRepository.kt` (interface)
- `data/repository/XxxRepositoryImpl.kt`
- `domain/usecase/XxxUseCase.kt`
- `data/mapper/XxxMapper.kt`
- `di/XxxModule.kt`
- ViewModel + UiState if requested
- Unit test for mapper and use-case

## Anti-Patterns

- Repository returning UI models
- ViewModel referencing `Context`, `Activity`, or `View`
- UseCase calling APIs directly
- DTOs leaking past the repository boundary
- One mega `AppModule.kt` for all DI
- Mutable state outside ViewModel
- `runBlocking` in any layer
