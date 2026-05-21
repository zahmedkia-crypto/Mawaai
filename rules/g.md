# ☕ JAVA DEVELOPMENT PROMPT (Android)
# ═══════════════════════════════════════════════
# 📁 WHERE TO SAVE:
#   Option A: .idea/java-dev-guidelines.md  (project root)
#   Option B: docs/guidelines/java-guidelines.md
#   Option C: Android Studio → Settings → Editor → Inspections
#             Import custom inspection profile: java-inspection-profile.xml
# ═══════════════════════════════════════════════

---

## JAVA ROLE CONTEXT

You are operating as a **Java Android Expert**.
Target: Java 8+ (lambdas, streams, Optional where available in Android API 24+).
Annotation processor awareness: Dagger 2, Room, Retrofit, Lombok if used.

---

## NULL SAFETY IN JAVA — MANDATORY ANNOTATIONS

```java
// ✅ MANDATORY — Always annotate nullability
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UserRepository {
    
    @NonNull
    public User getUser(@NonNull String userId) {
        // guaranteed non-null input and output
    }
    
    @Nullable
    public User findUser(@NonNull String email) {
        // might return null — caller must check
    }
    
    // ✅ CORRECT — Null check pattern
    public void processUser(@Nullable User user) {
        if (user == null) {
            Log.w(TAG, "processUser: user is null, skipping");
            return;
        }
        // safe to use user here
    }
}
```

---

## RESOURCE MANAGEMENT — CRITICAL RULES

```java
// ✅ CORRECT — Try-with-resources (Java 7+)
public List<User> readUsersFromFile(@NonNull File file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        // reader auto-closed even if exception thrown
        return parseUsers(reader);
    }
}

// ✅ CORRECT — Cursor management (Android-specific)
@Nullable
public User queryUser(@NonNull String id) {
    Cursor cursor = null;
    try {
        cursor = database.query(
            "users", null, "id = ?", 
            new String[]{id}, null, null, null
        );
        if (cursor != null && cursor.moveToFirst()) {
            return mapCursorToUser(cursor);
        }
        return null;
    } finally {
        if (cursor != null) {
            cursor.close();  // ALWAYS close cursor
        }
    }
}

// ❌ WRONG — Resource leak
public List<User> readUsers(File file) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    return parseUsers(reader);  // reader never closed if exception thrown!
}
```

---

## THREADING MODEL — CORRECT ANDROID JAVA PATTERNS

```java
// ✅ CORRECT — ExecutorService for background work
public class UserRepository {
    
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public void loadUser(String id, UserCallback callback) {
        executor.execute(() -> {
            try {
                User user = api.fetchUser(id).execute().body();
                mainHandler.post(() -> callback.onSuccess(user));
            } catch (IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}

// ✅ CORRECT — Interface for callbacks
public interface UserCallback {
    void onSuccess(@NonNull User user);
    void onError(@NonNull String message);
}

// ✅ CORRECT — AsyncTask replacement (AsyncTask is deprecated API 30)
// Use this instead:
public class FetchUserTask {
    
    private final ExecutorService executor;
    private final Handler handler;
    
    public FetchUserTask() {
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void execute(String userId, UserCallback callback) {
        executor.execute(() -> {
            // Background work
            User result = performNetworkCall(userId);
            // Post to main thread
            handler.post(() -> callback.onSuccess(result));
        });
    }
}

// ❌ DEPRECATED — Never use in new code
private class FetchUser extends AsyncTask<String, Void, User> {
    // DEPRECATED since API 30 — do not use
}
```

---

## MEMORY LEAK PREVENTION — JAVA

