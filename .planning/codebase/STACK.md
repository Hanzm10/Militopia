# Technology Stack
_Last updated: 2026-03-26_

## Summary
Militopia is a Java desktop game built on libGDX 1.14.0 with the LWJGL3 backend. It uses Ashley ECS for game logic, Gradle 7.6 as the build system, and targets Java 8 source/bytecode compatibility while running on JDK 17+.

## Languages

**Primary:**
- Java - source compatibility level 8, runs on JDK 17 (enforced via Netbeans hint and Temurin 17 bundles)

## Runtime

**Environment:**
- JVM (JDK 17 recommended — `netbeans.hint.jdkPlatform=JDK_17`, distribution bundles use Temurin 17.0.15)

**Package Manager / Build:**
- Gradle 7.6 (wrapper at `gradle/wrapper/gradle-wrapper.properties`)
- No lockfile detected

## Frameworks

**Core Game Framework:**
- libGDX `1.14.0` (`gdxVersion` in `gradle.properties`) — rendering, audio, input, asset management, UI (Scene2D)

**ECS (Entity-Component-System):**
- Ashley `1.7.4` (`ashleyVersion` in `gradle.properties`) — all game entities, systems, and components

**Physics:**
- gdx-box2d `1.14.0` — declared as dependency, available if needed

**Font Rendering:**
- gdx-freetype `1.14.0` — TTF font loading via `FreeTypeFontGenerator`

**Build / Distribution:**
- Construo `2.1.0` (`io.github.fourlastor:construo`) — cross-platform native executable packaging (Linux x64, macOS M1/x64, Windows x64)

**Optional / Disabled:**
- GraalVM Native Image support (`enableGraalNative=false`) via `gdx-svmhelper` `2.0.1` — disabled by default

## Testing

- JUnit Jupiter `5.10.0` (BOM: `org.junit:junit-bom:5.10.0`)
- Mockito Core `5.11.0`
- JUnit Platform Launcher (runtime only)
- Runner: `useJUnitPlatform()` in `core/build.gradle`
- Test files: `core/src/test/java/com/militopia/`

## Key Dependencies (core module)

| Dependency | Version | Purpose |
|---|---|---|
| `com.badlogicgames.gdx:gdx` | 1.14.0 | Core libGDX framework |
| `com.badlogicgames.ashley:ashley` | 1.7.4 | Entity-Component-System |
| `com.badlogicgames.gdx:gdx-box2d` | 1.14.0 | Physics (declared, available) |
| `com.badlogicgames.gdx:gdx-freetype` | 1.14.0 | TTF font rendering |

## Key Dependencies (lwjgl3 module)

| Dependency | Version | Purpose |
|---|---|---|
| `gdx-backend-lwjgl3` | 1.14.0 | Desktop OpenGL backend |
| `gdx-platform:natives-desktop` | 1.14.0 | Native binaries |
| `gdx-box2d-platform:natives-desktop` | 1.14.0 | Box2D native binaries |
| `gdx-freetype-platform:natives-desktop` | 1.14.0 | FreeType native binaries |
| `gdx-lwjgl3-angle` | 1.14.0 | ANGLE OpenGL ES 2.0 emulation (GLES20) |
| `gdx-tools` | 1.14.0 | Dev tooling (excludes old lwjgl backend) |

## Build Configuration

**Gradle JVM args:** `-Xms512M -Xmx1G -Dfile.encoding=UTF-8`
**Daemon:** disabled (`org.gradle.daemon=false`)
**Incremental compilation:** enabled
**Asset list generation:** custom `generateAssetList` task writes `assets/assets.txt` before every build
**Output JAR:** `Militopia-1.0.0.jar` (fat JAR, all runtimes bundled)

**Platform-specific JARs:**
- `jarWin` — Windows only
- `jarMac` — macOS only
- `jarLinux` — Linux only

## Project Version

- `projectVersion=1.0.0` (`gradle.properties`)

## Platform Requirements

**Development:**
- JDK 17+
- Gradle 7.6 (via wrapper — no separate install needed)

**Production Targets (via Construo):**
- Linux x64 (Temurin 17.0.15 bundled)
- macOS M1/ARM64 (Temurin 17.0.15 bundled)
- macOS x64 (Temurin 17.0.15 bundled)
- Windows x64 (Temurin 17.0.15 bundled)

**Window / Renderer:**
- Default windowed mode: 640x480
- VSync enabled, FPS capped to monitor refresh rate + 1
- Renderer: ANGLE GLES20 emulation (`setOpenGLEmulation(ANGLE_GLES20, 0, 0)`)

## Repository Layout (build-relevant)

```
Militopia/
├── build.gradle              # Root build — plugins, asset list task, subproject config
├── gradle.properties         # Versions, JVM args, feature flags
├── gradle/wrapper/           # Gradle 7.6 wrapper
├── core/
│   └── build.gradle          # Core deps: gdx, ashley, box2d, freetype, junit, mockito
└── lwjgl3/
    └── build.gradle          # Desktop launcher: lwjgl3 backend, construo packaging
```
