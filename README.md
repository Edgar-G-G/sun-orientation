<p align="center">
  <img src="src/main/resources/assets/sun_orientation/icon.png" width="96" alt="Sun Orientation icon">
</p>

<h1 align="center">Sun Orientation</h1>

<p align="center">
  <b>Choose where the sun rises.</b> South, north, west or any angle — the sky, shadows and shader lighting follow.<br>
  Fabric · Minecraft 26.3 · client-side only
</p>

<p align="center">
  <img src="publish/gallery/01-sunrise-south-complementary.jpg" width="720" alt="Sunrise in the south with Complementary Reimagined">
</p>

## Features

- **Any sunrise direction.** Pick North, East, South or West with one click, or any angle from 0° to 359°.
  The sun always sets on the opposite side and passes through the zenith at noon. Default: sunrise in the south.
- **Everything moves together.** Sun, moon and stars, the orange sunrise glow, and the sunrise tint of the fog.
- **Shaders (Iris).** Sun and moon positions, `shadowLightPosition`, the shadow map and the culling of shadows
  are rotated consistently, so shadows fall away from the sun you see.
- **Complementary, BSL and similar packs.** These packs compute the sun direction in their own GLSL code
  instead of using Iris' uniforms. Sun Orientation adapts that code when the pack loads, so block lighting,
  sky scattering and reflections match too. It can be turned off.
- **Live preview.** A compass screen (via Mod Menu): click or drag on the compass, use the presets or the slider,
  and watch the sky change behind the menu. Hold Shift for free angles.
- **Purely visual.** It does not change the time of day, the day length, world generation, servers or other
  players. Works in singleplayer and on any server; nothing to install server-side.
- **No hard dependencies.** Iris and Mod Menu are optional. English and Spanish translations.

## Requirements

| | |
| --- | --- |
| Minecraft | 26.3 |
| Loader | Fabric Loader 0.19.5 or newer |
| Java | 25 |
| Optional | [Iris](https://modrinth.com/mod/iris) 1.11.7+ (shaders), [Mod Menu](https://modrinth.com/mod/modmenu) (config screen) |

## Configuration

Open **Mods → Sun Orientation → ⚙** (Mod Menu). Or edit `config/sun_orientation.json`:

```json
{
  "sunriseBearing": 180,
  "adaptShaderPacks": true
}
```

`sunriseBearing` is a compass bearing: `0` north, `90` east (vanilla), `180` south, `270` west.
When you press **Done** with a shader pack active, the pack is reloaded so its lighting uses the new angle.

## Compatibility

| | Status |
| --- | --- |
| Vanilla sky (no shaders) | Tested in game |
| Iris 1.11.7 + Sodium 0.9.2 | Tested in game: uniforms, shadow matrix and culling checked numerically |
| Complementary Reimagined r5.9.3 | Tested in game (default settings): sun, shadows, lighting |
| Other shader packs | Sun position and shadows follow if the pack uses Iris' uniforms. Packs that compute the sun in GLSL with a different formula keep east–west lighting. |
| Nuit | Compatible. Custom skyboxes from Nuit resource packs draw their own sun and are not rotated. |
| Distant Horizons | Not tested |

If something does not follow the setting, `logs/latest.log` lists every hook that ran (`[SunOrientation] hook active: …`).

## Building

JDK 25 is required; the Gradle wrapper downloads Gradle 9.7.1.

```bash
./gradlew build
```

The jar is written to `build/libs/` and copied to `release/`. `./gradlew test` runs the geometry tests (35 cases, every rotated piece
for 12 directions). In-game tests use Fabric's client gametest; see [docs/TECHNICAL.md](docs/TECHNICAL.md)
for the technical notes and how to run them.

## License

[MIT](LICENSE) © 2026 Edgar_G. Inspired by the idea of [ClassicSunRotation](https://github.com/MinecraftAtHome/ClassicSunRotation)
(MIT); no code from Minecraft, Iris or shader packs is included.
