---
name: edge-to-edge-ui-fixer
description: Fixes Android, Flutter, and React Native edge-to-edge rendering issues — white top/bottom gaps, content under status bar, overlapping bottom buttons, keyboard not pushing inputs, SafeArea misuse, WindowInsets misconfiguration. Use when the user reports any layout gap, immersive mode bug, or system-bar overlap. Produces the correct BackgroundLayer + ContentLayer pattern, Compose `windowInsetsPadding` usage, Flutter SafeArea+Stack pattern, RN SafeAreaProvider setup, and verification checklists.
icon: maximize
color: Orange
---

# Edge-to-Edge UI Fixer

The Layering Rule fixes 90% of edge-to-edge bugs. Always split screens into a Background Layer (no insets) and a Content Layer (`safeDrawing` insets).

## When to Use

- White gap at top or bottom of screen
- Content drawing under status / navigation bar
- Bottom button hidden behind nav bar
- Status bar height hardcoded
- Keyboard covers text input
- Flutter `SafeArea` swallowing the background
- RN content not extending to edges

## The Layering Rule

```
┌──────────────────────────────┐
│  BACKGROUND LAYER             │  ← fills entire window, ignores insets
│  ┌────────────────────────┐   │
│  │  CONTENT LAYER          │  │  ← respects safeDrawing insets
│  └────────────────────────┘   │
└──────────────────────────────┘
```

## Android Compose Pattern

In `MainActivity.onCreate`:

```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
setContent { MawaaiApp() }
```

For every screen:

```kotlin
@Composable
fun ScreenScaffold(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        BackgroundLayer(Modifier.fillMaxSize())   // no insets
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

System bar transparency:

```kotlin
window.statusBarColor = Color.TRANSPARENT
window.navigationBarColor = Color.TRANSPARENT
WindowCompat.getInsetsController(window, window.decorView).apply {
    isAppearanceLightStatusBars = !isDarkTheme
    isAppearanceLightNavigationBars = !isDarkTheme
}
```

## Bottom Sheets / Keyboards / Lists

- Bottom sheets consume `WindowInsets.navigationBars` for handle area
- Inputs use `Modifier.imePadding()` on the **input container only** — never on the whole screen
- Lists scroll behind status bar; add `Modifier.statusBarsPadding()` only to sticky headers

## Flutter Pattern

```dart
// main.dart
SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

// Screen
return Stack(
  children: [
    Positioned.fill(child: BackgroundLayer()),
    SafeArea(child: ContentLayer()),
  ],
);
```

Never wrap the entire app in `SafeArea` and then try to extend the background.

## React Native Pattern

```tsx
// App root
<SafeAreaProvider>
  <NavigationContainer>...</NavigationContainer>
</SafeAreaProvider>

// Screen
<View style={{ flex: 1 }}>
  <BackgroundLayer style={StyleSheet.absoluteFill} />
  <SafeAreaView style={{ flex: 1 }}>
    <Content />
  </SafeAreaView>
</View>
```

Set `statusBarStyle="auto"` and translucent backgrounds via `expo-status-bar` or `react-native-system-navigation-bar`.

## Forbidden Patterns

- Hardcoded status bar height (`24.dp`, `padding-top: 24px`)
- Applying `safeDrawing` padding to background layers
- `fitsSystemWindows=true` in XML layouts when targeting edge-to-edge
- Drawing content directly under bars without explicit intent
- Wrapping the entire app in `SafeArea` and then trying to extend the background
- Reading insets imperatively in business logic (insets are layout concern only)

## Verification

1. Toggle gesture nav ↔ 3-button nav — layout adapts
2. Show/hide status bar via immersive mode — content reflows
3. Open keyboard on a text input — input rises with `imePadding`, background untouched
4. Rotate device — insets recompute, no clipping
5. Confirm screenshots match design at top/bottom edges (no unintended whitespace)
6. Test on a device with display cutout (notch) — content respects cutout insets

## Output

Per micro-task:
- `ScreenScaffold.kt` (or Flutter / RN equivalent) as the canonical wrapper
- Updated `MainActivity` / `main.dart` / `App.tsx`
- Removal of forbidden patterns from existing screens (listed explicitly)
- Verification checklist for QA
