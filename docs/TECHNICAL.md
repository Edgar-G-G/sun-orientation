# Sun Orientation — technical notes

How each part of the sky is rotated, what was verified and the known limitations.
User-facing documentation is in the [README](../README.md).

## The rotation

Minecraft axes: east = +X, west = -X, south = +Z, north = -Z. The configured sunrise direction is a compass
bearing `b` (0 = north, 90 = east/vanilla, 180 = south, 270 = west).

The mod applies one rigid rotation around +Y, `R = Ry(yaw)`, with the convention of JOML `Matrix4f.rotateY`.
Since `Ry(yaw)·(1,0,0) = (cos yaw, 0, -sin yaw)` and bearing `b` points to `(sin b, 0, -cos b)`, the rotation that
takes east to `b` is **`yaw = 90 - b`** (`SkyRotation.extraYawForBearing`). South gives `yaw = -90`.

`SunOrientationConfig` keeps an immutable state (bearing, yaw, sin, cos) that the mixins read every frame, so a
change is visible immediately. With `yaw = 0` (east) nothing is modified.

## What is rotated

| Where (Minecraft 26.3 / Iris 1.11.7) | Original | Change |
| --- | --- | --- |
| `SkyRenderer.renderSunMoonAndStars` | fixed `rotateDegrees(YP, -90)` | `@ModifyArg`: `-90 + yaw` (sun, moon, stars). Iris inserts `Rz(sunPathRotation)` right after this call (`tiltSun`, ordinal 0, AFTER), which keeps working. |
| `SkyRenderer.renderSunriseAndSunset` | **no** Y rotation (`Rx(90)·Rz(angle+90)`) | `@Inject` before its first `rotateDegrees`: `Ry(yaw)`; its `popPose` undoes it. |
| `AtmosphericFogEnvironment.getBaseColor` | sunrise tint when looking at `(±1,0,0)` | `@WrapOperation` on `Vector3fc.dot(FFF)`: the direction is rotated. Nuit wraps a different call of the same method. |
| Iris `CelestialUniforms.getCelestialPosition` / `…InWorldSpace` | `Ry(-90)·Rz(sunPath)·Rx(angle)` | `@ModifyConstant` on `-90`: `-90 + yaw`. Covers `sunPosition`, `moonPosition`, `shadowLightPosition` (view and world space) and shadow culling. `getUpPosition` rotates the vertical vector and needs no change. |
| Iris `ShadowMatrices.createBaselineModelViewMatrix` | view `V = Rx(90)·Rz(-a)·Rx(sunPath)` | `Ry(-yaw)` after the last rotation of the non-End branch (ordinal 4). |
| Iris `IncludeGraph.readFile` | packs that compute the sun in GLSL | the source is patched before Iris processes it (see below). |

### Shadow matrix sign

`V` maps world space to the shadow camera. The light is rotated, `L' = R·L`, so the view needs the inverse:
`V' = V·R⁻¹ = V·Ry(-yaw)`, which gives `V'·L' = V·L`. `PoseStack.rotateDegrees` post-multiplies
(`Matrix4f.rotateY`), so appending `Ry(-yaw)` is enough; `snapModelViewToGrid` translates in world space afterwards
and stays correct. Using the sky's own sign (as the original prototype copied from ClassicSunRotation) puts the
light behind the shadow camera; `GeometryTest.usingTheSkySignForShadowsWouldBeWrong` documents it.

### Shader packs that compute the sun in GLSL

Complementary Reimagined (and BSL, which it derives from) do **not** use `sunPosition` for lighting. They compute
the sun vector in GLSL with a fixed east-west formula (`GetSunVector` in `lib/util/commonFunctions.glsl`, and again
in `gbuffers_skybasic.glsl`):

```glsl
gbufferModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData) * 2000.0, 1.0)
```

