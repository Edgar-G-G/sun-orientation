# Changelog

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
