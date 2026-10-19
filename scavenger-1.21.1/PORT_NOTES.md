# Заметки по порту Scavenger 26.3 → 1.21.1

Документ описывает, **что именно и как** было изменено при переносе мода с Minecraft 26.3 на 1.21.1.
Оригинал: <https://github.com/catboybinary/scavenger> (ветка `26.3`, GPL-3.0-or-later).
Порт не изменял оригинальные исходники — они лежат рядом в `../scavenger-26.3/`.

---

## 1. Целевые версии и структура порта

```
scavenger-1.21.1/
├── build.gradle                 # architectury loom 1.13-SNAPSHOT, architectury-plugin 3.4-SNAPSHOT, shadow 8.1.1
├── gradle.properties            # версии MC/лоадеров/библиотек
├── gradle/wrapper/*             # Gradle 8.12.1 (wrapper jar взят из architectury-api, ветка 1.21)
├── common/                      # общий код + ресурсы + scavenger.mixins.json
├── fabric/                      # точка входа Fabric + fabric.mod.json
└── neoforge/                    # точка входа NeoForge + META-INF/neoforge.mods.toml
```

Версии, на которые делался перенос (в `gradle.properties`): `minecraft 1.21.1`, `architectury 13.0.8`,
`fabric-loader 0.16.14`, `fabric-api 0.116.6+1.21.1`, `neoforge 21.1.209`, `octolib 0.6.2+1.21`,
маппинги `mojang + parchment 1.21.1:2024.11.17`, Java 21. Версии тулчейна (loom `1.13-SNAPSHOT`,
architectury-plugin `3.4-SNAPSHOT`, shadow `8.1.1`, Gradle `8.12.1`) взяты из ветки `1.21.11` оригинала
и из проекта relics — то есть из реально собирающихся проектов под 1.21.1.

Репозитории, откуда порт тянет зависимости: `maven.shatterbyte.com/releases` (OctoLib),
`maven.parchmentmc.org`, `maven.neoforged.net/releases`, `maven.fabricmc.net`, `maven.architectury.dev`.

---

## 2. Замена библиотеки: ShatterLib → OctoLib

| 26.x (ShatterLib 0.7.0-beta.1) | 1.21.1 (OctoLib 0.6.2) |
| --- | --- |
| `it.hurts.shatterbyte.shatterlib.*` | `it.hurts.octostudios.octolib.*` |
| `ShatterLibServices.platform().getConfigDirectory()` | не нужно — `ConfigManager` сам создаёт/читает файл |
| `ConfigManager.register(id, config)` + `config.load(dir)` | `ConfigManager.registerConfig(id, config)` (сам создаёт файл и перезагружает) |
| `config.save()` | `ConfigManager.uploadDataToConfig(MOD_ID)` (`Scavenger.saveConfig()`) |
| `ShatterLibNetwork.registerS2CPayloadType(...)` | `NetworkManager.registerS2CPayloadType(...)` (только на dedicated-сервере) |
| `ShatterLibNetwork.registerS2CReceiver(type, codec, consumer)` | `NetworkManager.registerReceiver(Side.S2C, type, codec, (packet, ctx) -> ...)` |
| `ShatterLib.LOGGER` | `OctoLib.LOGGER` |
| `RenderPipelines` / цветной `blit` | `ScavengerRender.blitTinted` / `blitDodge` (см. §3) |
| `ConfigScreen` (встроенный экран конфига) | **свой** `ScavengerConfigScreen` (в OctoLib 0.6.2 экрана конфига нет) |

Конфиг (`client/Config.java`) переписан под SnakeYAML-сериализацию OctoLib: она работает **по полям**
(`FieldPropertyExt`), поэтому поля должны быть простыми YAML-типами — цвета хранятся строкой `#AARRGGBB`,
список предметов — как `ArrayList<String>` из id (`minecraft:diamond`), вложенные категории — статические классы.
Аннотации `@Prop(comment = ...)` сохранены, `@Comment`/`@Range` из 26.x в OctoLib 0.6.2 не существуют.

---

## 3. Основные замены API Minecraft (26.x → 1.21.1)

