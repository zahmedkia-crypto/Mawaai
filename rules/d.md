# testing-master-ultra-detailed.md
# ══════════════════════════════════════════════════════════
# SAVE LOCATION: Same as main-developer-role (see that file)
# PURPOSE: Write comprehensive tests for 100% critical path coverage
# ══════════════════════════════════════════════════════════

---

## YOUR ROLE: Android Test Engineer

> **Rule:** Every bug that reaches production represents a missing test.
> **Standard:** Test behavior, not implementation. Tests must survive refactoring.
> **Goal:** 100% coverage of critical paths (ViewModel, Repository, Room DAO).

---

## TESTING PYRAMID FOR ANDROID

```
              ╔═══════════════╗
              ║  UI / E2E      ║  ~10%  Espresso, UI Automator
              ║  (Slowest)     ║
         ╔════╩═══════════════╩════╗
         ║  Integration Tests      ║  ~20%  Room DB, API with MockServer
         ║  (Medium speed)         ║
    ╔════╩═════════════════════════╩════╗
    ║       UNIT TESTS                   ║  ~70%  JUnit5 + MockK (Kotlin)
    ║       (Fastest — run constantly)   ║        JUnit4 + Mockito (Java)
    ╚════════════════════════════════════╝
```

---

## MANDATORY SETUP: Test Dependencies

**Verify these exist in `app/build.gradle.kts`:**

```kotlin
dependencies {
    // ── Unit Testing ─────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    
    // ── Android/Instrumented Testing ──────────────
    androidTestImplementation("androidx.test.ext:junit:1.2.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.0")
    androidTestImplementation("io.mockk:mockk-android:1.13.11")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
    
    // ── Compose UI Testing ────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // ── Room Testing ──────────────────────────────
    testImplementation("androidx.room:room-testing:2.6.1")
}
```

---

## PART 1: UNIT TESTS — COMPLETE TEMPLATES

### Template A: ViewModel Test (Kotlin — JUnit5 + MockK + Turbine)

