# KMP Syntax Highlight Library - Project Plan

## 1. Overview

A Kotlin Multiplatform library for syntax highlighting in Compose Multiplatform (and native UI) apps,
with a **plugin-based engine architecture**. Users choose which highlighting engine(s) to include
per platform, keeping APK/IPA size minimal and avoiding forced dependencies.

**Project ID:** `dev.hossain.neon`

**Maven coordinates:**

```
dev.hossain:neon-core
dev.hossain:neon-ui
dev.hossain:neon-engine-highlightjs
dev.hossain:neon-engine-textmate
dev.hossain:neon-engine-shiki-network
```

## 2. Goals

- Plugin-based: users depend on `core` + `ui` + only the engine(s) they want
- UI-agnostic `core`: fully decoupled from Compose to natively support SwiftUI/UIKit
- Follow JetBrains' [new KMP default project structure](https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/)
- AGP 9.0 compliant: app entry points in separate modules from KMP shared code
- `Result<T>` everywhere, no exceptions from public API
- `kotlin.AutoCloseable` engines for deterministic native resource cleanup
- `suspend` on all engine methods (network, WebView, CPU-bound all benefit)
- Backward-compatible migration path from the existing `dev.hossain:compose-highlight` Android library
- Monorepo approach for easier cross-module publishing and versioning

## 3. Target Platforms

| Platform | Status |
|----------|--------|
| Android | Primary (migrate existing library) |
| iOS | Secondary (new) |
| Desktop (JVM) | Tertiary (new) |
| Wasm/JS | Future (stretch goal) |

## 4. Module Structure

```
neon/
├── core/                            # KMP library - interfaces, models, token representations (No UI)
│   ├── commonMain/
│   ├── androidMain/
│   ├── iosMain/
│   └── desktopMain/
│
├── ui/                              # KMP library - Compose Multiplatform composables & Mappers
│   ├── commonMain/
│   ├── androidMain/
│   ├── iosMain/
│   └── desktopMain/
│
├── engine-highlightjs/              # KMP library - Highlight.js engine
│   ├── commonMain/
│   ├── androidMain/                 #   WebView JsRuntime
│   ├── iosMain/                     #   JSContext JsRuntime
│   └── desktopMain/                 #   GraalJS / Zipline JsRuntime
│
├── engine-textmate/                 # KMP library - TextMate engine
│   ├── commonMain/
│   ├── androidMain/
│   ├── iosMain/
│   └── desktopMain/
│
├── engine-shiki-network/            # KMP library - Shiki engine (server-driven)
│   ├── commonMain/                  #   Almost entirely common (HTTP client)
│   └── ...
│
├── androidApp/                      # Android sample app (AGP 9.0 - application plugin here only)
├── iosApp/                          # iOS sample app (Xcode project)
├── desktopApp/                      # Desktop sample app
├── buildSrc/                        # Build-time code generation (CSS theme precompilation, etc.)
├── gradle/
│   └── libs.versions.toml           # Shared version catalog
├── build.gradle.kts                 # Root build file
├── settings.gradle.kts
└── gradle.properties
```

### Module Dependency Graph

```
androidApp / iosApp / desktopApp
  ├── ui
  │     └── core
  ├── engine-highlightjs
  │     └── core
  ├── engine-textmate
  │     └── core
  └── engine-shiki-network
        └── core
```

Each engine module depends only on `core`. The `ui` module depends only on `core`.
Engine modules never depend on each other or on `ui`.

## 5. Core Module

The `core` module is pure Kotlin with **zero UI dependencies (no Compose)**. It defines the
contracts that engines implement and the platform-neutral tokens that flow through the system.

**Package:** `dev.hossain.neon.core`

### 5.1 HighlightEngine Interface

```kotlin
package dev.hossain.neon.core

public interface HighlightEngine : kotlin.AutoCloseable {
    public val name: String
    public val supportedLanguages: Set<String>

    public suspend fun highlight(
        code: String,
        language: String,
        theme: HighlightTheme,
    ): Result<HighlightResult>

    public suspend fun highlightBoth(
        code: String,
        language: String,
        lightTheme: HighlightTheme,
        darkTheme: HighlightTheme,
    ): Result<ThemedHighlightResult>

    public suspend fun autoDetectLanguage(code: String): Result<String>

    public suspend fun listLanguages(): List<HighlightLanguageInfo>
}
```

### 5.2 HighlightTheme Interface

```kotlin
package dev.hossain.neon.core

public interface HighlightTheme {
    public val name: String
    public val isDark: Boolean
}
```

Each engine provides its own concrete theme type implementing this interface, with
engine-specific factory methods (CSS file, JSON grammar, server theme name, etc.).

### 5.3 Result Models (UI Agnostic)

Instead of returning Compose `AnnotatedString`, `core` returns a platform-neutral token list, enabling developers to map it to Compose, SwiftUI, or HTML natively.

```kotlin
package dev.hossain.neon.core

/**
 * A single styled token in the highlighted output.
 * Tokens are flat (not hierarchical) - each token carries its fully-resolved style.
 * Newlines are represented as separate tokens with text = "\n".
 */
public data class HighlightToken(
    val text: String,
    val color: String? = null,          // hex "#FF0000" or null for default
    val background: String? = null,     // hex "#000000" or null for default
    val fontWeight: TokenFontWeight? = null,
    val fontStyle: TokenFontStyle? = null,
    val isUnderline: Boolean = false,
)

public enum class TokenFontWeight { NORMAL, BOLD }
public enum class TokenFontStyle { NORMAL, ITALIC }

public data class HighlightResult(
    val tokens: List<HighlightToken>,
    val language: String,
    val timings: HighlightTimings,
)

public data class ThemedHighlightResult(
    val light: HighlightResult,
    val dark: HighlightResult,
)

public data class HighlightTimings(
    val engineTime: kotlin.time.Duration,
    val totalTime: kotlin.time.Duration,
)

public data class HighlightLanguageInfo(
    val id: String,
    val name: String,
    val aliases: List<String>,
)
```