| Было | Стало |
| --- | --- |
| `Identifier` | `ResourceLocation` (+ `readResourceLocation`/`writeResourceLocation`) |
| `pushMatrix/popMatrix`, `extract*` | `pushPose/popPose`, `render*` |
| `guiGraphics.text/centeredText/item/itemDecorations/outline` | `drawString/drawCenteredString/renderItem/renderItemDecorations/renderOutline` |
| `guiGraphics.setTooltipForNextFrame(...)` | `guiGraphics.renderTooltip(...)` (в 1.21.1 тултип рисуется сразу) |
| `guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, ...[, color])` | `blit(tex, x, y, u, v, w, h, tw, th)`; цвет — через `RenderSystem.setShaderColor` (`ScavengerRender.blitTinted`) |
| свой `RenderPipeline` с `BlendFunction(DST_COLOR, ONE)` | `RenderSystem.blendFunc(GL_DST_COLOR, GL_ONE)` вокруг blit (`ScavengerRender.blitDodge`) — в 1.21.1 `GuiGraphics.blit` рисует сразу через `BufferUploader`, поэтому состояние блендинга применяется |
| `pose().translate(x, y)`, `scale(a, b)`, `rotate(rad)` | `translate(x, y, z)`, `scale(a, b, c)`, `mulPose(Axis.ZP.rotation(rad))` |
| `guiGraphics.verticalLine(...)` | `guiGraphics.vLine(...)` |
| `ItemStack.getStyledHoverName()` | `ScavengerClient.styledHoverName(stack)` = `Component.empty().append(stack.getHoverName()).withStyle(stack.getRarity().color())` |
| `component.withColor(int)` | `component.withStyle(style -> style.withColor(int))` |
| `BuiltInRegistries.ITEM.getValue(id)` | `get(id)` / `getOptional(id).orElse(Items.AIR)` |
| `ResourceKey.identifier()` | `ResourceKey.location()` |
| `mouseClicked(MouseButtonEvent, boolean)`, `mouseDragged(MouseButtonEvent, dx, dy)`, `mouseReleased(MouseButtonEvent)`, `keyPressed(KeyEvent)` | `mouseClicked(double, double, int)`, `mouseDragged(double, double, int, double, double)`, `mouseReleased(double, double, int)`, `keyPressed(int, int, int)` |
| `AbstractWidget.onClick(MouseButtonEvent, boolean)` | `onClick(double, double)` |
| `minecraft.gui.setScreen(...)` / `gui.screen()` | `minecraft.setScreen(...)` / `minecraft.screen` |
| `Minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE)` | нет такого метода → `exitToTitleScreen()` как в `DeathScreen`: `level.disconnect(); disconnect(new GenericMessageScreen(translatable("menu.savingLevel"))); setScreen(new TitleScreen())` |
| `CommonComponents.disconnectButtonLabel(local)` | `isLocalServer() ? translatable("menu.returnToMenu") : CommonComponents.GUI_DISCONNECT` |
| `Minecraft.gui.toastManager()` + `ToastManager.visible` | `Minecraft.getToasts()` + `ToastComponent.visible` (`ToastComponentAccessor`, `ToastInstanceAccessor`) |
| `player.getActivePostEffects().add(Identifier)` (нуар) | `OctoLibPostEffects` — класс `NoirPostEffect extends PostEffect` (`RenderStage.LEVEL`, `shouldRender() = ClientScavengerData.is(NOIR)`); добавляется прямо в `OctoLibPostEffects.getPostEffects()`, чтобы не зависеть от порядка инициализации модов |
| `assets/scavenger/post_effect/noir.json` (формат 26.x) | `assets/scavenger/shaders/post/noir.json` (формат 1.21.1: `targets`/`passes`, программы `color_convolve` + `blit`) |
| `level.clockManager().moveToTimeMarker(OVERWORLD, MIDNIGHT/NOON)` | `ServerLevel.setDayTime(18000 / 6000)` |
| `GameRules.ADVANCE_TIME` + `gameRules.set(key, value, server)` | `GameRules.RULE_DAYLIGHT` + `gameRules.getRule(key).set(false, level.getServer())` |
| `player.hurtServer(level, source, amount)` | `player.hurt(source, amount)` |
| `player.drop(stack, false, Prediction.SERVER_ONLY)` | `player.drop(stack, false)` |
| `EnderDragonFight` | `EndDragonFight` (+ `ServerLevel.getDragonFight()`, `hasPreviouslyKilledDragon()`) |
| `ServerLevel`-аксессор для `getServer()` | `Level.getServer()` (nullable) / `ServerPlayer.serverLevel()` |
| `SavedDataType` + Codec-сохранение | `SavedData.Factory<>(new, (tag, provider) -> load(tag), DataFixTypes.LEVEL)` и ручной NBT (`save(CompoundTag, HolderLookup.Provider)`) |
| `NbtAccounter.uncompressedQuota()` | `NbtAccounter.unlimitedHeap()` |
| `tag.getCompound("data").orElse(root)`, `tag.getLong("seed")` → `Optional` | `contains(...)` + обычные `getCompound/getLong` |
| `LevelStorageSource` layout 26.x (`minecraft/world_gen_settings.dat`, `data/<modid>/`) | `<world>/data/scavenger_data.dat`, сид — из `<world>/level.dat` → `Data/WorldGenSettings/seed` (`WorldSeedReader`) |
| `MouseHandler.getScaledXPos(getWindow())` | параметры `mouseX/mouseY` из `render(...)` |
| `AbstractSelectionList.Entry.render` (короткая сигнатура) | 10 параметров `(GuiGraphics, index, top, left, width, height, mouseX, mouseY, hovered, partialTick)` |
| `SoundManager.play(...) -> SoundEngine.PlayResult` | `play(SoundInstance)` возвращает `void` → `CallbackInfo` + `ci.cancel()` |
| `ItemStackTemplate`, `Consumable`, `BedRule`, `isMobilityRestricted`, `util.Prediction` | не существуют — см. §4 |
| `catch (Throwable _)` (unnamed variables) | `catch (Throwable ignored)` — в Java 21 это preview-фича |