```kotlin
// ════════════════════════════════════════════════════════════
// FILE: app/src/test/java/com/yourapp/presentation/UserViewModelTest.kt
// ════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension::class)
class UserViewModelTest {

    // ── Mocks ────────────────────────────────────────────────
    @MockK private lateinit var userRepository: UserRepository
    @MockK private lateinit var analyticsTracker: AnalyticsTracker

    // ── System Under Test ─────────────────────────────────────
    private lateinit var viewModel: UserViewModel

    // ── Test Coroutine Dispatcher ─────────────────────────────
    // MANDATORY: Replace Main dispatcher with test version
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)  // ← REQUIRED for viewModelScope
        viewModel = UserViewModel(userRepository, analyticsTracker)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()  // ← REQUIRED: restore after each test
        unmockkAll()
    }

    // ══════════════════════════════════════════════════════════
    // HAPPY PATH TESTS
    // ══════════════════════════════════════════════════════════

    @Test
    fun `GIVEN valid userId WHEN loadUser called THEN state transitions Loading then Success`() = runTest {
        // GIVEN
        val userId = "user_42"
        val expectedUser = User(id = userId, name = "Ahmed", email = "ahmed@example.com")
        coEvery { userRepository.getUser(userId) } returns expectedUser

        // WHEN + THEN — use Turbine for Flow assertions
        viewModel.uiState.test {
            viewModel.loadUser(userId)

            assertEquals(UiState.Idle, awaitItem())              // initial
            assertEquals(UiState.Loading, awaitItem())           // loading started
            assertEquals(UiState.Success(expectedUser), awaitItem())  // success

            cancelAndIgnoreRemainingEvents()
        }

        // Verify exactly one API call was made
        coVerify(exactly = 1) { userRepository.getUser(userId) }
    }

    @Test
    fun `GIVEN user loaded WHEN loadUser called again THEN refreshes data`() = runTest {
        val user = User("1", "Ahmed")
        coEvery { userRepository.getUser("1") } returns user

        viewModel.loadUser("1")
        advanceUntilIdle()
        viewModel.loadUser("1")
        advanceUntilIdle()

        coVerify(exactly = 2) { userRepository.getUser("1") }
    }

    // ══════════════════════════════════════════════════════════
    // ERROR PATH TESTS
    // ══════════════════════════════════════════════════════════

    @Test
    fun `GIVEN network offline WHEN loadUser THEN emits Error with connection message`() = runTest {
        // GIVEN
        coEvery { userRepository.getUser(any()) } throws IOException("Connection refused")

        // WHEN
        viewModel.loadUser("user_1")
        advanceUntilIdle()

        // THEN
        val state = viewModel.uiState.value
        assertIs<UiState.Error>(state)
        assertTrue(state.message.contains("connection", ignoreCase = true) ||
                   state.message.contains("internet", ignoreCase = true))
        // Verify analytics NOT called on error (important business rule)
        verify(exactly = 0) { analyticsTracker.trackUserViewed(any()) }
    }

    @Test
    fun `GIVEN server returns 404 WHEN loadUser THEN emits user-friendly error (not raw HTTP code)`() = runTest {
        // GIVEN
        coEvery { userRepository.getUser(any()) } throws HttpException(
            Response.error<User>(404, "Not found".toResponseBody())
        )

        // WHEN
        viewModel.loadUser("nonexistent")
        advanceUntilIdle()

        // THEN
        val state = viewModel.uiState.value
        assertIs<UiState.Error>(state)
        // ← Verify user sees a friendly message, NOT "404" or raw HTTP error
        assertFalse(state.message.contains("404"))
        assertFalse(state.message.contains("HTTP"))
    }

    @Test
    fun `GIVEN server returns 500 WHEN loadUser THEN emits server error message`() = runTest {
        coEvery { userRepository.getUser(any()) } throws HttpException(
            Response.error<User>(500, "Internal Server Error".toResponseBody())
        )

        viewModel.loadUser("user_1")
        advanceUntilIdle()

        assertIs<UiState.Error>(viewModel.uiState.value)
    }

    // ══════════════════════════════════════════════════════════
    // EDGE CASE TESTS
    // ══════════════════════════════════════════════════════════

    @Test
    fun `GIVEN empty userId WHEN loadUser THEN does not call repository`() = runTest {
        // WHEN
        viewModel.loadUser("")
        advanceUntilIdle()

        // THEN — no network call for invalid input
        coVerify(exactly = 0) { userRepository.getUser(any()) }
        assertIs<UiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `GIVEN whitespace-only userId WHEN loadUser THEN validates and rejects`() = runTest {
        viewModel.loadUser("   ")
        advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.getUser(any()) }
    }

    @Test
    fun `GIVEN rapid consecutive calls WHEN loadUser THEN only last result shown`() = runTest {
        val user2 = User("2", "Mohammed")
        coEvery { userRepository.getUser("1") } coAnswers {
            delay(5000)  // slow first call
            User("1", "Ahmed")
        }
        coEvery { userRepository.getUser("2") } returns user2

        // Rapid calls
        viewModel.loadUser("1")
        advanceTimeBy(50)
        viewModel.loadUser("2")  // should cancel the first
        advanceUntilIdle()

        // Only second result visible
        assertEquals(UiState.Success(user2), viewModel.uiState.value)
    }

    @Test
    fun `GIVEN userId with special characters WHEN loadUser THEN handles correctly`() = runTest {
        val specialId = "user@domain.com"
        val user = User(specialId, "Ahmed")
        coEvery { userRepository.getUser(specialId) } returns user

        viewModel.loadUser(specialId)
        advanceUntilIdle()

        assertEquals(UiState.Success(user), viewModel.uiState.value)
    }
}
```

---

### Template B: Repository Test (Unit — Mocked DAO + API)

```kotlin
// ════════════════════════════════════════════════════════════
// FILE: app/src/test/java/com/yourapp/data/UserRepositoryTest.kt
// ════════════════════════════════════════════════════════════

class UserRepositoryTest {

    @MockK private lateinit var userApi: UserApiService
    @MockK private lateinit var userDao: UserDao
    private lateinit var repository: UserRepositoryImpl

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        repository = UserRepositoryImpl(userApi, userDao)
    }

    @AfterEach
    fun tearDown() { unmockkAll() }

    @Test
    fun `GIVEN API success WHEN getUser THEN caches result in Room and returns it`() = runTest {
        // GIVEN
        val user = User("1", "Ahmed")
        coEvery { userApi.getUser("1") } returns user
        coEvery { userDao.insertUser(user) } just Runs

        // WHEN
        val result = repository.getUser("1")

        // THEN — returns correct data
        assertEquals(user, result)

        // THEN — caches result in database
        coVerify(exactly = 1) { userDao.insertUser(user) }
    }

    @Test
    fun `GIVEN API fails but cache exists WHEN getUser THEN returns cached data`() = runTest {
        // GIVEN — network fails, cache available
        coEvery { userApi.getUser("1") } throws IOException("Offline")
        coEvery { userDao.getUser("1") } returns User("1", "Ahmed (cached)")

        // WHEN
        val result = repository.getUser("1")

        // THEN — gracefully serves from cache
        assertEquals("Ahmed (cached)", result.name)
    }

    @Test
    fun `GIVEN both API and cache fail WHEN getUser THEN throws exception`() = runTest {
        // GIVEN — everything fails
        coEvery { userApi.getUser("1") } throws IOException("Offline")
        coEvery { userDao.getUser("1") } returns null

        // WHEN + THEN
        assertThrows<NoDataException> {
            repository.getUser("1")
        }
    }
}
```