**Token Model Design Decisions:**
- **Typed fields** instead of `Map<String, String>` for compile-time safety
- **Hex color strings** (not Compose `Color`) to keep `core` UI-agnostic
- **Flat token list** - no hierarchy. Engines flatten nested styles at emission time.
- **Newlines as tokens** - `HighlightToken(text = "\n")` allows UI to handle line breaks and line numbers.

### 5.4 Exception Hierarchy

```kotlin
package dev.hossain.neon.core

public sealed class HighlightException(message: String, cause: Throwable? = null)
    : Exception(message, cause) {
    public class EngineInitializationFailed(engine: String, cause: Throwable) : ...
    public class UnsupportedLanguage(language: String, engine: String) : ...
    public class ThemeLoadFailed(themeName: String, cause: Throwable) : ...
    public class NetworkError(cause: Throwable) : ...
    public class JavaScriptEvaluationFailed(cause: Throwable) : ...
}
```

### 5.5 Engine Factory Interface

```kotlin
package dev.hossain.neon.core

public interface HighlightEngineFactory {
    public val name: String
    public fun isAvailable(): Boolean
    public suspend fun create(config: EngineConfig): HighlightEngine
}

public interface EngineConfig
```

### 5.6 Core Dependencies

- `kotlinx-coroutines-core`
- `kotlinx-serialization-json` (for JSON parsing, replacing `org.json`)
- *(Strict rule: No `org.jetbrains.compose.*` dependencies in core)*
- `kotlin.time.Duration` is in the Kotlin stdlib (1.6+), no external dependency needed

## 6. UI Module

Compose Multiplatform composables that map `HighlightToken` lists into `AnnotatedString` to render highlighted code. Depends on `core` and Compose Multiplatform.

**Package:** `dev.hossain.neon.ui`

### 6.1 Token-to-AnnotatedString Mapper

The core responsibility of the `ui` module. Located in `ui/commonMain/`:

```kotlin
package dev.hossain.neon.ui

/**
 * Converts a list of HighlightTokens into a Compose AnnotatedString.
 * Handles newline tokens by preserving them in the output.
 */
public fun tokensToAnnotatedString(
    tokens: List<HighlightToken>,
    defaultColor: Color = Color.Unspecified,
    defaultBackground: Color = Color.Unspecified,
): AnnotatedString = buildAnnotatedString {
    for (token in tokens) {
        val style = SpanStyle(
            color = token.color?.let { parseHexColor(it) } ?: defaultColor,
            background = token.background?.let { parseHexColor(it) } ?: defaultBackground,
            fontWeight = when (token.fontWeight) {
                TokenFontWeight.BOLD -> FontWeight.Bold
                else -> null
            },
            fontStyle = when (token.fontStyle) {
                TokenFontStyle.ITALIC -> FontStyle.Italic
                else -> null
            },
            textDecoration = if (token.isUnderline) TextDecoration.Underline else null,
        )
        withStyle(style) { append(token.text) }
    }
}

internal fun parseHexColor(hex: String): Color {
    // Parse "#RRGGBB" or "#AARRGGBB" to Compose Color
}
```

### 6.2 Public Composables

```kotlin
package dev.hossain.neon.ui

@Composable
public fun SyntaxHighlightedCode(
    code: String,
    language: String,
    theme: HighlightTheme,
    modifier: Modifier = Modifier,
    showLineNumbers: Boolean = false,
    codeBlockStyle: CodeBlockStyle = SyntaxHighlightedCodeDefaults.codeBlockStyle(),
)

@Composable
public fun SyntaxHighlightedTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: String,
    theme: HighlightTheme,
    modifier: Modifier = Modifier,
)
```

### 6.3 Engine Injection

```kotlin
package dev.hossain.neon.ui

public val LocalHighlightEngine = staticCompositionLocalOf<HighlightEngine> {
    error("No HighlightEngine provided. Wrap with HighlightEngineProvider {}")
}

@Composable
public fun HighlightEngineProvider(
    engine: HighlightEngine,
    content: @Composable () -> Unit,
)

@Composable
public fun rememberHighlightEngine(
    factory: HighlightEngineFactory,
    config: EngineConfig,
): HighlightEngine
```

### 6.4 Platform-Specific UI Concerns

| Concern | Android | iOS | Desktop |
|---------|---------|-----|---------|
| Clipboard (copy button) | `LocalClipboard` | expect/actual | expect/actual |
| Resource icons | `compose.resources` | `compose.resources` | `compose.resources` |
| `@Preview` composables | `androidMain` only | N/A | N/A |

### 6.5 UI Module Dependencies

- `core` (api dependency)
- `org.jetbrains.compose.runtime`
- `org.jetbrains.compose.foundation`
- `org.jetbrains.compose.material3`
- `org.jetbrains.compose.ui`
- `org.jetbrains.compose.components:components-resources`

## 7. Engine Modules

### 7.1 engine-highlightjs

