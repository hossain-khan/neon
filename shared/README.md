# Shared

`shared` is the showcase/demo module for Neon.

## What lives here

- `NeonShowcaseApp`
- demo UI state and sample snippets
- shared Compose UI used by Android, desktop, web, and iOS demo shells

## Depends on

- `core`
- `ui`
- `engine-runtime`
- `engine-highlightjs`
- `engine-shiki-network`

## Important

This module is not the recommended library entrypoint for consumers. It exists to host the demo application surface across platforms.

## Used by

- `androidApp`
- `desktopApp`
- `webApp`
- `iosApp`
