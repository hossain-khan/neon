# Repository Guidelines

## Project Structure & Module Organization
`core` defines the portable highlighting API and models. `ui` contains shared Compose UI components. `engine-highlightjs` and `engine-shiki-network` provide engine implementations. `shared` composes those modules into app-facing shared code. Platform wrappers live in `androidApp`, `desktopApp`, `webApp`, and `iosApp`. Source sets follow KMP conventions such as `src/commonMain`, `src/androidMain`, `src/iosMain`, `src/jsMain`, and matching `*Test` folders.

## Build, Test, and Development Commands
Use Gradle from the repo root:

- `./gradlew :androidApp:assembleDebug` builds the Android app.
- `./gradlew :desktopApp:run` runs the desktop app.
- `./gradlew :webApp:wasmJsBrowserDevelopmentRun` runs the web demo with the Wasm target.
- `./gradlew :engine-highlightjs:allTests :ui:allTests :engine-shiki-network:allTests` runs the main library test suites.
- `./gradlew :shared:testAndroidHostTest` runs Android host tests for shared code.

Prefer targeted module tasks over full-project builds while iterating.

## Coding Style & Naming Conventions
Write Kotlin with 4-space indentation and standard Kotlin naming: `PascalCase` for types, `camelCase` for functions and properties, `SCREAMING_SNAKE_CASE` for constants. Keep public APIs explicit and avoid hidden platform bootstrap requirements where possible. Follow existing KMP source-set naming and keep target-specific code inside the matching source set. Use concise comments only where control flow or platform behavior is not obvious.

## Testing Guidelines
Tests use Kotlin Test; some modules also use Truth. Place tests beside the relevant module in `src/commonTest`, `src/jvmTest`, `src/androidHostTest`, or other target-specific test source sets. Name test files after the class or behavior under test, for example `InlineScriptExtractorTest.kt`. Add or update tests for shared parsing, engine lifecycle, and target-specific regressions when changing public library behavior.

## Commit & Pull Request Guidelines
Recent history favors short, imperative commit subjects such as `Harden KMP library runtime behavior` or `minor fixes`. Keep commits focused and scoped to one change set. For pull requests, include:

- a short summary of what changed
- the reason for the change
- the Gradle commands used for verification
- screenshots only when UI behavior changed

## Architecture Notes
This repository is a KMP library first, with app modules used as integration surfaces and demos. Prefer improving `core`, `ui`, and engine modules directly rather than adding library logic to platform app wrappers.
