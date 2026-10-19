**Choose where the sun rises.** South, north, west or any angle — the sky, the shadows and the lighting of your shader pack follow it.

In vanilla Minecraft the sun always rises in the east and sets in the west. Sun Orientation lets you move that path: have the sun rise in the south and set in the north, rise in the north like in old Beta versions, or pick any compass angle you like. It always sets on the opposite side and crosses the zenith at noon.

## ✨ Features

- **Any direction** — North / East / South / West presets or any angle from 0° to 359°. Default: sunrise in the south.
- **Everything moves together** — sun, moon and stars, the orange sunrise glow, and the sunrise tint of the fog.
- **Made for shaders** — with Iris, the sun and moon positions, the shadow light, the shadow map and shadow culling are rotated consistently, so shadows always fall away from the sun you see.
- **Complementary, BSL & co.** — these packs calculate the sun direction in their own shader code instead of using Iris' values. Sun Orientation adapts that code when the pack loads, so block lighting, sky scattering and reflections match as well.
- **Live compass screen** — open it from Mod Menu (Fabric, Quilt) or the mods list (NeoForge), click or drag on the compass, and watch the sky turn behind the menu. Hold Shift for free angles.
- **Purely visual & client-side** — no change to the time of day, day length, world generation or gameplay. Works on any server; nothing to install there.
- **No required dependencies** — Iris and Mod Menu are optional. Available in English and Spanish.
- **Fabric, Quilt and NeoForge** — for Minecraft 26.3, 26.2 and 26.1.2. Download the file for your version and loader (Quilt uses the Fabric file).

## ⚙️ Configuration

**Mods → Sun Orientation → ⚙** (Fabric/Quilt, requires Mod Menu) or **Mods → Sun Orientation → Config** (NeoForge), or edit `config/sun_orientation.json`:

```json
{ "sunriseBearing": 180, "adaptShaderPacks": true }
```

`sunriseBearing`: `0` north · `90` east (vanilla) · `180` south · `270` west.

## 🧩 Compatibility

| | |
| --- | --- |
| Vanilla sky | ✅ Tested |
| Iris + Sodium | ✅ Tested on Fabric, Quilt and NeoForge — sun, moon, shadows (Iris 1.11.7 on 26.3, 1.11.4 on 26.2 and 26.1.2) |
| Complementary Reimagined r5.9.3 | ✅ Tested (default settings) — sun, shadows, lighting |
| Other shader packs | ✔️ Sun position and shadows follow when the pack uses Iris' uniforms. Packs computing the sun with a different formula keep their own east–west lighting. |
| Nuit | ✔️ Compatible. Custom Nuit skyboxes draw their own sun and are not rotated. |
| Distant Horizons | ❔ Not tested |

Something not following the setting? `logs/latest.log` lists every part that is active (`[SunOrientation] hook active: …`). Please open an issue on [GitHub](https://github.com/Edgar-G-G/sun-orientation/issues) with that log.
