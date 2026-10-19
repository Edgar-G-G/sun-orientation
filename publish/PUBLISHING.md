# Publishing Sun Orientation 1.0.0

Everything both platforms need is in this folder. Publishing is done from your own accounts: it is a public
action and cannot be fully undone once the project is approved and indexed.

| File | Use |
| --- | --- |
| `../release/sun-orientation-1.0.0.jar` | the file to upload (same jar for both platforms) |
| `../CHANGELOG.md` | changelog of the version (section 1.0.0) |
| `gallery/` | screenshot shared by both platforms |
| `modrinth/description.md`, `modrinth/icon-512.png` | Modrinth page and icon |
| `curseforge/description.md`, `curseforge/icon-512.png` | CurseForge page and avatar |

## 1. GitHub

1. Create the empty public repository **`Edgar-G-G/sun-orientation`**, without README, license or `.gitignore`
   (they are already here). The jar already links to it (`github_repo` in `gradle.properties`).
2. Push the code:

   ```bash
   git remote add origin https://github.com/Edgar-G-G/sun-orientation.git
   git push -u origin main
   ```

   `.github/workflows/build.yml` builds and runs the tests on every push.

## 2. Modrinth

https://modrinth.com → **Create a project**

| Field | Value |
| --- | --- |
| Name | Sun Orientation |
| URL / slug | `sun-orientation` (available on 2026-10-18) |
| Summary | Choose where the sun rises: south, north, west or any angle. The sky, shadows and shader lighting (Iris, Complementary) follow. Client-side. |
| Project type | Mod |
| Icon | `modrinth/icon-512.png` |
| Description | paste `modrinth/description.md` |
| Categories | **Decoration** (optionally **Utility**) |
| Environment | Client: **Required** · Server: **Unsupported** |
| License | **MIT** |
| Links | Source: https://github.com/Edgar-G-G/sun-orientation · Issues: https://github.com/Edgar-G-G/sun-orientation/issues |

**Versions → Create a version**

| Field | Value |
| --- | --- |
| File | `../release/sun-orientation-1.0.0.jar` |
| Version name / number | Sun Orientation 1.0.0 / `1.0.0` |
| Release channel | Release |
| Loaders / Game versions | Fabric / 26.3 |
| Changelog | section 1.0.0 of `CHANGELOG.md` |
| Dependencies | Iris (**optional**), Mod Menu (**optional**) |

Modrinth reviews new projects manually; it usually takes from a few hours to a few days.

## 3. CurseForge

https://authors.curseforge.com → **Create Project** → Minecraft → **Mods**

| Field | Value |
| --- | --- |
| Project name | Sun Orientation |
| Summary | Choose where the sun rises: south, north, west or any angle. The sky, shadows and shader lighting follow. Client-side. |
| Description | paste `curseforge/description.md` (Markdown editor) |
| Avatar | `curseforge/icon-512.png` (CurseForge requires at least 400×400) |
| Main category | **Cosmetic** |
| Additional categories | **Utility & QoL** |
| License | **MIT License** |
| Source / Issues | https://github.com/Edgar-G-G/sun-orientation · `…/issues` |

**Files → Upload file**

| Field | Value |
| --- | --- |
| File | `../release/sun-orientation-1.0.0.jar` |
| Display name | Sun Orientation 1.0.0 |
| Release type | Release |
| Game version | 26.3 |
| Mod loader | Fabric |
| Java version | Java 25 |
| Environment | Client |
| Changelog | section 1.0.0 of `CHANGELOG.md` |
| Relations | Iris Shaders (**Optional Dependency**), Mod Menu (**Optional Dependency**) |

CurseForge also reviews new projects and files before they are public.

## Gallery (both platforms)

| File | Title | Description |
| --- | --- | --- |
| `config-screen.png` | Compass screen | Pick any sunrise direction; the sky updates live behind the menu. Use it as the **featured** image. |

## Status (1.0.0)

| Platform | Project | State on 2026-10-19 |
| --- | --- | --- |
| GitHub | https://github.com/Edgar-G-G/sun-orientation | public |
| Modrinth | https://modrinth.com/mod/sun-orientation (id `chwkC3Ka`) | submitted for review, "Contains AI-generated content" declared |
| CurseForge | project id `1753365` | file 1.0.0 uploaded, waiting for moderation (auto-publish on approval, 3rd-party distribution allowed) |
