# Engine Shiki Network

`engine-shiki-network` is a Neon engine that delegates highlighting to a remote Shiki-compatible service.

## What lives here

- `ShikiNetworkEngineProvider`
- `ShikiNetworkConfig`
- built-in Shiki theme descriptors
- Ktor-based client integration

## Depends on

- `core`
- Ktor client + content negotiation + logging

## Supported targets

- Android
- iOS
- Desktop
- JS
- WasmJs

## Notes

- JS and Wasm use an explicit Ktor `Js` client path.
- Browser targets still depend on the remote service being reachable and CORS-compatible.

## When to depend on this module

Depend on `engine-shiki-network` when you want Shiki rendering without bundling a local highlighter runtime.
