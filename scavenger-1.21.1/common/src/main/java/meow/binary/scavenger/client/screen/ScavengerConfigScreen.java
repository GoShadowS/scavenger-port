package meow.binary.scavenger.client.screen;

import it.hurts.octostudios.octolib.module.config.ConfigManager;
import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.client.AnchorPoint;
import meow.binary.scavenger.client.Config;
import meow.binary.scavenger.client.TextAlignment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Small in game config screen.
 * <p>
 * OctoLib 0.6.2 does not ship a config screen (the 26.x version relied on ShatterLib's
 * {@code ConfigScreen}), so this screen edits {@link Config} directly and writes the result back to
 * {@code config/scavenger.yml} through {@link ConfigManager#uploadDataToConfig(String)}.
 * Options are grouped into pages because they do not fit on a single screen.
 */
public class ScavengerConfigScreen extends Screen {
    private static final int COLUMN_WIDTH = 200;
    private static final int COLUMN_GAP = 8;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_STEP = 24;
    private static final int TOP = 48;
    private static final int LABEL_WIDTH = 96;

    public enum Page {
        TIMER,
        WHEELS,
        GAMEPLAY;

        public Component label() {
            return Component.translatable("scavenger.config.page." + this.name().toLowerCase(Locale.ROOT));
        }
    }

    private final Screen parent;
    private Page page = Page.TIMER;

    public ScavengerConfigScreen(Screen parent) {
        super(Component.translatable("scavenger.config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.rowLabels.clear();

        this.addRenderableWidget(CycleButton.builder(Page::label, this.page)
                .withValues(List.of(Page.values()))
                .displayOnlyValue()
                .create(centerX - 100, 20, 200, ROW_HEIGHT, Component.translatable("scavenger.config.page"), (button, value) -> {
                    this.page = value;
                    this.rebuildWidgets();
                }));

        List<RowFactory> rows = switch (this.page) {
            case TIMER -> this.timerRows();
            case WHEELS -> this.wheelRows();
            case GAMEPLAY -> this.gameplayRows();
        };

        int perColumn = (rows.size() + 1) / 2;
        for (int i = 0; i < rows.size(); i++) {
            int column = i < perColumn ? 0 : 1;
            int row = column == 0 ? i : i - perColumn;
            int x = centerX - COLUMN_WIDTH - COLUMN_GAP / 2 + column * (COLUMN_WIDTH + COLUMN_GAP);
            int y = TOP + row * ROW_STEP;
            rows.get(i).add(x, y, COLUMN_WIDTH, ROW_HEIGHT);
        }

        int buttonsY = this.height - 26;
        this.addRenderableWidget(Button.builder(Component.translatable("scavenger.config.discard"), button -> {
            ConfigManager.reload(Scavenger.MOD_ID);
            this.minecraft.setScreen(this.parent);
        }).bounds(centerX - 102, buttonsY, 100, ROW_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.translatable("scavenger.config.save"), button -> {
            Scavenger.saveConfig();
            this.minecraft.setScreen(this.parent);
        }).bounds(centerX + 2, buttonsY, 100, ROW_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (RowLabel rowLabel : this.rowLabels) {
            guiGraphics.drawString(this.font, rowLabel.label(), rowLabel.x(), rowLabel.y(), 0xffdddddd, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, Component.translatable("scavenger.config.lists_hint"),
                this.width / 2, this.height - 40, 0xff999999);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private List<RowFactory> timerRows() {
        Config.TimerCategory timer = Scavenger.CONFIG.timer;
        List<RowFactory> rows = new ArrayList<>();
        rows.add(this.enumRow("scavenger.config.timer.anchor_point", timer.anchorPoint, value -> timer.anchorPoint = value, AnchorPoint.values()));
        rows.add(this.enumRow("scavenger.config.timer.text_alignment", timer.textAlignment, value -> timer.textAlignment = value, TextAlignment.values()));
        rows.add(this.booleanRow("scavenger.config.timer.show_ms", () -> timer.showMs, value -> timer.showMs = value));
        rows.add(this.booleanRow("scavenger.config.timer.show_modifier_text", () -> timer.showModifierText, value -> timer.showModifierText = value));
        rows.add(this.booleanRow("scavenger.config.timer.show_item_name", () -> timer.showItemName, value -> timer.showItemName = value));
        rows.add(this.booleanRow("scavenger.config.timer.move_item_left", () -> timer.moveItemLeft, value -> timer.moveItemLeft = value));
        rows.add(this.booleanRow("scavenger.config.timer.move_timer_under_toasts", () -> timer.moveTimerUnderToasts, value -> timer.moveTimerUnderToasts = value));
        rows.add(this.booleanRow("scavenger.config.timer.outline_color_match", () -> timer.outlineColorMatch, value -> timer.outlineColorMatch = value));
        rows.add(this.floatRow("scavenger.config.timer.background_opacity", () -> timer.backgroundOpacity, value -> timer.backgroundOpacity = value));
        rows.add(this.intRow("scavenger.config.timer.side_padding", () -> timer.sidePadding, value -> timer.sidePadding = value));
        rows.add(this.intRow("scavenger.config.timer.x_offset", () -> timer.xOffset, value -> timer.xOffset = value));
        rows.add(this.intRow("scavenger.config.timer.y_offset", () -> timer.yOffset, value -> timer.yOffset = value));
        rows.add(this.colorRow("scavenger.config.timer.color_finished", () -> timer.colorFinished, value -> timer.colorFinished = value));
        rows.add(this.colorRow("scavenger.config.timer.color_default", () -> timer.colorDefault, value -> timer.colorDefault = value));
        return rows;
    }

    private List<RowFactory> wheelRows() {
        Config.WheelsCategory wheels = Scavenger.CONFIG.wheels;
        List<RowFactory> rows = new ArrayList<>();
        rows.add(this.floatRow("scavenger.config.wheels.item_roll_time", () -> wheels.itemRollTime, value -> wheels.itemRollTime = value));
        rows.add(this.floatRow("scavenger.config.wheels.modifier_roll_time", () -> wheels.modifierRollTime, value -> wheels.modifierRollTime = value));
        rows.add(this.floatRow("scavenger.config.wheels.scale_item_wheel", () -> wheels.scaleItemWheel, value -> wheels.scaleItemWheel = value));
        rows.add(this.floatRow("scavenger.config.wheels.scale_modifier_wheel", () -> wheels.scaleModifierWheel, value -> wheels.scaleModifierWheel = value));
        rows.add(this.booleanRow("scavenger.config.wheels.remove_item_reveal", () -> wheels.removeItemReveal, value -> wheels.removeItemReveal = value));
        rows.add(this.booleanRow("scavenger.config.wheels.click_anywhere_to_spin", () -> wheels.clickAnywhereToSpin, value -> wheels.clickAnywhereToSpin = value));
        return rows;
    }

    private List<RowFactory> gameplayRows() {
        Config.GameplayCategory gameplay = Scavenger.CONFIG.gameplay;
        Config.MiscCategory misc = Scavenger.CONFIG.misc;
        List<RowFactory> rows = new ArrayList<>();
        rows.add(this.booleanRow("scavenger.config.gameplay.rollable_items_is_blacklist", () -> gameplay.rollableItemsIsBlacklist, value -> gameplay.rollableItemsIsBlacklist = value));
        rows.add(this.booleanRow("scavenger.config.gameplay.remove_item_after_win", () -> gameplay.removeItemAfterWin, value -> gameplay.removeItemAfterWin = value));
        rows.add(this.booleanRow("scavenger.config.gameplay.skip_modifier_wheel", () -> gameplay.skipModifierWheel, value -> gameplay.skipModifierWheel = value));
        rows.add(this.booleanRow("scavenger.config.misc.static_victory_title", () -> misc.staticVictoryTitle, value -> misc.staticVictoryTitle = value));
        rows.add(this.intRow("scavenger.config.misc.menu_buttons_x_offset", () -> misc.menuButtonsXOffset, value -> misc.menuButtonsXOffset = value));
        return rows;
    }

    private RowFactory booleanRow(String key, BooleanSupplier get, Consumer<Boolean> set) {
        return (x, y, width, height) -> this.addRenderableWidget(CycleButton.onOffBuilder(get.getAsBoolean())
                .create(x, y, width, height, Component.translatable(key), (button, value) -> set.accept(value)));
    }

    private <E extends Enum<E>> RowFactory enumRow(String key, E value, Consumer<E> set, E[] values) {
        return (x, y, width, height) -> this.addRenderableWidget(CycleButton.builder(ScavengerConfigScreen::enumLabel, value)
                .withValues(List.of(values))
                .displayOnlyValue()
                .create(x, y, width, height, Component.translatable(key), (button, newValue) -> set.accept(newValue)));
    }

    private RowFactory intRow(String key, IntSupplier get, IntConsumer set) {
        return this.textRow(key, String.valueOf(get.getAsInt()), value -> {
            try {
                set.accept(Integer.parseInt(value.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
    }

    private RowFactory floatRow(String key, FloatSupplier get, Consumer<Float> set) {
        return this.textRow(key, String.valueOf(get.getAsFloat()), value -> {
            try {
                set.accept(Float.parseFloat(value.trim().replace(',', '.')));
            } catch (NumberFormatException ignored) {
            }
        });
    }

    private RowFactory colorRow(String key, Supplier<String> get, Consumer<String> set) {
        return this.textRow(key, get.get(), value -> set.accept(value.trim()));
    }

    private RowFactory textRow(String key, String initial, Consumer<String> responder) {
        return (x, y, width, height) -> {
            this.drawRowLabel(Component.translatable(key), x, y);

            EditBox box = new EditBox(this.font, x + LABEL_WIDTH, y, width - LABEL_WIDTH, height, Component.translatable(key));
            box.setValue(initial);
            box.setResponder(responder);
            this.addRenderableWidget(box);
        };
    }

    private final List<RowLabel> rowLabels = new ArrayList<>();

    private void drawRowLabel(Component label, int x, int y) {
        this.rowLabels.add(new RowLabel(label, x, y + 6));
    }

    private record RowLabel(Component label, int x, int y) {
    }

    private static Component enumLabel(Enum<?> value) {
        String[] words = value.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.literal(builder.toString());
    }

    @FunctionalInterface
    private interface RowFactory {
        void add(int x, int y, int width, int height);
    }

    @FunctionalInterface
    private interface FloatSupplier {
        float getAsFloat();
    }
}
