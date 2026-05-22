# Edge-to-Edge UI Repair

Correct, production-ready edge-to-edge for Android Compose. Same principles apply to Flutter `SafeArea` and RN `SafeAreaView`.

## The Layering Rule

Always split a screen into two layers:

```
┌──────────────────────────────┐
│  BACKGROUND LAYER             │  ← fills entire window, ignores insets
│  ┌────────────────────────┐   │
│  │  CONTENT LAYER          │  │  ← respects safeDrawing insets
│  └────────────────────────┘   │
└──────────────────────────────┘
```

The background renders behind the status and nav bars; content stays inside the safe area.

## Android Compose Pattern

In `MainActivity.onCreate`:

```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
setContent { MawaaiApp() }
```

In screens:

```kotlin
@Composable
fun ScreenScaffold(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        // Background — no insets
        BackgroundLayer(Modifier.fillMaxSize())

        // Content — safeDrawing
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            content()
        }
    }
}
```

For system bar transparency:

```kotlin
WindowCompat.getInsetsController(window, window.decorView).apply {
    isAppearanceLightStatusBars = !isDarkTheme
    isAppearanceLightNavigationBars = !isDarkTheme
}
window.statusBarColor = Color.TRANSPARENT
window.navigationBarColor = Color.TRANSPARENT
```

## Bottom Sheets / Keyboards

- Bottom sheets consume `WindowInsets.navigationBars` for their handle area
- Inputs use `Modifier.imePadding()` on the input container — not on the whole screen
- Lists scroll behind status bar by adding `Modifier.statusBarsPadding()` only to sticky headers

## Flutter Equivalent

```dart
return Stack(
  children: [
    Positioned.fill(child: BackgroundLayer()),
    SafeArea(child: ContentLayer()),
  ],
);
// In main(): SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
```

## Forbidden Patterns

- Hardcoded status bar height (`24.dp`, `padding-top: 24px`)
- Applying `safeDrawing` padding to background layers
- `fitsSystemWindows=true` in XML layouts when targeting edge-to-edge
- Drawing content directly under bars without explicit intent
- Wrapping the entire app in `SafeArea` and then trying to extend the background

## Verification

1. Toggle gesture nav ↔ 3-button nav — layout should adapt
2. Show/hide status bar via immersive mode — content reflows
3. Open keyboard on a text input — input rises with `imePadding`, background untouched
4. Rotate device — insets recompute, no clipping
5. Confirm screenshots match design at top/bottom edges (no unintended whitespace)
