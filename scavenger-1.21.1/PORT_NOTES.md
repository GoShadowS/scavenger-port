# Porting notes: Scavenger 26.3 → 1.21.1

This document describes **what exactly was changed and how** while moving the mod from Minecraft 26.3 to 1.21.1.
Original: <https://github.com/catboybinary/scavenger> (branch `26.3`, GPL-3.0-or-later).
The port did not modify the original sources — they sit next to it in `../scavenger-26.3/`.

---

## 1. Target versions and project layout

```
scavenger-1.21.1/
├── build.gradle                 # architectury loom 1.13-SNAPSHOT, architectury-plugin 3.4-SNAPSHOT, shadow 8.1.1
├── gradle.properties            # Minecraft / loader / library versions
├── gradle/wrapper/*             # Gradle 8.12.1 (wrapper jar taken from architectury-api, branch 1.21)
├── common/                      # shared code + resources + scavenger.mixins.json
├── fabric/                      # Fabric entrypoint + fabric.mod.json
└── neoforge/                    # NeoForge entrypoint + META-INF/neoforge.mods.toml
```

Versions the port targets (in `gradle.properties`): `minecraft 1.21.1`, `architectury 13.0.8`,
`fabric-loader 0.16.14`, `fabric-api 0.116.6+1.21.1`, `neoforge 21.1.209`, `octolib 0.6.2+1.21`,
mappings `mojang + parchment 1.21.1:2024.11.17`, Java 21. The toolchain versions (loom `1.13-SNAPSHOT`,
architectury-plugin `3.4-SNAPSHOT`, shadow `8.1.1`, Gradle `8.12.1`) were taken from the original's `1.21.11`
branch and from the relics project — that is, from projects that really do build for 1.21.1.

Repositories the port pulls dependencies from: `maven.shatterbyte.com/releases` (OctoLib),
`maven.parchmentmc.org`, `maven.neoforged.net/releases`, `maven.fabricmc.net`, `maven.architectury.dev`.

---

## 2. Library swap: ShatterLib → OctoLib