---

## PART 2: ROOM DATABASE INTEGRATION TESTS

```kotlin
// ════════════════════════════════════════════════════════════
// FILE: app/src/androidTest/java/com/yourapp/data/local/UserDaoTest.kt
// ════════════════════════════════════════════════════════════

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        // In-memory database — fast, isolated, auto-destroyed after test
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        userDao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUser_thenGetUser_returnsCorrectUser() = runBlocking {
        val user = User(id = "1", name = "Ahmed", email = "ahmed@example.com")

        userDao.insertUser(user)
        val retrieved = userDao.getUser("1")

        assertNotNull(retrieved)
        assertEquals(user, retrieved)
    }

    @Test
    fun insertDuplicateUser_replacesExistingUser() = runBlocking {
        val original = User("1", "Ahmed", "ahmed@example.com")
        val updated = User("1", "Ahmed Updated", "new@example.com")

        userDao.insertUser(original)
        userDao.insertUser(updated)  // same ID → should replace

        val retrieved = userDao.getUser("1")
        assertEquals("Ahmed Updated", retrieved?.name)
        assertEquals("new@example.com", retrieved?.email)
    }

    @Test
    fun deleteUser_thenGet_returnsNull() = runBlocking {
        val user = User("1", "Ahmed")
        userDao.insertUser(user)
        userDao.deleteUser("1")

        val retrieved = userDao.getUser("1")
        assertNull(retrieved)
    }

    @Test
    fun observeAllUsers_emitsNewListOnInsert() = runTest {
        userDao.observeAllUsers().test {
            // Initial state: empty list
            assertEquals(emptyList<User>(), awaitItem())

            // After insert: list with one user
            userDao.insertUser(User("1", "Ahmed"))
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Ahmed", list[0].name)

            // After second insert
            userDao.insertUser(User("2", "Mohammed"))
            val updatedList = awaitItem()
            assertEquals(2, updatedList.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllUsers_withEmptyDatabase_returnsEmptyList() = runBlocking {
        val users = userDao.getAllUsers()
        assertTrue(users.isEmpty())
    }
}
```

---

## PART 3: COMPOSE UI TESTS (Arabic + English)

```kotlin
// ════════════════════════════════════════════════════════════
// FILE: app/src/androidTest/java/com/yourapp/presentation/LoginScreenTest.kt
// ════════════════════════════════════════════════════════════

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var fakeRepository: FakeUserRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // ── English LTR Tests ─────────────────────────────────────

    @Test
    fun loginButton_isDisplayed_inEnglish() {
        composeTestRule
            .onNodeWithText("Login")
            .assertIsDisplayed()
    }

    @Test
    fun loginButton_clickWithEmptyEmail_showsValidationError() {
        composeTestRule
            .onNodeWithTag("login_button")
            .performClick()

        composeTestRule
            .onNodeWithTag("email_error")
            .assertIsDisplayed()
    }

    @Test
    fun loginFlow_withValidCredentials_navigatesToHome() {
        composeTestRule
            .onNodeWithTag("email_field")
            .performTextInput("test@example.com")

        composeTestRule
            .onNodeWithTag("password_field")
            .performTextInput("password123")

        composeTestRule
            .onNodeWithTag("login_button")
            .performClick()

        composeTestRule.waitUntil(5000L) {
            composeTestRule
                .onAllNodesWithTag("home_screen")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ── Arabic RTL Tests ──────────────────────────────────────

    @Test
    fun loginButton_isDisplayed_inArabic_with_correct_RTL_text() {
        // Switch to Arabic using the correct modern API
        composeTestRule.activityRule.scenario.onActivity { activity ->
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("ar")
            )
        }
        composeTestRule.waitForIdle()

        // Verify Arabic text appears
        composeTestRule
            .onNodeWithText("تسجيل الدخول")
            .assertIsDisplayed()
    }

    @Test
    fun screen_layoutDirection_isRTL_when_Arabic() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("ar")
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("login_screen_root")
            .assert(
                SemanticsMatcher("layoutDirection is RTL") { node ->
                    node.layoutInfo.localToWindowMatrix.values[0] < 0  // Flipped X
                }
            )
    }

    // ── Error State Tests ─────────────────────────────────────

    @Test
    fun loginScreen_networkError_showsRetryButton() {
        fakeRepository.setShouldReturnError(true)

        composeTestRule
            .onNodeWithTag("email_field")
            .performTextInput("test@example.com")
        composeTestRule
            .onNodeWithTag("password_field")
            .performTextInput("password123")
        composeTestRule
            .onNodeWithTag("login_button")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("error_message")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("retry_button")
            .assertIsDisplayed()
    }
}
```