Runs [Highlight.js](https://highlightjs.org/) in a JavaScript runtime. Supports 190+ languages.

**Package:** `dev.hossain.neon.engine.highlightjs`

**Architecture:**

```
commonMain:
  HljsEngine (implements HighlightEngine)
  HljsTheme (implements HighlightTheme)
  HljsConfig (implements EngineConfig)
  internal expect class JsRuntime(config) {
      suspend fun evaluate(script: String): String
      fun close()
  }

androidMain:
  internal actual class JsRuntime - WebView

iosMain:
  internal actual class JsRuntime - JavaScriptCore (JSContext)

desktopMain:
  internal actual class JsRuntime - GraalJS or Zipline (CashApp's QuickJS wrapper)
```

**Resource Loading:**
Bundled assets (`highlight.min.js`, `bridge.html`, and CSS themes) are placed in the `composeResources/` directory of the engine module. They are loaded across all platforms using KMP's built-in `Res.readBytes()`.

**Note:** This means `engine-highlightjs` depends on `org.jetbrains.compose.components:components-resources`, which transitively brings in Compose. This is an accepted trade-off for simpler cross-platform asset loading. Users who need a Compose-free engine can implement their own asset loading via expect/actual.

**Config:**

```kotlin
package dev.hossain.neon.engine.highlightjs

public data class HljsConfig(
    val jsEngine: JsEngineType = JsEngineType.AUTO,
) : EngineConfig

public enum class JsEngineType { AUTO, WEBVIEW, ZIPLINE, JSCONTEXT, GRAALJS }
```

**Theme:**

`HljsTheme` stores a color map as hex strings (not Compose `SpanStyle`) to keep the engine Compose-free.
The `HljsEngine` uses this map when converting highlight.js HTML output into `HighlightToken` lists.

```kotlin
package dev.hossain.neon.engine.highlightjs

public class HljsTheme private constructor(
    override val name: String,
    override val isDark: Boolean,
    internal val colorMap: Map<String, TokenStyleData>,
) : HighlightTheme {
    public companion object {
        public fun fromCss(css: String, name: String, isDark: Boolean = false): HljsTheme
        public fun builtin(name: BuiltinHljsTheme): HljsTheme
    }
}

/**
 * Platform-neutral style data extracted from CSS.
 * Used by HljsEngine to populate HighlightToken fields.
 */
internal data class TokenStyleData(
    val color: String? = null,
    val background: String? = null,
    val fontWeight: TokenFontWeight? = null,
    val fontStyle: TokenFontStyle? = null,
    val isUnderline: Boolean = false,
)
```

**Factory:**

```kotlin
package dev.hossain.neon.engine.highlightjs

public class HljsEngineFactory : HighlightEngineFactory {
    override val name: String = "highlightjs"
    override fun isAvailable(): Boolean = true
    override suspend fun create(config: EngineConfig): HighlightEngine {
        require(config is HljsConfig) { "Expected HljsConfig, got ${config::class}" }
        return HljsEngine(config)
    }
}
```

**Dependencies:**

- `core` (api)
- `androidx.webkit:webkit` (androidMain only)
- CashApp's Zipline or GraalJS (desktopMain only)
- `org.jetbrains.compose.components:components-resources` (commonMain, for asset loading)

### 7.2 engine-textmate

Uses [kotlin-textmate](https://github.com/ivan-magda/kotlin-textmate) for TextMate grammar-based
tokenization. Fully offline. Supports any VS Code-compatible `.tmLanguage.json` grammar.

**Package:** `dev.hossain.neon.engine.textmate`

**Architecture:**

```
commonMain:
  TextMateEngine (implements HighlightEngine)
  TextMateTheme (implements HighlightTheme)
  TextMateConfig (implements EngineConfig)
  GrammarProvider interface

androidMain / iosMain / desktopMain:
  Platform-specific TextMate initialization
```

**Config:**

```kotlin
package dev.hossain.neon.engine.textmate

public data class TextMateConfig(
    val grammarProvider: GrammarProvider,
    val themeProvider: TextMateThemeProvider,
) : EngineConfig

public interface GrammarProvider {
    public suspend fun loadGrammar(language: String): String
    public suspend fun availableLanguages(): List<String>
}
```

**Dependencies:**

- `core` (api)
- `kotlin-textmate` (must verify KMP target support)

### 7.3 engine-shiki-network

Server-driven highlighting via a hosted
[Shiki Token Service](https://github.com/hossain-khan/shiki-token-service).
The server tokenizes code and returns per-token colors. Works on all platforms since it is
just HTTP.

**Package:** `dev.hossain.neon.engine.shiki`

**Architecture:**

```
commonMain (almost entirely common):
  ShikiNetworkEngine (implements HighlightEngine)
  ShikiTheme (implements HighlightTheme)
  ShikiNetworkConfig (implements EngineConfig)
  internal ShikiApiClient (Ktor client)
```

**Config:**

```kotlin
package dev.hossain.neon.engine.shiki

public data class ShikiNetworkConfig(
    val serviceUrl: String,
    val timeout: kotlin.time.Duration = 5.seconds,
    val httpClient: HttpClient = HttpClient(),
) : EngineConfig
```

**Theme:**

```kotlin
public class ShikiTheme private constructor(
    override val name: String,
    override val isDark: Boolean,
) : HighlightTheme {
    public companion object {
        public fun builtin(name: String): ShikiTheme
        // e.g., "github-dark", "dracula", "one-dark-pro"
    }
}
```

**Factory:**

```kotlin
package dev.hossain.neon.engine.shiki

public class ShikiNetworkEngineFactory : HighlightEngineFactory {
    override val name: String = "shiki-network"
    override fun isAvailable(): Boolean = true
    override suspend fun create(config: EngineConfig): HighlightEngine {
        require(config is ShikiNetworkConfig) { "Expected ShikiNetworkConfig, got ${config::class}" }
        return ShikiNetworkEngine(config)
    }
}
```

**Dependencies:**

- `core` (api)
- `ktor-client-core` + `ktor-client-content-negotiation`
- `ktor-client-okhttp` (androidMain) / `ktor-client-darwin` (iosMain) / `ktor-client-cio` (desktopMain)

## 8. Build Configuration

### 8.1 settings.gradle.kts

```kotlin
rootProject.name = "neon"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":core")
include(":ui")
include(":engine-highlightjs")
include(":engine-textmate")
include(":engine-shiki-network")
include(":androidApp")
include(":desktopApp")
```

### 8.2 Root build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.kotlinter) apply false
}
```

### 8.3 core/build.gradle.kts

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kotlinter)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "NeonCore"
            isStatic = true
        }
    }
    jvm("desktop")

    androidLibrary {
        namespace = "dev.hossain.neon.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.truth)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}
```

### 8.4 ui/build.gradle.kts

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kotlinter)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "NeonUi"
            isStatic = true
        }
    }
    jvm("desktop")

    androidLibrary {
        namespace = "dev.hossain.neon.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.uiToolingPreview)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiTooling)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.truth)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}
```

### 8.5 engine-highlightjs/build.gradle.kts

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kotlinter)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "NeonEngineHighlightjs"
            isStatic = true
        }
    }
    jvm("desktop")

    androidLibrary {
        namespace = "dev.hossain.neon.engine.highlightjs"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.androidx.webkit)
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.truth)
        }
        val desktopMain by getting {
            dependencies {
                // TODO: Add Zipline or GraalJS dependency
                // implementation(libs.zipline)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}
```

### 8.6 engine-shiki-network/build.gradle.kts

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kotlinter)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "NeonEngineShikiNetwork"
            isStatic = true
        }
    }
    jvm("desktop")

    androidLibrary {
        namespace = "dev.hossain.neon.engine.shiki"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.truth)
        }
    }
}
```

### 8.7 androidApp/build.gradle.kts

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(projects.core)
    implementation(projects.ui)
    implementation(projects.engineHighlightjs)
    implementation(projects.engineShikiNetwork)

    implementation(libs.androidx.activity.compose)
    implementation(compose.uiToolingPreview)
    debugImplementation(compose.uiTooling)
}

android {
    namespace = "dev.hossain.neon.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.hossain.neon.sample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

### 8.8 desktopApp/build.gradle.kts

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.core)
    implementation(projects.ui)
    implementation(projects.engineHighlightjs)
    implementation(projects.engineShikiNetwork)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "dev.hossain.neon.sample.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "neon-sample"
            packageVersion = "1.0.0"
        }
    }
}
```