HUD и клиентский тик больше не разнесены по лоадерам: в common-коде используются события Architectury —
`ClientGuiEvent.RENDER_HUD` и `ClientTickEvent.CLIENT_POST` (`ScavengerClient.init()`), серверные —
`PlayerEvent.PLAYER_JOIN`, `TickEvent.PLAYER_POST`, `LifecycleEvent.SERVER_LEVEL_LOAD` (`Scavenger.init()`).

---

## 4. Модификаторы: как реализованы в 1.21.1

| Модификатор | 26.x | 1.21.1 |
| --- | --- | --- |
| GIANT / TINY / ALIEN | `Attributes.SCALE` / `GRAVITY` | без изменений (в 1.21.1 те же атрибуты) |
| TURTLE / SONIC | редирект `OptionInstance.get()` в `MouseHandler.turnPlayer` | то же (ordinal 0 — чувствительность) |
| DRUNK | инверсия X и Y через опции | в 1.21.1 нет «invert mouse X» → редирект `invertYMouse().get()` (ordinal 1) + инверсия `accumulatedDX` в HEAD `turnPlayer` (поле обнуляется ванилью сразу после) |
| VEGETARIAN / CARNIVORE | `Consumable.canConsume` | `Item.use(Level, Player, InteractionHand)` HEAD → `InteractionResultHolder.pass(stack)`; теги `scavenger:vegetarian_food`, `minecraft:meat`, `minecraft:fishes` |
| SNAIL | `Player.isMobilityRestricted` | `Player.setSprinting(true)` HEAD → cancel |
| INSOMNIA | `Player.startSleepInBed(bed, state, BedRule, pos)` + свой `BedSleepingProblem` | `startSleepInBed(BlockPos)`; `BedSleepingProblem` в 1.21.1 — enum, поэтому берётся `OTHER_PROBLEM`, а текст `scavenger.insomnia` показывается в action bar |
| SPEED_UP | `level.tickRateManager().setTickRate(40)` | без изменений |
| ECLIPSE / SOLSTICE | `clockManager().moveToTimeMarker(...)` | `setDayTime(18000)` / `setDayTime(6000)` + отключение `doDaylightCycle` |
| HYDROPHOBIC | `hurtServer` | `hurt` |
| FEARFUL | `hurtServer` TAIL | `Player.hurt` TAIL |
| ONE_ARM | `Inventory.SLOT_OFFHAND` + `drop(..., Prediction.SERVER_ONLY)` | `Inventory.SLOT_OFFHAND` (в 1.21.1 есть) + `drop(stack, false)` |
| NOIR | `player.getActivePostEffects()` | `NoirPostEffect` через OctoLib (см. §3) |
| TOURIST | `LanguageManager` | без изменений (API совпадает) |
| ASOCIAL / MAIN_CHARACTER / NPC / UNEDUCATED / HOLEY_POCKETS / BRITTLE_BONES / SHUFFLED_CHESTS / SILENCE / MOLE / FINALIST / DEJAVU / LARGE_BIOMES / AMPLIFIED / TWICE / THRICE / NONE | — | перенесены 1:1 (только переименования API) |

---

## 5. Миксины (`common/src/main/resources/scavenger.mixins.json`)