---

## PART 4: FAKE IMPLEMENTATIONS (Preferred over Mocks for Integration Tests)

```kotlin
// ════════════════════════════════════════════════════════════
// FILE: app/src/test/java/com/yourapp/fakes/FakeUserRepository.kt
// ════════════════════════════════════════════════════════════

/**
 * Fake implementation of UserRepository for testing.
 * - Use in unit tests instead of mocking the interface
 * - Use in UI tests as a Hilt test module replacement
 */
@Singleton
class FakeUserRepository @Inject constructor() : UserRepository {

    private val users = mutableMapOf<String, User>()
    private var shouldReturnError = false
    private val _usersFlow = MutableStateFlow<List<User>>(emptyList())

    // ── Test Setup Methods ────────────────────────────────────

    fun addUser(user: User) {
        users[user.id] = user
        _usersFlow.value = users.values.toList()
    }

    fun setUsers(newUsers: List<User>) {
        users.clear()
        newUsers.forEach { users[it.id] = it }
        _usersFlow.value = users.values.toList()
    }

    fun setShouldReturnError(error: Boolean) {
        shouldReturnError = error
    }

    fun clear() {
        users.clear()
        shouldReturnError = false
        _usersFlow.value = emptyList()
    }

    // ── Repository Interface Implementation ───────────────────

    override suspend fun getUser(id: String): User {
        if (shouldReturnError) throw IOException("Fake network error")
        return users[id] ?: throw NoSuchElementException("User not found: $id")
    }

    override fun observeUsers(): Flow<List<User>> = _usersFlow.asStateFlow()

    override suspend fun saveUser(user: User) {
        if (shouldReturnError) throw IOException("Fake save error")
        users[user.id] = user
        _usersFlow.value = users.values.toList()
    }

    override suspend fun deleteUser(id: String) {
        users.remove(id)
        _usersFlow.value = users.values.toList()
    }
}
```

---

## MANDATORY TEST NAMING CONVENTION

```kotlin
// ════════════════════════════════════════════════════════════
// FORMAT: `GIVEN [context] WHEN [action] THEN [expected outcome]`
// Use BACKTICK syntax for spaces in test names — this is valid Kotlin!
// ════════════════════════════════════════════════════════════

// ✅ CORRECT — readable, documents behavior, valid Kotlin:
@Test
fun `GIVEN valid email WHEN login called THEN navigates to home`() { }

@Test
fun `GIVEN empty list WHEN loadUsers called THEN shows empty state`() { }

@Test
fun `GIVEN network offline WHEN retry clicked THEN shows loading then error`() { }

// ❌ WRONG — spaces in function name WITHOUT backticks → COMPILATION ERROR:
@Test
fun test login success() { }  // DOES NOT COMPILE — missing backticks

// ❌ WRONG — unclear, no context:
@Test
fun testLogin() { }

@Test
fun test1() { }
```

---

## RUN ALL TESTS — COMMANDS

```bash
# Unit tests only (fast — run during development)
./gradlew testDebugUnitTest

# Unit tests with HTML coverage report
./gradlew testDebugUnitTestCoverage
# Report at: app/build/reports/coverage/test/debug/index.html

# Instrumented tests (requires device or emulator running)
./gradlew connectedDebugAndroidTest

# Full CI pipeline (run before every PR)
./gradlew clean testDebugUnitTest connectedDebugAndroidTest lint

# Run a specific test class:
./gradlew :app:testDebugUnitTest --tests "com.yourapp.UserViewModelTest"

# Run a specific test function (backtick names work too):
./gradlew :app:testDebugUnitTest --tests "com.yourapp.UserViewModelTest.GIVEN valid userId*"
```

**All commands must end with:**
```
BUILD SUCCESSFUL in Xs
```
**Any other result = do not merge, do not ship.**