### 8.9 gradle/libs.versions.toml

```toml
[versions]
agp = "9.0.1"
android-compileSdk = "36"
android-minSdk = "24"
android-targetSdk = "36"
androidx-activity = "1.13.0"
androidx-webkit = "1.16.0"
composeMultiplatform = "1.11.1"
dokka = "2.2.0"
junit = "4.13.2"
kotlin = "2.4.0"
kotlinter = "5.5.0"
kotlinx-coroutines = "1.11.0"
kotlinx-serialization = "1.8.1"
ktor = "3.1.3"
material3 = "1.11.0-alpha07"
mavenPublish = "0.36.0"
truth = "1.4.4"

[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
junit = { module = "junit:junit", version.ref = "junit" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }
androidx-webkit = { module = "androidx.webkit:webkit", version.ref = "androidx-webkit" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
compose-uiTooling = { module = "org.jetbrains.compose.ui:ui-tooling", version.ref = "composeMultiplatform" }
compose-uiToolingPreview = { module = "org.jetbrains.compose.ui:ui-tooling-preview", version.ref = "composeMultiplatform" }

[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidMultiplatformLibrary = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
dokka = { id = "org.jetbrains.dokka", version.ref = "dokka" }
kotlinJvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlinter = { id = "org.jmailen.kotlinter", version.ref = "kotlinter" }
mavenPublish = { id = "com.vanniktech.maven.publish", version.ref = "mavenPublish" }
```

### 8.10 gradle.properties

```properties
kotlin.code.style=official
kotlin.daemon.jvmargs=-Xmx3072M
org.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8
org.gradle.configuration-cache=true
org.gradle.caching=true
android.nonTransitiveRClass=true
android.useAndroidX=true
VERSION_NAME=0.1.0-SNAPSHOT
```

## 9. Testing Strategy

### 9.1 Test Locations

| Test Type | Location | Runs On |
|-----------|----------|---------|
| Pure logic unit tests | `commonTest` | All platforms (JVM host) |
| Android host tests | `androidHostTest` | JVM with Android framework stubs |
| Android instrumented tests | `androidInstrumentedTest` | Device/emulator |
| iOS tests | `iosTest` | iOS simulator |
| Desktop tests | `desktopTest` | JVM |

### 9.2 What Goes Where

**`core/commonTest`:**
- Engine interface contract tests
- `HighlightToken` and `HighlightResult` data class tests
- `HighlightException` hierarchy tests
- JSON serialization tests (for Shiki API responses)

**`engine-highlightjs/commonTest`:**
- CSS theme parsing tests (ported from `ThemeParserTest.kt`)
- JS string escape/unescape tests (ported from `HighlightEngineEscapeTest.kt`, `HighlightEngineUnescapeTest.kt`)
- HTML-to-token-list conversion tests (new, replacing `HtmlToAnnotatedStringTest.kt`)
- `HljsSelectors` constant validation

**`engine-highlightjs/androidHostTest`:**
- WebView lifecycle tests (Robolectric)
- `WebViewManager` threading tests

**`engine-highlightjs/androidInstrumentedTest`:**
- Full engine integration tests (real WebView)
- Device benchmarks

**`engine-shiki-network/commonTest`:**
- API client tests with Ktor `MockEngine`
- Token deserialization tests
- Timeout and error handling tests

**`ui/commonTest`:**
- `tokensToAnnotatedString()` mapper tests
- Hex color parsing tests

**`ui/androidHostTest`:**
- Composable rendering tests (Robolectric)
- Screenshot tests (Roborazzi)

### 9.3 Test Dependencies

Add to `libs.versions.toml`:

```toml
[versions]
robolectric = "4.14.1"
roborazzi = "1.64.0"

[libraries]
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
```

## 10. Publishing Strategy

### 10.1 Artifacts

Each publishable module produces platform-specific artifacts:

| Module | Android (AAR) | iOS (XCFramework) | Desktop (JAR) |
|--------|---------------|-------------------|---------------|
| `core` | Yes | Yes | Yes |
| `ui` | Yes | Yes | Yes |
| `engine-highlightjs` | Yes | Yes | Yes |
| `engine-textmate` | Yes | Yes | Yes |
| `engine-shiki-network` | Yes | Yes | Yes |

### 10.2 Maven Central Coordinates

```
dev.hossain:neon-core:<version>
dev.hossain:neon-ui:<version>
dev.hossain:neon-engine-highlightjs:<version>
dev.hossain:neon-engine-textmate:<version>
dev.hossain:neon-engine-shiki-network:<version>
```

### 10.3 Versioning

All modules share a single version, managed via the `prepare-release.sh` script (adapted from
the existing compose-highlight script). Git tags without `v` prefix (e.g., `0.1.0`).

### 10.4 Publishing Workflow

1. Run `./scripts/prepare-release.sh <version>` to update all version references
2. Create release branch, run checks, commit, push, open PR
3. After merge, tag and push tag
4. Manually trigger publish GitHub Actions workflow (dry-run first, then actual publish)

## 11. Migration Path from compose-highlight

### 11.1 API Changes (Breaking)

The KMP library introduces breaking changes from `compose-highlight`:

| Old API (`compose-highlight`) | New API (`neon`) |
|-------------------------------|------------------|
| `HighlightEngine(context)` | `HljsEngineFactory().create(HljsConfig())` |
| `engine.highlight(code, lang, theme)` returns `Result<HighlightResult>` with `AnnotatedString` | `engine.highlight(code, lang, theme)` returns `Result<HighlightResult>` with `List<HighlightToken>` |
| `HighlightTheme.fromAsset(context, path)` | `HljsTheme.fromCss(cssString, name)` or `HljsTheme.builtin(BuiltinTheme.TOMORROW)` |
| `HighlightThemeProvider { }` | `HighlightEngineProvider(engine = engine) { }` |
| `LocalHighlightTheme.current` | Explicit `theme` parameter on composables |

### 11.2 Migration Guide

```kotlin
// OLD (compose-highlight)
HighlightThemeProvider(
    lightHighlightTheme = rememberTomorrowTheme(),
    darkHighlightTheme = rememberTomorrowNightTheme(),
) {
    SyntaxHighlightedCode(
        code = code,
        language = "kotlin",
    )
}

// NEW (neon)
val engine = rememberHighlightEngine(HljsEngineFactory(), HljsConfig())
HighlightEngineProvider(engine = engine) {
    SyntaxHighlightedCode(
        code = code,
        language = "kotlin",
        theme = HljsTheme.builtin(BuiltinHljsTheme.TOMORROW),
    )
}
```

### 11.3 Parallel Maintenance

- `compose-highlight` continues receiving bug fixes during migration
- Deprecation announced after `neon` 1.0 stable release
- `compose-highlight` enters maintenance-only mode after 6 months

## 12. Source File Mapping

Files to port from `compose-highlight` to `neon`:

### 12.1 Pure Kotlin -> `core/commonMain`

| Source (compose-highlight) | Destination (neon) | Changes |
|---------------------------|-------------------|---------|
| `engine/HighlightException.kt` | `core/commonMain/.../core/HighlightException.kt` | Rename variants, update package |
| `engine/HighlightTimings.kt` | `core/commonMain/.../core/HighlightTimings.kt` | Use `kotlin.time.Duration` |
| `engine/HljsSelectors.kt` | `core/commonMain/.../core/HljsSelectors.kt` | Update package |
| `engine/HighlightLanguage.kt` | `core/commonMain/.../core/HighlightLanguage.kt` | Replace `java.util.Locale` with `kotlin.text` |
| `engine/internal/JsStringEscape.kt` | `core/commonMain/.../core/internal/JsStringEscape.kt` | Replace `Character.toCodePoint()` and `StringBuilder.appendCodePoint()` with pure Kotlin |
| `engine/internal/EngineErrorHandling.kt` | `core/commonMain/.../core/internal/ErrorHandling.kt` | Update package |
| `ui/ExperimentalHighlightApi.kt` | `core/commonMain/.../core/ExperimentalNeonApi.kt` | Rename annotation |

### 12.2 Compose-only -> `ui/commonMain`

| Source (compose-highlight) | Destination (neon) | Changes |
|---------------------------|-------------------|---------|
| `engine/internal/HtmlToAnnotatedString.kt` | `ui/commonMain/.../ui/internal/TokensToAnnotatedString.kt` | New mapper from `List<HighlightToken>` to `AnnotatedString` |
| `ui/SyntaxHighlightedCode.kt` | `ui/commonMain/.../ui/SyntaxHighlightedCode.kt` | Remove `android.content.ClipData`, use expect/actual clipboard |
| `ui/SyntaxHighlightedCodeDefaults.kt` | `ui/commonMain/.../ui/SyntaxHighlightedCodeDefaults.kt` | Replace `R.drawable` with `compose.resources` |
| `ui/SyntaxHighlightedTextEditor.kt` | `ui/commonMain/.../ui/SyntaxHighlightedTextEditor.kt` | Update package |
| `ui/SyntaxHighlightedTextEditorDefaults.kt` | `ui/commonMain/.../ui/SyntaxHighlightedTextEditorDefaults.kt` | Update package |
| `ui/RememberHighlightedCode.kt` | `ui/commonMain/.../ui/RememberHighlightedCode.kt` | Adapt to token-based result |
| `ui/RememberSyntaxHighlightedEditorValue.kt` | `ui/commonMain/.../ui/RememberSyntaxHighlightedEditorValue.kt` | Update package |
| `ui/HighlightThemeProvider.kt` | `ui/commonMain/.../ui/HighlightEngineProvider.kt` | Rename, remove theme locals, engine-based |
| `ui/RememberHighlightEngine.kt` | `ui/commonMain/.../ui/RememberHighlightEngine.kt` | Use factory pattern |
| `ui/CodeBlockStyle.kt` | `ui/commonMain/.../ui/CodeBlockStyle.kt` | Update package |
| `ui/internal/LocalHighlightEngine.kt` | `ui/commonMain/.../ui/internal/LocalHighlightEngine.kt` | Update package |
| `ui/internal/ApplySnapshotSpans.kt` | `ui/commonMain/.../ui/internal/ApplySnapshotSpans.kt` | Update package |