Without a fix, shadows (from the shadow map) were correct while faces were lit as if the sun were still east/west.
`ShaderSunPatch` recognizes exactly the idiom `vec3(-sin(X), cos(X) * Y)` (same identifier in both) and wraps it:
`(mat3(Ry(yaw)) * vec3(...))`, where `mat3` is built by columns `(c,0,-s), (0,1,0), (s,0,c)`. The rotation is written
into the pack source, so the configuration screen reloads the pack (`Iris.reload()` through reflection) when the
angle changes. It can be disabled in the screen or with `-Dsunorientation.shaderPatch=false`.

## Compatibility design

- **Client only** (`environment: client`). No server code, no change to time or world.
- **Without Iris**: `SunOrientationMixinPlugin` skips the Iris mixins (they are also `@Pseudo`), so no Iris class is
  ever loaded. Iris is accessed only through reflection (`IrisCompat`).
- **Iris version**: the plugin logs a warning outside `>=1.11.7 <1.12`, the range whose bytecode was reviewed.
- **Injection failures** never crash the game (`defaultRequire = 0`). Every hook logs
  `[SunOrientation] hook active: …` the first time it runs, so a missing line shows which part is not rotating.
- **Mod Menu** is `compileOnly`; the entrypoint is only loaded when Mod Menu is present.
- **Nuit**: without custom skyboxes Nuit calls the vanilla sky renderer, so everything rotates. Custom Nuit skyboxes
  draw their own sun/moon (`DecorationBox`) and are not rotated.
- `breaks: sur_norte` prevents loading together with the 0.1.0-alpha test build (different mod id).

## Verification

**Unit tests** (`./gradlew test`, 35 cases) use the real `PoseStack`/`Axis` classes and JOML:

- `GeometryTest` — default orientation (south): sky, sunrise glow, fog, Iris light = vanilla sky, shadow camera
  looking from the light for `sunPathRotation` 0, -40 and 25, and the inverse-sign proof.
- `OrientationsTest` — 12 bearings (including 30°, 45°, 200°, 333°, 359°): every part above plus the GLSL `mat3`.
- `ShaderSunPatchTest` — the exact Complementary lines, and that other forms are left untouched.

**In-game tests** (Fabric client gametest, `src/gametest`, not shipped in the jar):

```bash
./gradlew runClientGameTest -Psunorientation.variant=iris
```

Variants: `vanilla`, `iris`, `nuit`, `iris-nuit`, plus `gallery` / `gallery-vanilla` for the mod page screenshots.
Third-party jars are read from `../_shared/testenv/` (`mods-iris/`, `mods-nuit/`, `mods-extra/`, `shaderpacks/`),
or another folder with `-Ptestenv_dir=…`. The test creates a flat world, sets bearings 180°, 0°, 270° and 45°, and at
several times of day calls the real Iris code through reflection to check:

- `shadowLightPosition` in world space points at the configured bearing at sunrise and the opposite one at sunset;
- the view-space position transformed back with `gbufferModelView⁻¹` matches it;
- `ShadowMatrices.createBaselineModelViewMatrix` maps the light to (0, 0, 1).

It also clicks through the configuration screen (N preset, Cancel, West + Done saves 270°), checks the Mod Menu
entrypoint, switches the game to Spanish and takes screenshots. The report is written to
`build/run/clientGameTest/sunorientation-results.txt`.

Last results (1.0.0, Minecraft 26.3, Fabric Loader 0.19.5, Iris 1.11.7, Sodium 0.9.2, Complementary r5.9.3 with
default settings): all variants pass; 58 numeric checks with Iris. Example screenshots are in
[`screenshots/`](screenshots/) (from the 0.1 test build, south orientation).

## Known limitations

- Complementary was checked with its default settings; water reflections, volumetric clouds in detail and
  non-default `sunPathRotation` were not reviewed visually (the math is covered by unit tests).
- Other shader packs that compute the sun in GLSL with a different formula keep east-west lighting, although
  their sun position (from Iris uniforms) and shadows follow the setting.
- Custom Nuit skyboxes are not rotated. Distant Horizons is untested.
- The End "end flash" path of Iris is not modified (the End has no sun).
