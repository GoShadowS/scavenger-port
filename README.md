# scavenger-port

Port of the **Scavenger** mod (by `catboybinary`, **GPL-3.0**) from Minecraft **26.3** to Minecraft **1.21.1**.

| Folder | Contents |
| --- | --- |
| `scavenger-26.3/` | Original sources from the `26.3` branch (untouched, except for deleting the stray `delete this file AI`). |
| `scavenger-1.21.1/` | **The port** — a separate multiloader project (Fabric + NeoForge, Architectury) for Minecraft 1.21.1. |

---

## The port (`scavenger-1.21.1/`)

* **Minecraft:** 1.21.1, **Java 21**, Mojang mappings + Parchment `1.21.1:2024.11.17`.
* **Loaders:** Fabric (`fabric-loader 0.16.14`, `fabric-api 0.116.6+1.21.1`) and NeoForge (`21.1.209`).
* **Dependencies:** [Architectury API](https://github.com/architectury/architectury-api) `13.0.8` and [OctoLib](https://github.com/Octo-Studios/octo-lib) `0.6.2+1.21` (the successor of ShatterLib, which the original is built on).
* **Build:** `cd scavenger-1.21.1 && ./gradlew build` → `fabric/build/libs/scavenger-fabric-1.1.2+1.21.1.jar`, `neoforge/build/libs/scavenger-neoforge-1.1.2+1.21.1.jar`.

> ⚠️ The port was written against decompiled Minecraft 1.21.1 sources (Mojang mappings), the OctoLib 0.6.2 sources and Architectury 13, but it was **never compiled** in the environment where it was made (no JDK/Gradle and no access to maven repositories). Run `./gradlew build` once before publishing — every rewritten spot and every judgement call is listed in [`scavenger-1.21.1/PORT_NOTES.md`](scavenger-1.21.1/PORT_NOTES.md).

## IntelliJ IDEA

1. **JDK 21** (Temurin / JetBrains Runtime 21). Not 22+ — Loom 1.13 and Minecraft 1.21.1 break on newer JDKs.
2. `File → Open…` → pick the **`scavenger-1.21.1`** folder (not the `scavenger-port` root: it also holds the 26.3 project, which needs Java 25, and IDEA would try to import both). Confirm `Trust Project`.
3. `Settings → Build, Execution, Deployment → Build Tools → Gradle`:
   * **Gradle JVM = 21**;
   * `Use Gradle from` → `gradle-wrapper.properties` (Gradle 8.12.1);
   * `Build and run using` / `Run tests using` → **Gradle**.
4. `Reload All Gradle Projects` (the elephant icon). The first sync downloads Gradle, Minecraft 1.21.1 and the Mojang + Parchment mappings, then decompiles the client — 5–20 minutes and a few GB in `~/.gradle`.
5. After the sync, `Run/Debug Configurations` contains **Minecraft Client** and **Minecraft Server** for the `:fabric` and `:neoforge` subprojects — launching from the IDE creates `fabric/run/` or `neoforge/run/`. Fabric API, Architectury and OctoLib are already on the dev classpath (`modImplementation`); there is nothing to drop in by hand.
6. Build: `./gradlew build` → `fabric/build/libs/scavenger-fabric-1.1.2+1.21.1.jar`, `neoforge/build/libs/scavenger-neoforge-1.1.2+1.21.1.jar` (publish the plain jar, not `-dev-shadow` and not `-sources`).

## If the sync or the build fails

| Error | Cause → fix |
| --- | --- |
| `Unsupported class file major version 68/69`, `Invalid source release: 21` | Gradle is not running on JDK 21 → set `Settings → Build Tools → Gradle → Gradle JVM` to 21 |
| `Could not resolve dev.architectury.loom:1.13-SNAPSHOT` | `maven.architectury.dev` (a SNAPSHOT repository) is unreachable → try `1.11-SNAPSHOT`, `1.10-SNAPSHOT` or `1.9-SNAPSHOT` in `build.gradle` and re-run with `--refresh-dependencies` |
| `Could not find it.hurts.octostudios.octolib:octolib-{common,fabric,neoforge}:0.6.2+1.21` | OctoLib is served from `maven.shatterbyte.com/releases` (**not** `maven.octo-studios.com` — that domain is abandoned and parked, and Gradle hangs on it until it times out) → use another version (`0.6.1+1.21`, `0.6.0.3+1.21`), or drop the OctoLib jar into `libs/` and switch the dependency to `files(...)` |
| `Could not GET 'https://ww38.<some-domain>/...' failed: Read timed out` | the repository is dead/parked; Gradle treats a timeout as a fatal error and does not fall through to the next repository → remove or replace the URL, and scope repositories with `content { includeGroup ... }` |
| `Could not find net.neoforged:neoforge:21.1.209` | `maven.neoforged.net/releases` unreachable → any `21.1.x` from NeoForge's 1.21.1 list |
| `Could not find org.parchmentmc.data:parchment-1.21.1:2024.11.17@zip` | `maven.parchmentmc.org` unreachable → replace the `mappings` block with `mappings loom.officialMojangMappings()` (Parchment only supplies parameter names, it does not affect behaviour) |
| `OutOfMemoryError` / `GC overhead limit exceeded` while decompiling | `-Xmx3G` in `gradle.properties` (already set); close other IDEA projects |
| No `Minecraft Client/Server` run configurations appeared | `platformSetupLoomIde()` did not run → delete `.idea/` and `.gradle/`, reopen the project, sync again |
| Java compilation errors | expected: the port was written without a compiler. Run `./gradlew build --console=plain 2>&1 \| tee build.log` and fix things according to the log |
| IDEA shows `@Mixin` targets in red but Gradle builds fine | normal: the IDE knows nothing about Loom's remapping; trust the `./gradlew build` output |

The description of the port itself (API replacements, modifiers, mixins, differences from 26.3) lives in [`scavenger-1.21.1/PORT_NOTES.md`](scavenger-1.21.1/PORT_NOTES.md).

## What was ported

The full 1.1.2 feature set: the item and modifier wheels, the victory screen, the HUD timer (toast-aware), run history plus the details screen, manual item/modifier selection, 33 modifiers, "tourist" (a random language), the noir post effect, world re-creation after a hardcore death, the seed on the victory screen and in run history, and the ru/uk localizations.

## License and publishing

The original is distributed under **GPL-3.0-or-later** — porting it is allowed without the author's separate permission as long as the license terms are met (include the license text, keep the attribution, mark the work as modified, and distribute the sources under the same license). The license text is in [`scavenger-1.21.1/LICENSE`](scavenger-1.21.1/LICENSE); the attribution notice and the links to every platform are in [`scavenger-1.21.1/README.md`](scavenger-1.21.1/README.md).
