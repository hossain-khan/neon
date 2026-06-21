# Engine Runtime

`engine-runtime` is the integration layer between engine providers and Compose.

## What lives here

- `HighlightEngineRegistry`
- engine selection and theme lookup helpers
- `rememberEngine(...)` APIs
- managed engine lifecycle handling for Compose

## Depends on

- `core`
- Compose runtime

## Used by

- `shared`

## When to depend on this module

Depend on `engine-runtime` when your app needs to register providers, select engines dynamically, or create engines from Compose state.