`compatibilityLevel: JAVA_21`. Клиентские: `CreateWorldScreenAccessor/Mixin`, `DeathScreenInvoker/Mixin`,
`ImageButtonAccessor`, `MinecraftMixin` (`disconnect(Screen, boolean)` — точка сброса клиентских данных),
`OptionsSubScreenAccessor`, `PauseScreenMixin`, `ScreenInvoker`, `TitleScreenMixin`, `ToastComponentAccessor`,
`ToastInstanceAccessor`, `WorldListEntryMixin`, `modifier.{CameraType, ContainerScreen, Food, MouseHandler,
MouseSettingsScreen, Options, RemoveRecipeBook, Screen, SoundEngine}Mixin`.
Общие: `modifier.{CakeBlock, Fearful, LivingEntity, OnlyHotbar, PreventSprinting, ShuffledChests, Slot,
VillagerLevel, Villager}Mixin`.

Удалены: `ServerLevelAccessor` (в 1.21.1 есть `Level.getServer()`), пустой `PreventClicksMixin`.
Access widener / access transformer не используются — везде достаточно миксин-аксессоров.

Особенности:
* `PauseScreen.createPauseMenu()` в 1.21.1 не имеет `LinearLayout` с иконочными кнопками, поэтому кнопка
  конфига добавлена в сетку отдельной строкой на всю ширину (инъекция в `GridLayout.arrangeElements()`).
* `WorldSelectionList.WorldListEntry.summary` — package-private поле, берётся через `@Shadow`.
* `Screen.addRenderableWidget` protected → вызывается через `ScreenInvoker` из миксинов в чужом пакете.

---

## 6. Особенности и незакрытые риски переноса

1. **Порт не компилировался.** Правки вносились по декомпилированным исходникам Minecraft 1.21.1
   (Mojang-маппинги), исходникам OctoLib 0.6.2 и Architectury 13 — в среде без JDK, Gradle и доступа
   к maven-репозиториям. Взамен прогнаны статические проверки: 0 неразрешённых импортов во всём `common`;
   все цели `@Inject` / `@Redirect` / `@Accessor` / `@Invoker` / `@Shadow` в 30 миксинах существуют
   в исходниках 1.21.1; `scavenger.mixins.json` ↔ файлы миксинов совпадают 1:1; все id в тегах предметов
   валидны для 1.21.1; сверены сигнатуры `SavedData.Factory`, `GuiGraphics`, `Button.Builder`,
   `Packet`/`NetworkManager` OctoLib, `Level.tickRateManager()` и остальных использованных методов.
2. `ScavengerConfigScreen` — новый код (в OctoLib 0.6.2 экрана конфига нет): вёрстка в две колонки,
   страницы Timer / Wheels / Gameplay, «Сохранить» пишет YAML через `ConfigManager.uploadDataToConfig`,
   «Отменить» перечитывает файл через `ConfigManager.reload`.
3. Нуар: `assets/scavenger/shaders/post/noir.json` + `OctoLibPostEffects.getPostEffects().put(...)`
   (порядок инициализации OctoLib/Scavenger не гарантирован, поэтому эффект кладётся сразу в активную карту).
4. Блендинг подсветки колеса (`blitDodge`) переносился «в лоб» через `RenderSystem.blendFunc` —
   визуально может отличаться от 26.x.
5. История забегов: сид читается из `level.dat`, данные — из `<world>/data/scavenger_data.dat`
   (миры, созданные на 26.x, порт не подхватит).
6. `pack.mcmeta` → `pack_format: 34` (1.21.1).
7. Ключи локализации для нового экрана конфига добавлены в `en_us.json` и `ru_ru.json`
   (`uk_ua.json` возьмёт английский fallback).

---

## Юридическая часть

* **Оригинал — GPL-3.0-or-later.** Порт и его публикация разрешены без отдельного разрешения автора при
  соблюдении условий лицензии: приложить текст GPL-3.0, сохранить уведомления об авторстве
  (`catboybinary`), явно отметить, что это изменённая версия (с датой), распространять под той же
  лицензией и давать доступ к полному исходному коду (репозиторий или ссылка на него).
* **Modrinth** принимает GPL-моды; **CurseForge** строже к чужим проектам — стоит пометить проект как
  *unofficial port*, дать ссылку на оригинал и на свой репозиторий с исходниками, а в идеале получить
  письменное согласие автора.
* **Minecraft EULA**: мод распространяется бесплатно, без пейволов и платного доступа.
* **OctoLib / ShatterLib — All Rights Reserved.** Это только рантайм-зависимость: подключается из
  `maven.shatterbyte.com/releases` (или Modrinth/CurseForge), его код **нельзя** копировать в репозиторий
  или переупаковывать в jar. В `fabric.mod.json` / `neoforge.mods.toml` объявлена обязательная
  зависимость `octolib >= 0.6.0` и `architectury >= 13.0.8`.