### 12.3 Compose-only -> `engine-highlightjs/commonMain`

| Source (compose-highlight) | Destination (neon) | Changes |
|---------------------------|-------------------|---------|
| `engine/internal/HtmlParser.kt` | `engine-highlightjs/commonMain/.../internal/HtmlParser.kt` | Refactor to emit `List<HighlightToken>` instead of `AnnotatedString`; use `HljsTheme.colorMap` (hex strings) for styling |
| `engine/internal/ThemeParser.kt` | `engine-highlightjs/commonMain/.../internal/ThemeParser.kt` | Remove `Context` parameter, accept CSS string directly; output `Map<String, TokenStyleData>` instead of `Map<String, SpanStyle>` |

### 12.4 Android-specific -> `engine-highlightjs/androidMain`

| Source (compose-highlight) | Destination (neon) | Changes |
|---------------------------|-------------------|---------|
| `engine/HighlightEngine.kt` | `engine-highlightjs/commonMain/.../HljsEngine.kt` | Extract interface to `core`, implementation uses `JsRuntime` |
| `engine/HighlightTheme.kt` | `engine-highlightjs/commonMain/.../HljsTheme.kt` | Remove `Context`, use CSS string input |
| `engine/internal/WebViewManager.kt` | `engine-highlightjs/androidMain/.../internal/WebViewJsRuntime.kt` | Rename, implement `JsRuntime` interface |

### 12.5 New Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `HighlightEngine.kt` (interface) | `core/commonMain/.../core/` | Engine contract |
| `HighlightToken.kt` | `core/commonMain/.../core/` | Token data class |
| `HighlightResult.kt` | `core/commonMain/.../core/` | Result with tokens (not AnnotatedString) |
| `ThemedHighlightResult.kt` | `core/commonMain/.../core/` | Dual-theme result |
| `HighlightEngineFactory.kt` | `core/commonMain/.../core/` | Factory interface |
| `EngineConfig.kt` | `core/commonMain/.../core/` | Config marker interface |
| `JsRuntime.kt` (expect) | `engine-highlightjs/commonMain/.../internal/` | JS execution contract |
| `WebViewJsRuntime.kt` (actual) | `engine-highlightjs/androidMain/.../internal/` | Android WebView impl |
| `JSContextJsRuntime.kt` (actual) | `engine-highlightjs/iosMain/.../internal/` | iOS JavaScriptCore impl |
| `ZiplineJsRuntime.kt` (actual) | `engine-highlightjs/desktopMain/.../internal/` | Desktop Zipline impl |
| `HljsEngine.kt` | `engine-highlightjs/commonMain/.../` | Highlight.js engine impl |
| `HljsEngineFactory.kt` | `engine-highlightjs/commonMain/.../` | Factory implementing `HighlightEngineFactory` |
| `HljsTheme.kt` | `engine-highlightjs/commonMain/.../` | Highlight.js theme impl |
| `HljsConfig.kt` | `engine-highlightjs/commonMain/.../` | Config data class |
| `ShikiNetworkEngine.kt` | `engine-shiki-network/commonMain/.../` | Shiki engine impl |
| `ShikiNetworkEngineFactory.kt` | `engine-shiki-network/commonMain/.../` | Factory implementing `HighlightEngineFactory` |
| `ShikiTheme.kt` | `engine-shiki-network/commonMain/.../` | Shiki theme impl |
| `ShikiNetworkConfig.kt` | `engine-shiki-network/commonMain/.../` | Config data class |
| `ShikiApiClient.kt` | `engine-shiki-network/commonMain/.../internal/` | Ktor HTTP client |
| `tokensToAnnotatedString.kt` | `ui/commonMain/.../ui/` | Token-to-AnnotatedString mapper |
| `TokenStyleData.kt` | `engine-highlightjs/commonMain/.../internal/` | Internal style data (hex strings) for HljsTheme color map |

### 12.6 Test File Mapping