```java
// ✅ CORRECT — WeakReference pattern for callbacks
public class ImageLoader {
    
    public void loadImage(
        @NonNull ImageView imageView, 
        @NonNull String url
    ) {
        WeakReference<ImageView> weakImageView = new WeakReference<>(imageView);
        
        executor.execute(() -> {
            Bitmap bitmap = downloadBitmap(url);
            handler.post(() -> {
                ImageView view = weakImageView.get();
                if (view != null) {  // view might be gone
                    view.setImageBitmap(bitmap);
                }
            });
        });
    }
}

// ✅ CORRECT — Static inner class (avoids implicit outer reference)
public class MyActivity extends AppCompatActivity {
    
    private Handler handler;
    
    // ✅ Static — no implicit reference to Activity
    private static class MyHandler extends Handler {
        private final WeakReference<MyActivity> activityRef;
        
        MyHandler(MyActivity activity) {
            super(Looper.getMainLooper());
            this.activityRef = new WeakReference<>(activity);
        }
        
        @Override
        public void handleMessage(@NonNull Message msg) {
            MyActivity activity = activityRef.get();
            if (activity != null && !activity.isFinishing()) {
                // safe to use activity
            }
        }
    }
    
    // ❌ WRONG — Non-static inner class holds implicit Activity reference
    private class LeakyHandler extends Handler {
        // This prevents Activity from being garbage collected!
    }
}
```

---

## JAVA GENERICS — CORRECT USAGE

```java
// ✅ CORRECT — Bounded generics
public class ApiResponse<T> {
    @Nullable private final T data;
    @Nullable private final String error;
    private final int code;
    
    private ApiResponse(@Nullable T data, @Nullable String error, int code) {
        this.data = data;
        this.error = error;
        this.code = code;
    }
    
    public static <T> ApiResponse<T> success(@NonNull T data) {
        return new ApiResponse<>(data, null, 200);
    }
    
    public static <T> ApiResponse<T> error(@NonNull String message, int code) {
        return new ApiResponse<>(null, message, code);
    }
    
    public boolean isSuccessful() { return error == null && data != null; }
    
    @Nullable public T getData() { return data; }
    @Nullable public String getError() { return error; }
}

// ❌ WRONG — Raw types
List list = new ArrayList();  // RAW TYPE — never use
Map map = new HashMap();      // RAW TYPE — always specify <K, V>
```

---

## JAVA UNIT TESTING STANDARDS

```java
// ✅ CORRECT — JUnit 4 with Mockito (most common in Android Java projects)
@RunWith(MockitoJUnitRunner.class)
public class UserViewModelTest {
    
    @Mock
    private UserRepository mockRepository;
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    private UserViewModel viewModel;
    
    @Before
    public void setUp() {
        viewModel = new UserViewModel(mockRepository);
    }
    
    @Test
    public void loadUser_success_updatesLiveData() throws Exception {
        // GIVEN
        User expectedUser = new User("1", "Ahmed", "ahmed@example.com");
        when(mockRepository.getUser("1")).thenReturn(expectedUser);
        
        // Observer to capture LiveData
        Observer<UiState> observer = mock(Observer.class);
        viewModel.getState().observeForever(observer);
        
        // WHEN
        viewModel.loadUser("1");
        
        // THEN
        verify(observer).onChanged(argThat(state -> 
            state instanceof UiState.Success &&
            ((UiState.Success) state).getData().equals(expectedUser)
        ));
        
        viewModel.getState().removeObserver(observer);
    }
    
    @Test
    public void loadUser_networkError_emitsErrorState() throws Exception {
        // GIVEN
        when(mockRepository.getUser(anyString()))
            .thenThrow(new IOException("Network unavailable"));
        
        // WHEN
        viewModel.loadUser("1");
        
        // THEN
        UiState state = viewModel.getState().getValue();
        assertNotNull(state);
        assertTrue(state instanceof UiState.Error);
    }
}
```

---

## JAVA ANTI-PATTERNS TABLE

| ❌ Anti-Pattern | ✅ Replacement |
|---|---|
| `AsyncTask` | `ExecutorService` + `Handler` |
| Non-static inner Handler | Static inner class + WeakReference |
| Raw generic types `List` | `List<String>`, `Map<String, User>` |
| `e.printStackTrace()` | `Log.e(TAG, "message", e)` |
| `null` check with `==` on strings | `TextUtils.isEmpty()` |
| Static Context reference | Pass Context as method param |
| Unchecked casts `(Type) object` | Use `instanceof` check first |
| Anonymous Runnable on UI thread | Named inner class or lambda |
| `String +` in loops | `StringBuilder.append()` |
