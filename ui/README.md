# UI

`ui` contains Compose UI primitives for rendering highlighted code.

## What lives here

- `SyntaxHighlightedCode`
- Compose helpers for providing an engine to UI
- code block styling and presentation utilities

## Depends on

- `core`
- Compose Multiplatform UI/foundation/material

## Used by

- `shared`

## When to depend on this module

Depend on `ui` when you already have a Neon `HighlightEngine` and want to render highlighted output in Compose.