| Source (compose-highlight) | Destination (neon) |
|---------------------------|-------------------|
| `test/.../engine/ThemeParserTest.kt` | `engine-highlightjs/commonTest/.../ThemeParserTest.kt` |
| `test/.../engine/HighlightEngineEscapeTest.kt` | `engine-highlightjs/commonTest/.../JsStringEscapeTest.kt` |
| `test/.../engine/HighlightEngineUnescapeTest.kt` | `engine-highlightjs/commonTest/.../JsStringEscapeTest.kt` |
| `test/.../engine/HtmlToAnnotatedStringTest.kt` | `ui/commonTest/.../TokensToAnnotatedStringTest.kt` (refactored) |
| `test/.../engine/HighlightExceptionTest.kt` | `core/commonTest/.../HighlightExceptionTest.kt` |
| `test/.../engine/HljsSelectorsParserTest.kt` | `core/commonTest/.../HljsSelectorsTest.kt` |
| `test/.../engine/HighlightTimingsTest.kt` | `core/commonTest/.../HighlightTimingsTest.kt` |
| `test/.../engine/HighlightLanguageTest.kt` | `core/commonTest/.../HighlightLanguageTest.kt` |
| `test/.../engine/HighlightEngineErrorHandlingTest.kt` | `core/commonTest/.../ErrorHandlingTest.kt` |
| `test/.../ui/CodeBlockStyleTest.kt` | `ui/commonTest/.../CodeBlockStyleTest.kt` |
| `test/.../engine/WebViewManagerRobolectricTest.kt` | `engine-highlightjs/androidHostTest/.../WebViewJsRuntimeTest.kt` |
| `test/.../ui/SyntaxHighlightedCodeRobolectricTest.kt` | `ui/androidHostTest/.../SyntaxHighlightedCodeTest.kt` |
| `test/.../screenshot/*` | `ui/androidHostTest/.../screenshot/` |
| `androidTest/.../engine/HighlightEngineTest.kt` | `engine-highlightjs/androidInstrumentedTest/.../HljsEngineTest.kt` |
| `androidTest/.../benchmark/*` | `engine-highlightjs/androidInstrumentedTest/.../benchmark/` |

## 13. Verification Commands

```bash
# Format all modules
./gradlew formatKotlin

# Build all library modules
./gradlew :core:assembleRelease :ui:assembleRelease :engine-highlightjs:assembleRelease :engine-shiki-network:assembleRelease

# Run core unit tests (all platforms)
./gradlew :core:allTests

# Run core JVM tests only
./gradlew :core:desktopTest

# Run engine-highlightjs unit tests
./gradlew :engine-highlightjs:allTests

# Run engine-shiki-network unit tests
./gradlew :engine-shiki-network:allTests

# Run UI unit tests
./gradlew :ui:allTests

# Build Android sample app
./gradlew :androidApp:assembleDebug

# Build Desktop sample app
./gradlew :desktopApp:run

# Run Android instrumented tests (requires device/emulator)
./gradlew :engine-highlightjs:connectedAndroidTest

# Lint check
./gradlew lintKotlin

# Generate Dokka API docs
./gradlew dokkaGenerate

# Publish to Maven Central (dry run)
./gradlew publishAllPublicationsToMavenCentral --dry-run
```

## 14. Phased Implementation Plan

### Phase 1: Foundation (Weeks 1-3)

- [ ] Create new repo or directory structure with `core`, `ui`, `androidApp` modules
- [ ] Set up `settings.gradle.kts`, root `build.gradle.kts`, `libs.versions.toml`, `gradle.properties`
- [ ] Define `HighlightEngine`, `HighlightTheme`, `HighlightResult`, `HighlightToken` interfaces in `core/commonMain`
- [ ] Define `HighlightException` sealed class hierarchy in `core/commonMain`
- [ ] Port `JsStringEscape.kt` to `core/commonMain` (replace JVM-only APIs with pure Kotlin)
- [ ] Port `HljsSelectors` constants to `core/commonMain`
- [ ] Port `HighlightLanguage` to `core/commonMain` (replace `java.util.Locale`)
- [ ] Port `HighlightTimings` to `core/commonMain` (use `kotlin.time.Duration`)
- [ ] Port `EngineErrorHandling` helpers to `core/commonMain`
- [ ] Write `core/commonTest` tests for all ported files
- [ ] Set up `ui/commonMain` with `tokensToAnnotatedString()` mapper
- [ ] Set up `HighlightEngineProvider` and `LocalHighlightEngine` in `ui/commonMain`
- [ ] Verify: `./gradlew :core:desktopTest :ui:desktopTest` passes

### Phase 2: Highlight.js Engine - Android (Weeks 4-6)

- [ ] Create `engine-highlightjs` module with `commonMain` / `androidMain`
- [ ] Port `ThemeParser` to `engine-highlightjs/commonMain` (remove `Context` parameter)
- [ ] Create `expect class JsRuntime` in `engine-highlightjs/commonMain`
- [ ] Port `WebViewManager` to `engine-highlightjs/androidMain` as `WebViewJsRuntime` (actual)
- [ ] Implement `HljsEngine` in `commonMain` using `JsRuntime`
- [ ] Implement `HljsTheme` with `fromCss()`, `builtin()` factories
- [ ] Place `highlight.min.js` and `bridge.html` in `composeResources/`
- [ ] Port `buildSrc/GenerateThemesTask` for precompiled built-in themes
- [ ] Port `HtmlParser` to `engine-highlightjs/commonMain` (emit `HighlightToken` list)
- [ ] Wire up `androidApp` sample to use `HljsEngine` + `SyntaxHighlightedCode`
- [ ] Port JVM unit tests to `engine-highlightjs/commonTest` and `androidHostTest`
- [ ] Verify: `./gradlew :engine-highlightjs:androidHostTest :androidApp:assembleDebug`

### Phase 3: Shiki Network Engine (Weeks 7-8)

- [ ] Create `engine-shiki-network` module (almost entirely `commonMain`)
- [ ] Implement `ShikiNetworkEngine` with Ktor HTTP client
- [ ] Implement `ShikiTheme` with `builtin()` factory
- [ ] Implement `ShikiNetworkConfig` with service URL, timeout, custom HttpClient
- [ ] Add Ktor engine dependencies per platform (okhttp, darwin, cio)
- [ ] Write `commonTest` tests with Ktor `MockEngine`
- [ ] Wire up `androidApp` sample to demonstrate Shiki engine
- [ ] Verify: `./gradlew :engine-shiki-network:allTests :androidApp:assembleDebug`

### Phase 4: Desktop Support (Weeks 9-11)

