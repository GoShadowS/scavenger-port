# scavenger-port

Порт мода **Scavenger** (автор `catboybinary`, лицензия **GPL-3.0**) с Minecraft **26.3** на Minecraft **1.21.1**.

| Папка | Что внутри |
| --- | --- |
| `scavenger-26.3/` | Оригинальные исходники с ветки `26.3` (не тронуты, только удалён мусорный файл `delete this file AI`). |
| `scavenger-1.21.1/` | **Сам порт** — отдельный multiloader-проект (Fabric + NeoForge, Architectury) под Minecraft 1.21.1. |

---

## Порт (`scavenger-1.21.1/`)

* **Minecraft:** 1.21.1, **Java 21**, маппинги Mojang + Parchment `1.21.1:2024.11.17`.
* **Лоадеры:** Fabric (`fabric-loader 0.16.14`, `fabric-api 0.116.6+1.21.1`) и NeoForge (`21.1.209`).
* **Зависимости:** [Architectury API](https://github.com/architectury/architectury-api) `13.0.8` и [OctoLib](https://github.com/Octo-Studios/octo-lib) `0.6.2+1.21` (преемник ShatterLib, на котором написан оригинал).
* **Сборка:** `cd scavenger-1.21.1 && ./gradlew build` → `fabric/build/libs/scavenger-fabric-1.1.2+1.21.1.jar`, `neoforge/build/libs/scavenger-neoforge-1.1.2+1.21.1.jar`.

> ⚠️ Порт написан по декомпилированным исходникам Minecraft 1.21.1 (Mojang-маппинги), исходникам OctoLib 0.6.2 и Architectury 13, но **не компилировался** в среде, где он делался (нет JDK/Gradle и доступа к maven-репозиториям). Перед публикацией нужен один прогон `./gradlew build` — список всех переписанных мест и спорных решений лежит в [`scavenger-1.21.1/PORT_NOTES.md`](scavenger-1.21.1/PORT_NOTES.md).

## IntelliJ IDEA

1. **JDK 21** (Temurin / JetBrains Runtime 21). Не 22+ — Loom 1.13 и Minecraft 1.21.1 на более новых JDK падают.
2. `File → Open…` → выбрать папку **`scavenger-1.21.1`** (не корень `scavenger-port`: там лежит ещё и проект под 26.3 с Java 25, IDEA попытается импортировать оба). Подтвердить `Trust Project`.
3. `Settings → Build, Execution, Deployment → Build Tools → Gradle`:
   * **Gradle JVM = 21**;
   * `Use Gradle from` → `gradle-wrapper.properties` (Gradle 8.12.1);
   * `Build and run using` / `Run tests using` → **Gradle**.
4. `Reload All Gradle Projects` (иконка слона). Первый синк качает Gradle, Minecraft 1.21.1, Mojang+Parchment маппинги и декомпилирует клиент — 5–20 минут и несколько ГБ в `~/.gradle`.
5. После синка в `Run/Debug Configurations` появятся **Minecraft Client** и **Minecraft Server** для подпроектов `:fabric` и `:neoforge` — запуск из IDE создаёт `fabric/run/` или `neoforge/run/`. Fabric API, Architectury и OctoLib уже в dev-класспасе (`modImplementation`), докладывать их руками не нужно.
6. Сборка: `./gradlew build` → `fabric/build/libs/scavenger-fabric-1.1.2+1.21.1.jar`, `neoforge/build/libs/scavenger-neoforge-1.1.2+1.21.1.jar` (публиковать обычный jar, не `-dev-shadow` и не `-sources`).

## Если синк или сборка падает

| Ошибка | Причина → решение |
| --- | --- |
| `Unsupported class file major version 68/69`, `Invalid source release: 21` | Gradle запущен не на JDK 21 → `Settings → Build Tools → Gradle → Gradle JVM` поставить на 21 |
| `Could not resolve dev.architectury.loom:1.13-SNAPSHOT` | недоступен `maven.architectury.dev` (SNAPSHOT-репозиторий) → попробовать в `build.gradle` версии `1.11-SNAPSHOT`, `1.10-SNAPSHOT`, `1.9-SNAPSHOT` и повторить с `--refresh-dependencies` |
| `Could not find it.hurts.octostudios.octolib:octolib-{common,fabric,neoforge}:0.6.2+1.21` | OctoLib лежит на `maven.shatterbyte.com/releases` (**не** `maven.octo-studios.com` — этот домен заброшен и паркуется, Gradle на нём виснет до таймаута) → взять другую версию (`0.6.1+1.21`, `0.6.0.3+1.21`) или подложить jar OctoLib в `libs/` и заменить зависимость на `files(...)` |
| `Could not GET 'https://ww38.<какой-то-домен>/...' failed: Read timed out` | репозиторий мёртв/припаркован; таймаут Gradle считает фатальной ошибкой и не идёт к следующему репозиторию → убрать/заменить URL и ограничить репозитории фильтром `content { includeGroup ... }` |
| `Could not find net.neoforged:neoforge:21.1.209` | недоступен `maven.neoforged.net/releases` → любая `21.1.x` из списка NeoForge для 1.21.1 |
| `Could not find org.parchmentmc.data:parchment-1.21.1:2024.11.17@zip` | недоступен `maven.parchmentmc.org` → заменить блок `mappings` на `mappings loom.officialMojangMappings()` (Parchment — только имена параметров, на работу не влияет) |
| `OutOfMemoryError` / `GC overhead limit exceeded` при декомпиляции | `-Xmx3G` в `gradle.properties` (уже выставлено), закрыть другие проекты IDEA |
| Не появились run-конфиги `Minecraft Client/Server` | не отработал `platformSetupLoomIde()` → удалить `.idea/` и `.gradle/`, переоткрыть проект, повторить sync |
| Ошибки компиляции в Java-коде | ожидаемо: порт писался без компилятора. Прогнать `./gradlew build --console=plain 2>&1 \| tee build.log` и править по логу |
| IDEA подсвечивает `@Mixin`-цели красным, но Gradle собирается | норма: IDE не знает про ремаппинг Loom; доверять выводу `./gradlew build` |

Описание самого переноса (замены API, модификаторы, миксины, отличия от 26.3) — в [`scavenger-1.21.1/PORT_NOTES.md`](scavenger-1.21.1/PORT_NOTES.md).

## Что перенесено

Полный набор фич версии 1.1.2: колёса предмета и модификатора, экран победы, HUD-таймер (с учётом тостов), история забегов + экран деталей, ручной выбор предмета/модификатора, 33 модификатора, «турист» (случайный язык), нуарный пост-эффект, пересоздание мира после смерти в хардкоре, seed в экране победы и в истории забегов, ru/uk локализации.

## Лицензия и публикация

Оригинал распространяется под **GPL-3.0-or-later** — порт разрешён без отдельного согласия автора при соблюдении условий лицензии (текст лицензии, указание авторства, пометка об изменениях, распространение исходников под той же лицензией). Текст лицензии — в [`scavenger-1.21.1/LICENSE`](scavenger-1.21.1/LICENSE), уведомление об оригинале и ссылки на все площадки — в [`scavenger-1.21.1/README.md`](scavenger-1.21.1/README.md).

---

## English TL;DR

Unofficial **Minecraft 1.21.1** port of `catboybinary/scavenger` (GPL-3.0), kept in a separate folder next to the untouched 26.3 sources. Fabric + NeoForge via Architectury, running on OctoLib 0.6.2. Feature-complete port; **not yet compiled** — see `scavenger-1.21.1/PORT_NOTES.md` for every API change and every known difference from 26.3. Publishing must credit the original author, keep the GPL-3.0 license and source link, and treat OctoLib as an external (all-rights-reserved) runtime dependency.
