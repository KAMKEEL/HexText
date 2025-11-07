# Angelica ↔ HexText Compatibility Plan

This document outlines the concrete refactor steps Angelica should follow to consume HexText exclusively through its public API using reflection. The goal is to eliminate every dependency on HexText internals while keeping Angelica's HexText integration lightweight and resilient across updates.

## 1. Centralise reflective API bootstrapping

1. Introduce a `HexTextReflection` helper in `com.gtnewhorizons.angelica.compat.hextext` that resolves the HexText entry point on demand:
   - Load `kamkeel.hextext.api.HexTextApi` via `Class.forName`.
   - Read `apiVersion()` to ensure it is at least `1.1.0`.
   - Cache `MethodHandle`s (or plain `Method` objects) for the service accessors Angelica needs: `textRenderer()`, `tokenHighlighter()`, `textFormatter()`, `renderEnvironment()`, `dynamicEffects()`, and `colors()`.
   - Each lookup should be guarded with logging that identifies when the API is absent or outdated so the compat layer can short-circuit gracefully.
2. Replace the current direct calls in `HexTextServices` with wrapper methods that delegate to `HexTextReflection` and expose simple Java interfaces (`Object` return types cast through reflection).
3. Ensure every accessor handles reflection failures by returning `null` and logging once, preventing repeated stack traces during the render loop.

## 2. Rewire rendering-environment checks

1. Modify `HexTextServices` to provide `RenderingEnvironmentService` instances through the reflection bridge.
2. Update `HexTextCompat` (`com.gtnewhorizons.angelica.compat.hextext.HexTextCompat`) to:
   - Query `RenderingEnvironmentService.isRawTextRendering()` instead of `FontRenderContext.isRawTextRendering()`.
   - Gate Angelica's raw-mode stack manipulations behind the new `pushRawTextRendering()`/`popRawTextRendering()` API calls.
3. Refactor `HexTextTokenHighlighter` and `HexTextColorResolver` to ask `HexTextServices.renderEnvironment()` for the raw-mode flag rather than importing `FontRenderContext` directly.

## 3. Delegate colour utilities to the API

1. Extend `HexTextServices` with a `ColorService` accessor retrieved via reflection.
2. Change `HexTextCompat.computeShadowColor(int)` to call `ColorService.calculateShadowColor()` so Angelica no longer touches `ColorCodeUtils`.
3. When HexText is unavailable, preserve the current fallback shadow computation to keep Angelica functional in standalone mode.

## 4. Forward dynamic text effects through the service facade

1. Add a reflective accessor for `DynamicEffectService` inside `HexTextServices`.
2. Rewrite `HexTextDynamicEffectsHelper` (`com.gtnewhorizons.angelica.compat.hextext.effects`) to:
   - Cache the service instance on construction and mark itself inactive if it cannot be retrieved.
   - Delegate `computeRainbowColor`, `computeIgniteColor`, and `computeShakeOffset` directly to the service methods without consulting `HexTextConfig` or `TextEffectMath`.
3. Update `HexTextCompat.EffectsHelperHolder` so it suppresses Angelica's dynamic effect rendering when the service is missing, mirroring the existing no-op fallback behaviour.

## 5. Normalise render directive handling

1. Validate that `HexTextRenderBridge` only relies on the `RenderPlan` and `RenderDirective` interfaces exposed by the API; adjust the adapter to depend solely on the public getters (already present) and avoid casting to internal implementations.
2. If additional directive metadata is required in the future, surface those fields through new HexText API accessors rather than peeking into implementation classes.

## 6. Harden failure handling and logging

1. Ensure every reflective call funnels through a shared `CompatLogger` that deduplicates warnings per execution path. The logger should clarify whether Angelica disabled a feature because HexText is missing, outdated, or returned `null`.
2. During Angelica's startup diagnostics (`ModStatus`), log the detected HexText API version and whether each compat subsystem (rendering, highlighting, effects) activated successfully.

## 7. Testing checklist

1. With HexText present (API ≥ 1.1.0), exercise Angelica's font batching to confirm:
   - Raw edit mode honours HexText's highlighting and formatting.
   - Dynamic effects (rainbow/ignite/shake) match HexText's output.
   - Drop shadows use `ColorService` and remain visually identical.
2. With HexText absent or API < 1.1.0, verify that Angelica falls back to its vanilla rendering paths without spammy logs.
3. Toggle Angelica's font renderer module off to confirm the reflection guards keep HexText unaffected.

Following this plan will let Angelica depend exclusively on HexText's stable API contracts while keeping the integration constrained to a handful of reflective entry points.