- [ ] Evaluate and select JS runtime for desktop (Zipline vs GraalJS)
- [ ] Implement `JsRuntime` actual for `desktopMain`
- [ ] Bundle `highlight.min.js` and `bridge.html` as desktop resources
- [ ] Test `engine-highlightjs` on desktop JVM
- [ ] Test `engine-shiki-network` on desktop (should work with minimal changes)
- [ ] Create `desktopApp` sample with Compose for Desktop
- [ ] Verify: `./gradlew :engine-highlightjs:desktopTest :desktopApp:run`

### Phase 5: iOS Support (Weeks 12-15)

- [ ] Implement `JsRuntime` actual for `iosMain` using JavaScriptCore (`JSContext`)
- [ ] Bundle `highlight.min.js` and `bridge.html` as iOS resources
- [ ] Test `engine-highlightjs` on iOS simulator
- [ ] Test `engine-shiki-network` on iOS (should work with minimal changes)
- [ ] Create `iosApp` sample with Compose Multiplatform for iOS
- [ ] Verify: `./gradlew :engine-highlightjs:iosSimulatorArm64Test`

### Phase 6: TextMate Engine (Weeks 16-19)

- [ ] Evaluate `kotlin-textmate` KMP target support
- [ ] If supported: create `engine-textmate` module, implement `TextMateEngine`
- [ ] If not supported: evaluate alternatives (fork, different library, or defer)
- [ ] Implement `TextMateTheme` with JSON theme file loading
- [ ] Implement `GrammarProvider` interface and bundled grammar loading
- [ ] Bundle sample grammars (Kotlin, Python, JSON, JavaScript)
- [ ] Write tests for TextMate tokenization
- [ ] Verify sample apps highlight code with TextMate engine

### Phase 7: Polish and Release (Weeks 20-22)

- [ ] KDoc on all public API (Dokka requirement)
- [ ] Migration guide from `compose-highlight` to `neon`
- [ ] Dokka API docs generation and publishing
- [ ] Update docs site with `neon` library documentation
- [ ] Performance benchmarks across all engines and platforms
- [ ] CI/CD pipeline for all targets (Android, iOS, Desktop)
- [ ] First stable release (1.0.0) to Maven Central
- [ ] Announce deprecation timeline for `compose-highlight`

## 15. Resolved Architectural Decisions

1. **Decoupled `core`**: The `core` module is strictly UI-agnostic. By returning lists of `HighlightToken` instead of Compose `AnnotatedString`, users can build native SwiftUI or React Native layers over the KMP engine logic, maximizing flexibility.

2. **Typed Token Model**: `HighlightToken` uses typed fields (`color: String?`, `fontWeight: TokenFontWeight?`) instead of `Map<String, String>` for compile-time safety and clear API contracts.

3. **Monorepo Approach**: The project lives in a single repository for simplified versioning and publishing pipelines across all modules.

4. **Asset Management**: Engine modules use Compose Multiplatform's `composeResources/` for bundled assets. This is an accepted trade-off that brings Compose transitively into engine modules. Users needing Compose-free engines can implement custom asset loading.

5. **Desktop JS Runtime**: Zipline (CashApp's Kotlin wrapper around QuickJS) is the preferred choice for desktop JS execution. It handles native binding and provides Kotlin/JS interop, avoiding manual JNI complexity. GraalJS is a fallback option.

6. **Flat Token List**: Tokens are flat (not hierarchical). Engines flatten nested styles at emission time. Newlines are represented as separate tokens with `text = "\n"`.

7. **Hex Color Strings**: Colors in `HighlightToken` are hex strings (e.g., `"#FF0000"`) rather than Compose `Color` objects, keeping `core` UI-framework agnostic.

## 16. Reference Implementations

When implementing engines, refer to these existing projects for working examples:

| Project | URL | Relevance |
|---------|-----|-----------|
| **Android Syntax Highlighter (Compose)** | https://github.com/hossain-khan/android-syntax-highlighter-compose | Comparison app with working implementations of all three engines: compose-highlight (Highlight.js/WebView), kotlin-textmate (TextMate), and Shiki Token Service (network). Use as reference for engine integration patterns, theme handling, and performance measurement. |
| **compose-highlight** | https://github.com/hossain-khan/android-compose-highlight | The existing Android-only Highlight.js library being migrated. Source of truth for the WebView-based engine, CSS theme parsing, HTML-to-token conversion, and all UI composables. |
| **kotlin-textmate** | https://github.com/ivan-magda/kotlin-textmate | Kotlin port of TextMate grammar engine. Reference for grammar loading, tokenization pipeline, and theme application. Must verify KMP target support before adopting. |

## 17. Open Questions and Risks

### Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Zipline/GraalJS maturity for desktop JVM | High | Evaluate both in Phase 4; fallback to JCEF |
| `kotlin-textmate` KMP target support | High | Verify early in Phase 6; may need to fork or find alternative |
| `composeResources/` pulling Compose into engine modules | Medium | Document trade-off; offer custom asset loading escape hatch |
| iOS JavaScriptCore performance vs WebView | Medium | Benchmark in Phase 5; document trade-offs |
| Binary size impact of multiple engines | Low | Each engine is a separate artifact; users only include what they need |

### Open Questions

1. **Should `core` have Android target?** Currently yes, using `androidMultiplatformLibrary` plugin. This allows Android-specific optimizations if needed, but adds build complexity. Alternative: pure common + JVM + iOS targets only.

2. **Zipline vs GraalJS for desktop?** Zipline is Kotlin-native but newer. GraalJS is mature but heavier. Need benchmarking in Phase 4.

3. **Should `engine-shiki-network` support offline caching?** Could cache recent tokenization results for repeated code snippets. Adds complexity but improves UX.

4. **Amper support?** JetBrains' [Amper](https://amper.org/) enforces one product per module, which this plan already follows. Should be compatible when Amper matures.
