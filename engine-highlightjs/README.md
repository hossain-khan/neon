# Engine Highlight.js

`engine-highlightjs` is a Neon engine backed by Highlight.js.

## What lives here

- `HljsEngineProvider`
- `HljsConfig`
- built-in Highlight.js themes
- platform-specific JS runtime bridges

## Depends on

- `core`
- Compose resources/runtime
- Android WebView support on Android

## Supported targets

- Android
- iOS
- Desktop
- JS
- WasmJs

## When to depend on this module

Depend on `engine-highlightjs` when you want a local JS-backed highlighting engine instead of a network service.