| 26.x (ShatterLib 0.7.0-beta.1) | 1.21.1 (OctoLib 0.6.2) |
| --- | --- |
| `it.hurts.shatterbyte.shatterlib.*` | `it.hurts.octostudios.octolib.*` |
| `ShatterLibServices.platform().getConfigDirectory()` | not needed — `ConfigManager` creates and reads the file itself |
| `ConfigManager.register(id, config)` + `config.load(dir)` | `ConfigManager.registerConfig(id, config)` (creates the file and reloads it on its own) |
| `config.save()` | `ConfigManager.uploadDataToConfig(MOD_ID)` (`Scavenger.saveConfig()`) |
| `ShatterLibNetwork.registerS2CPayloadType(...)` | `NetworkManager.registerS2CPayloadType(...)` (dedicated server only) |
| `ShatterLibNetwork.registerS2CReceiver(type, codec, consumer)` | `NetworkManager.registerReceiver(Side.S2C, type, codec, (packet, ctx) -> ...)` |
| `ShatterLib.LOGGER` | `OctoLib.LOGGER` |
| `RenderPipelines` / coloured `blit` | `ScavengerRender.blitTinted` / `blitDodge` (see §3) |
| `ConfigScreen` (the library's built-in config screen) | a **custom** `ScavengerConfigScreen` (OctoLib 0.6.2 has no config screen) |

The config (`client/Config.java`) was rewritten for OctoLib's SnakeYAML serialization: it works **field by field**
(`FieldPropertyExt`), so fields must be plain YAML types — colours are stored as a `#AARRGGBB` string, the item
list as an `ArrayList<String>` of ids (`minecraft:diamond`), nested categories as static classes.
The `@Prop(comment = ...)` annotations were kept; `@Comment`/`@Range` from 26.x do not exist in OctoLib 0.6.2.

---

## 3. Main Minecraft API replacements (26.x → 1.21.1)

| Before | After |
| --- | --- |
| `Identifier` | `ResourceLocation` (+ `readResourceLocation`/`writeResourceLocation`) |
| `pushMatrix/popMatrix`, `extract*` | `pushPose/popPose`, `render*` |
| `guiGraphics.text/centeredText/item/itemDecorations/outline` | `drawString/drawCenteredString/renderItem/renderItemDecorations/renderOutline` |
| `guiGraphics.setTooltipForNextFrame(...)` | `guiGraphics.renderTooltip(...)` (in 1.21.1 the tooltip is drawn immediately) |
| `guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, ...[, color])` | `blit(tex, x, y, u, v, w, h, tw, th)`; colour via `RenderSystem.setShaderColor` (`ScavengerRender.blitTinted`) |
| a custom `RenderPipeline` with `BlendFunction(DST_COLOR, ONE)` | `RenderSystem.blendFunc(GL_DST_COLOR, GL_ONE)` around the blit (`ScavengerRender.blitDodge`) — in 1.21.1 `GuiGraphics.blit` draws immediately through `BufferUploader`, so the blend state does apply |
| `pose().translate(x, y)`, `scale(a, b)`, `rotate(rad)` | `translate(x, y, z)`, `scale(a, b, c)`, `mulPose(Axis.ZP.rotation(rad))` |
| `guiGraphics.verticalLine(...)` | `guiGraphics.vLine(...)` |
| `ItemStack.getStyledHoverName()` | `ScavengerClient.styledHoverName(stack)` = `Component.empty().append(stack.getHoverName()).withStyle(stack.getRarity().color())` |
| `component.withColor(int)` | `component.withStyle(style -> style.withColor(int))` |
| `BuiltInRegistries.ITEM.getValue(id)` | `get(id)` / `getOptional(id).orElse(Items.AIR)` |
| `ResourceKey.identifier()` | `ResourceKey.location()` |
| `mouseClicked(MouseButtonEvent, boolean)`, `mouseDragged(MouseButtonEvent, dx, dy)`, `mouseReleased(MouseButtonEvent)`, `keyPressed(KeyEvent)` | `mouseClicked(double, double, int)`, `mouseDragged(double, double, int, double, double)`, `mouseReleased(double, double, int)`, `keyPressed(int, int, int)` |
| `AbstractWidget.onClick(MouseButtonEvent, boolean)` | `onClick(double, double)` |
| `minecraft.gui.setScreen(...)` / `gui.screen()` | `minecraft.setScreen(...)` / `minecraft.screen` |
| `Minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE)` | no such method → `exitToTitleScreen()` the way `DeathScreen` does it: `level.disconnect(); disconnect(new GenericMessageScreen(translatable("menu.savingLevel"))); setScreen(new TitleScreen())` |
| `CommonComponents.disconnectButtonLabel(local)` | `isLocalServer() ? translatable("menu.returnToMenu") : CommonComponents.GUI_DISCONNECT` |
| `Minecraft.gui.toastManager()` + `ToastManager.visible` | `Minecraft.getToasts()` + `ToastComponent.visible` (`ToastComponentAccessor`, `ToastInstanceAccessor`) |
| `player.getActivePostEffects().add(Identifier)` (noir) | `OctoLibPostEffects` — a `NoirPostEffect extends PostEffect` class (`RenderStage.LEVEL`, `shouldRender() = ClientScavengerData.is(NOIR)`); inserted straight into `OctoLibPostEffects.getPostEffects()` so it does not depend on mod initialization order |
| `assets/scavenger/post_effect/noir.json` (26.x format) | `assets/scavenger/shaders/post/noir.json` (1.21.1 format: `targets`/`passes`, programs `color_convolve` + `blit`) |
| `level.clockManager().moveToTimeMarker(OVERWORLD, MIDNIGHT/NOON)` | `ServerLevel.setDayTime(18000 / 6000)` |
| `GameRules.ADVANCE_TIME` + `gameRules.set(key, value, server)` | `GameRules.RULE_DAYLIGHT` + `gameRules.getRule(key).set(false, level.getServer())` |
| `player.hurtServer(level, source, amount)` | `player.hurt(source, amount)` |
| `player.drop(stack, false, Prediction.SERVER_ONLY)` | `player.drop(stack, false)` |
| `EnderDragonFight` | `EndDragonFight` (+ `ServerLevel.getDragonFight()`, `hasPreviouslyKilledDragon()`) |
| a `ServerLevel` accessor for `getServer()` | `Level.getServer()` (nullable) / `ServerPlayer.serverLevel()` |
| `SavedDataType` + Codec-based saving | `SavedData.Factory<>(new, (tag, provider) -> load(tag), DataFixTypes.LEVEL)` and hand-written NBT (`save(CompoundTag, HolderLookup.Provider)`) |
| `NbtAccounter.uncompressedQuota()` | `NbtAccounter.unlimitedHeap()` |
| `tag.getCompound("data").orElse(root)`, `tag.getLong("seed")` → `Optional` | `contains(...)` + plain `getCompound/getLong` |
| 26.x `LevelStorageSource` layout (`minecraft/world_gen_settings.dat`, `data/<modid>/`) | `<world>/data/scavenger_data.dat`, the seed from `<world>/level.dat` → `Data/WorldGenSettings/seed` (`WorldSeedReader`) |
| `MouseHandler.getScaledXPos(getWindow())` | the `mouseX/mouseY` parameters of `render(...)` |
| `AbstractSelectionList.Entry.render` (short signature) | 10 parameters `(GuiGraphics, index, top, left, width, height, mouseX, mouseY, hovered, partialTick)` |
| `SoundManager.play(...) -> SoundEngine.PlayResult` | `play(SoundInstance)` returns `void` → `CallbackInfo` + `ci.cancel()` |
| `ItemStackTemplate`, `Consumable`, `BedRule`, `isMobilityRestricted`, `util.Prediction` | do not exist — see §4 |
| `catch (Throwable _)` (unnamed variables) | `catch (Throwable ignored)` — a preview feature on Java 21 |

The HUD and the client tick are no longer split per loader: the common code uses Architectury events —
`ClientGuiEvent.RENDER_HUD` and `ClientTickEvent.CLIENT_POST` (`ScavengerClient.init()`), and on the server side
`PlayerEvent.PLAYER_JOIN`, `TickEvent.PLAYER_POST`, `LifecycleEvent.SERVER_LEVEL_LOAD` (`Scavenger.init()`).

---

## 4. Modifiers: how they are implemented in 1.21.1

| Modifier | 26.x | 1.21.1 |
| --- | --- | --- |
| GIANT / TINY / ALIEN | `Attributes.SCALE` / `GRAVITY` | unchanged (1.21.1 has the same attributes) |
| TURTLE / SONIC | redirect of `OptionInstance.get()` in `MouseHandler.turnPlayer` | the same (ordinal 0 — sensitivity) |
| DRUNK | inverts X and Y through the options | 1.21.1 has no "invert mouse X" → redirect of `invertYMouse().get()` (ordinal 1) plus inverting `accumulatedDX` at HEAD of `turnPlayer` (vanilla zeroes the field right after) |
| VEGETARIAN / CARNIVORE | `Consumable.canConsume` | HEAD of `Item.use(Level, Player, InteractionHand)` → `InteractionResultHolder.pass(stack)`; tags `scavenger:vegetarian_food`, `minecraft:meat`, `minecraft:fishes` |
| SNAIL | `Player.isMobilityRestricted` | HEAD of `LivingEntity.setSprinting(true)` → cancel, guarded by `instanceof Player` (`setSprinting` is declared by `LivingEntity`, not `Player`) |
| INSOMNIA | `Player.startSleepInBed(bed, state, BedRule, pos)` + its own `BedSleepingProblem` | `startSleepInBed(BlockPos)`; `BedSleepingProblem` is an enum in 1.21.1, so `OTHER_PROBLEM` is used and the `scavenger.insomnia` text is shown in the action bar |
| SPEED_UP | `level.tickRateManager().setTickRate(40)` | unchanged |
| ECLIPSE / SOLSTICE | `clockManager().moveToTimeMarker(...)` | `setDayTime(18000)` / `setDayTime(6000)` + disabling `doDaylightCycle` |
| HYDROPHOBIC | `hurtServer` | `hurt` |
| FEARFUL | `hurtServer` TAIL | `Player.hurt` TAIL |
| ONE_ARM | `Inventory.SLOT_OFFHAND` + `drop(..., Prediction.SERVER_ONLY)` | `Inventory.SLOT_OFFHAND` (exists in 1.21.1) + `drop(stack, false)` |
| NOIR | `player.getActivePostEffects()` | `NoirPostEffect` through OctoLib (see §3) |
| TOURIST | `LanguageManager` | unchanged (the API matches) |
| ASOCIAL / MAIN_CHARACTER / NPC / UNEDUCATED / HOLEY_POCKETS / BRITTLE_BONES / SHUFFLED_CHESTS / SILENCE / MOLE / FINALIST / DEJAVU / LARGE_BIOMES / AMPLIFIED / TWICE / THRICE / NONE | — | ported 1:1 (API renames only) |

---

## 5. Mixins (`common/src/main/resources/scavenger.mixins.json`)

`compatibilityLevel: JAVA_21`. Client: `CreateWorldScreenAccessor/Mixin`, `DeathScreenInvoker/Mixin`,
`ImageButtonAccessor`, `MinecraftMixin` (`disconnect(Screen, boolean)` — the point where client data is reset),
`OptionsSubScreenAccessor`, `PauseScreenMixin`, `ScreenInvoker`, `TitleScreenMixin`, `ToastComponentAccessor`,
`ToastInstanceAccessor`, `WorldListEntryMixin`, `modifier.{CameraType, ContainerScreen, Food, MouseHandler,
MouseSettingsScreen, Options, RemoveRecipeBook, Screen, SoundEngine}Mixin`.
Common: `modifier.{CakeBlock, Fearful, Insomnia, LivingEntity, OnlyHotbar, ShuffledChests, Slot,
VillagerLevel, Villager}Mixin`.

Removed: `ServerLevelAccessor` (1.21.1 has `Level.getServer()`) and the empty `PreventClicksMixin`.
No access widener / access transformer is used — mixin accessors cover everything.

Details:
* `PauseScreen.createPauseMenu()` in 1.21.1 has no `LinearLayout` with icon buttons, so the config button is
  added to the grid as its own full-width row (injection into `GridLayout.arrangeElements()`).
* `WorldSelectionList.WorldListEntry.summary` is a package-private field, obtained through `@Shadow`.
* `Screen.addRenderableWidget` is protected → called through `ScreenInvoker` from mixins in other packages.
* An injection must target the class that **declares** the method: the old `PreventSprintingMixin` injected
  `setSprinting` into `@Mixin(Player.class)`, but that method is declared by `LivingEntity`, so the refmap
  remapper reported `Cannot remap setSprinting ... does not exists in any of the targets [Player]` and the
  production (intermediary) jar would have lost the mapping. SNAIL now injects into `LivingEntityMixin` with
  an `instanceof Player` guard, and the remaining INSOMNIA injection lives in `InsomniaMixin`
  (`startSleepInBed` is declared by `Player`).

---

## 6. Known differences from 26.3

* **The config screen is written from scratch.** OctoLib 0.6.2 has no ready-made config screen, so the port has
  its own: three pages (Timer, Wheels, Gameplay), options in two columns, numbers and colours typed as text,
  toggles and lists as cycle buttons. "Save" rewrites `config/scavenger.yaml`, "Discard" re-reads it from disk.
  The config button is available in the pause menu and on the title screen.
* **Noir** (`NOIR`) is hooked up through OctoLib's post effects instead of the player's active effect list: the
  shader moved from `post_effect/noir.json` to `assets/scavenger/shaders/post/noir.json` and was rewritten in the
  1.21.1 format (programs `color_convolve` + `blit`, the same 0.3 / 0.59 / 0.11 matrices).
* **The wheel highlight** is drawn with plain blending (`GL_DST_COLOR`, `GL_ONE`) instead of 26.x render
  pipelines — the tint may differ slightly from the original.
* **Run history** lives in `<world>/data/scavenger_data.dat`, and the world seed is read from `level.dat`.
  Worlds created on 26.x are not picked up: history and the victory state start over.
* **Resources**: `pack.mcmeta` → `pack_format: 34`; items that do not exist in 1.21.1 were dropped from the
  `scavenger:unrollable_by_default` and `scavenger:vegetarian_food` tags
  (`unrollable_by_default`: 123 → 113 ids).
* **Localization**: keys for the new config screen were added to `en_us.json` and `ru_ru.json`;
  `uk_ua.json` falls back to English for them.
* **Mixins**: `ServerLevelAccessor` and the empty `PreventClicksMixin` were removed, and no access widener or
  access transformer is used at all — everything is covered by mixin accessors (`ScreenInvoker`,
  `DeathScreenInvoker`, `CreateWorldScreenAccessor`, `ImageButtonAccessor`, `OptionsSubScreenAccessor`,
  `ToastComponentAccessor`, `ToastInstanceAccessor`).
* **The config button in the pause menu** takes a full-width row of its own in the grid: `PauseScreen` in 1.21.1
  has no row of icon buttons like 26.x does.
