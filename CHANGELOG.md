# Changelog

## 1.1.0 — Minecraft 26.1.2, 26.2 and 26.3

- NeoForge builds for Minecraft 26.3, 26.2 and 26.1.2; the configuration screen opens from the NeoForge mods list
  (Mods → Sun Orientation → Config).
- Fabric builds for Minecraft 26.2 and 26.1.2 (in addition to 26.3).
- Quilt: the Fabric jar works on Quilt Loader 0.31.
- Iris 1.11.4 is supported on 26.2 and 26.1.2 (1.11.7 on 26.3).
- No change in behaviour on Fabric 26.3.

## 1.0.0 — Minecraft 26.3

First public release.

- Configurable sunrise direction: North / East / South / West presets or any angle (0–359°). Default: south.
- Configuration screen with an interactive compass and live preview, available from Mod Menu.
  Settings are stored in `config/sun_orientation.json`.
- Rotates the vanilla sun, moon and stars, the sunrise/sunset glow and the sunrise tint of the fog.
- Iris: sun/moon/shadow-light positions (view and world space), shadow view matrix and shadow culling.
- Shader packs that compute the sun in GLSL (Complementary, BSL and derivatives) are adapted on load;
  the pack reloads automatically when the angle changes. Can be turned off.
- English and Spanish translations.
- Client-side only, no required dependencies.

## 0.1.0-alpha (private test build)

- Fixed south sunrise / north sunset. Mod id `sur_norte` (1.0 refuses to load alongside it).
