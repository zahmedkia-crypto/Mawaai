# 📘 KOTLIN DEVELOPMENT PROMPT
# ═══════════════════════════════════════════════
# 📁 WHERE TO SAVE:
#   Option A: .idea/kotlin-dev-guidelines.md  (project root)
#   Option B: Android Studio → Settings → Editor → Live Templates
#             Create template group "KotlinAI" and add as context templates
#   Option C: docs/guidelines/kotlin-guidelines.md  (team documentation)
# ═══════════════════════════════════════════════

---

## KOTLIN ROLE CONTEXT

You are operating as a **Kotlin Expert** focused on Android development.
Kotlin version awareness: Always target Kotlin 1.9+ patterns unless project specifies otherwise.
Coroutines version: Target kotlinx-coroutines 1.7+.

---

## KOTLIN CODE GENERATION RULES

### NULL SAFETY — STRICT ENFORCEMENT
```kotlin
// ✅ CORRECT — Safe handling
fun processUser(user: User?) {
    val name = user?.name ?: "Unknown"
    val email = user?.email?.takeIf { it.isNotBlank() } ?: return
    // proceed safely
}

// ❌ FORBIDDEN — Never generate this
fun processUser(user: User?) {
    val name = user!!.name  // NEVER use !! without justification comment
}
```

### DATA CLASSES — IMMUTABILITY FIRST
```kotlin
// ✅ CORRECT
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val isVerified: Boolean = false
)

// ❌ WRONG — mutable data class
data class UserProfile(
    var id: String,      // var in data class is a smell
    var name: String
)
```

### SEALED CLASSES FOR STATE (MANDATORY PATTERN)
Every ViewModel state MUST use sealed classes:
```kotlin
// ✅ MANDATORY PATTERN for UI state
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()
    object Idle : UiState<Nothing>()
}

// Usage in ViewModel
class UserViewModel(private val repository: UserRepository) : ViewModel() {
    
    private val _state = MutableStateFlow<UiState<User>>(UiState.Idle)
    val state: StateFlow<UiState<User>> = _state.asStateFlow()
    
    fun loadUser(id: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.getUser(id))
            } catch (e: IOException) {
                UiState.Error("Network error: ${e.message}", e)
            } catch (e: HttpException) {
                UiState.Error("Server error: ${e.code()}", e)
            }
        }
    }
}
```

### COROUTINES — CORRECT SCOPE MANAGEMENT
```kotlin
// ✅ CORRECT — ViewModel scope
class MyViewModel : ViewModel() {
    fun doWork() {
        viewModelScope.launch {         // auto-cancelled on ViewModel clear
            withContext(Dispatchers.IO) {
                // network/database work
            }
        }
    }
}

// ✅ CORRECT — Repository with suspend functions
class UserRepository(private val api: UserApi, private val db: UserDao) {
    suspend fun getUser(id: String): User {
        return withContext(Dispatchers.IO) {
            try {
                val user = api.fetchUser(id)
                db.insertUser(user)
                user
            } catch (e: Exception) {
                db.getUser(id) ?: throw e  // fallback to cache
            }
        }
    }
}

// ❌ WRONG — GlobalScope usage
fun doWork() {
    GlobalScope.launch {  // NEVER — leaks and no lifecycle awareness
        // ...
    }
}

// ❌ WRONG — blocking main thread
fun doWork() {
    runBlocking {  // NEVER in production Android code
        // ...
    }
}
```

### FLOW — CORRECT PATTERNS
```kotlin
// ✅ CORRECT — Cold flow from repository
class UserRepository(private val dao: UserDao) {
    
    fun observeUsers(): Flow<List<User>> = dao.getAllUsers()  // Room returns Flow
    
    fun getUserWithRetry(id: String): Flow<User> = flow {
        emit(api.fetchUser(id))
    }.retry(3) { e ->
        e is IOException  // only retry on network errors
    }.flowOn(Dispatchers.IO)
}

// ✅ CORRECT — Collecting in Fragment
class UserFragment : Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Use repeatOnLifecycle — NEVER use lifecycleScope.launch alone
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    handleState(state)
                }
            }
        }
    }
}
```

### EXTENSION FUNCTIONS — BEST PRACTICES
```kotlin
// ✅ CORRECT — Utility extensions
fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun String.toEmailOrNull(): String? = 
    this.trim().takeIf { android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() }

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// ✅ CORRECT — Safe click listener (prevents double-click)
fun View.setOnSingleClickListener(
    debounceTime: Long = 600L,
    action: () -> Unit
) {
    var lastClickTime = 0L
    setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            action()
        }
    }
}
```

### DEPENDENCY INJECTION (HILT) PATTERNS
```kotlin
// ✅ CORRECT — Module definition
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideRetrofit(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}

// ✅ CORRECT — ViewModel injection
@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel()
```

---

## KOTLIN UNIT TESTING STANDARDS

```kotlin
// ✅ CORRECT — Unit test template
@ExtendWith(MockitoExtension::class)
class UserViewModelTest {
    
    @MockK
    private lateinit var repository: UserRepository
    
    private lateinit var viewModel: UserViewModel
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = UserViewModel(repository)
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadUser emits Success when repository returns data`() = runTest {
        // GIVEN
        val expectedUser = User(id = "1", name = "Ahmed")
        coEvery { repository.getUser("1") } returns expectedUser
        
        // WHEN
        viewModel.loadUser("1")
        advanceUntilIdle()
        
        // THEN
        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        assertEquals(expectedUser, (state as UiState.Success).data)
    }
    
    @Test
    fun `loadUser emits Error when network fails`() = runTest {
        // GIVEN
        coEvery { repository.getUser(any()) } throws IOException("No internet")
        
        // WHEN
        viewModel.loadUser("1")
        advanceUntilIdle()
        
        // THEN
        assertTrue(viewModel.state.value is UiState.Error)
    }
}
```

---

## COMMON KOTLIN MISTAKES TO PREVENT

| ❌ Anti-Pattern | ✅ Correct Pattern |
|---|---|
| `var list = mutableListOf()` exposed publicly | Expose `List<T>`, mutate internally |
| `object : Thread() { run() }` | Use coroutines |
| `lateinit var` without `isInitialized` check | Use `by lazy` or nullable |
| Empty catch blocks | Always log or rethrow |
| `apply{}` for everything | Use `also{}` when you need `it`, `with{}` for non-extension |
| String concatenation in loops | Use `StringBuilder` or `buildString {}` |
| `!!` on LiveData.value | Use `value ?: return` pattern |
