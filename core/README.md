# Core

`core` is the foundation module for Neon.

## What lives here

- engine-neutral models such as tokens, themes, timings, and results
- provider and engine interfaces
- shared exceptions and configuration types

## Depends on

- Kotlin coroutines
- Kotlin serialization

## Used by

- `ui`
- `engine-runtime`
- `engine-highlightjs`
- `engine-shiki-network`

## When to depend on this module

Depend on `core` when you need Neon types or want to implement a new engine/provider without bringing in UI or demo code.
