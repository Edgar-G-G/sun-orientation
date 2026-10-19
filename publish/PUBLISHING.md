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
   (they are already here). The jar already links to it (`mod.github_repo` in `stonecutter.properties.toml`).
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

## Version 1.1.0 (multi-loader)

Upload only after the 1.0.0 review is finished and with explicit approval. Same six files on both platforms, from
`../release/` (built by `gradlew build`). Changelog: section 1.1.0 of `CHANGELOG.md`. Before uploading, update the
project descriptions (`modrinth/description.md`, `curseforge/description.md`, already rewritten for 1.1.0).

| File | Version number (Modrinth) / display name (CurseForge) | Loaders | Game version |
| --- | --- | --- | --- |
| `sun-orientation-1.1.0+26.3-fabric.jar` | `1.1.0+26.3-fabric` / Sun Orientation 1.1.0 (26.3, Fabric/Quilt) | Fabric, Quilt | 26.3 |
| `sun-orientation-1.1.0+26.3-neoforge.jar` | `1.1.0+26.3-neoforge` / Sun Orientation 1.1.0 (26.3, NeoForge) | NeoForge | 26.3 |
| `sun-orientation-1.1.0+26.2-fabric.jar` | `1.1.0+26.2-fabric` / Sun Orientation 1.1.0 (26.2, Fabric/Quilt) | Fabric, Quilt | 26.2 |
| `sun-orientation-1.1.0+26.2-neoforge.jar` | `1.1.0+26.2-neoforge` / Sun Orientation 1.1.0 (26.2, NeoForge) | NeoForge | 26.2 |
| `sun-orientation-1.1.0+26.1.2-fabric.jar` | `1.1.0+26.1.2-fabric` / Sun Orientation 1.1.0 (26.1.2, Fabric/Quilt) | Fabric, Quilt | 26.1.2 |
| `sun-orientation-1.1.0+26.1.2-neoforge.jar` | `1.1.0+26.1.2-neoforge` / Sun Orientation 1.1.0 (26.1.2, NeoForge) | NeoForge | 26.1.2 |

Other fields as in 1.0.0 (Release, Java 25, client). Dependencies: Iris **optional** on every file; Mod Menu
**optional** on the Fabric files only. Quilt was tested in game on 26.3 only; on 26.2 and 26.1.2 it is declared because
it runs the same Fabric jar.

## Gallery (both platforms)

| File | Title | Description |
| --- | --- | --- |
| `config-screen.png` | Compass screen | Pick any sunrise direction; the sky updates live behind the menu. Use it as the **featured** image. |

## Status (1.0.0)

| Platform | Project | State on 2026-10-19 |
| --- | --- | --- |
| GitHub | https://github.com/Edgar-G-G/sun-orientation | public |
| Modrinth | https://modrinth.com/mod/sun-orientation (id `chwkC3Ka`) | submitted for review, "Contains AI-generated content" declared |
| CurseForge | project id `1753365` | 1.0.0 approved |

## Status (1.1.0)

| Platform | State on 2026-10-19 |
| --- | --- |
| CurseForge | description updated; 6 files uploaded (ids 9260167 26.3 Fabric/Quilt, 9260174 26.3 NeoForge, 9260181 26.2 NeoForge, 9260184 26.1.2 NeoForge, 9260194 26.2 Fabric/Quilt, 9260195 26.1.2 Fabric/Quilt); the first three approved, the rest under review |
| Modrinth | not uploaded: the project (and 1.0.0) is still in review |
