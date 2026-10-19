package meow.binary.scavenger.client;

import it.hurts.octostudios.octolib.module.config.annotation.Prop;
import it.hurts.octostudios.octolib.module.config.impl.ConfigSide;
import it.hurts.octostudios.octolib.module.config.impl.OctoConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Scavenger's (client side) configuration.
 * <p>
 * Serialized by OctoLib's {@link it.hurts.octostudios.octolib.module.config.ConfigManager} into a YAML file
 * inside the instance's {@code config} folder. Everything is stored using plain, YAML friendly types:
 * colors are {@code #AARRGGBB} strings and the item list holds registry ids such as {@code minecraft:diamond}.
 */
public class Config implements OctoConfig {
    public TimerCategory timer = new TimerCategory();
    public WheelsCategory wheels = new WheelsCategory();
    public GameplayCategory gameplay = new GameplayCategory();
    public MiscCategory misc = new MiscCategory();

    @Override
    public ConfigSide getSide() {
        return ConfigSide.CLIENT;
    }

    public int getVictoryAccentColorArgb() {
        return parseHexColor(timer.colorFinished, DEFAULT_COLOR_FINISHED);
    }

    public int getTimerDefaultColorArgb() {
        return parseHexColor(timer.colorDefault, DEFAULT_COLOR_DEFAULT);
    }

    public static final int DEFAULT_COLOR_FINISHED = 0xFF11D0F0;
    public static final int DEFAULT_COLOR_DEFAULT = 0xFF16F464;

    public static int parseHexColor(String value, int fallback) {
        if (value == null) {
            return fallback;
        }

        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        if (normalized.length() == 6) {
            normalized = "FF" + normalized;
        }

        if (normalized.length() != 8) {
            return fallback;
        }

        try {
            return (int) Long.parseUnsignedLong(normalized, 16);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static String toHexColor(int argb) {
        return String.format("#%08X", argb);
    }

    public static class TimerCategory {
        @Prop(comment = "Opacity of the timer background (0.0 - 1.0)")
        public float backgroundOpacity = 0.6f;

        @Prop(comment = "Anchor point for the timer. Possible values: TOP_LEFT, TOP_CENTER, TOP_RIGHT,"
                + " CENTER_LEFT, CENTER, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT")
        public AnchorPoint anchorPoint = AnchorPoint.TOP_CENTER;

        public int xOffset = 0;
        public int yOffset = 0;

        @Prop(comment = "Moves the timer out of the way whenever toasts are on screen. (Only works if the anchor point is TOP_RIGHT)")
        public boolean moveTimerUnderToasts = true;

        public boolean showMs = true;
        public boolean moveItemLeft = false;
        public int sidePadding = 4;

        @Prop(comment = "Color used for the victory accent and winning timer text. Format: #AARRGGBB")
        public String colorFinished = toHexColor(DEFAULT_COLOR_FINISHED);

        @Prop(comment = "Color used for the regular HUD timer text. Format: #AARRGGBB")
        public String colorDefault = toHexColor(DEFAULT_COLOR_DEFAULT);

        public boolean outlineColorMatch = false;
        public boolean showModifierText = true;
        public boolean showItemName = true;

        @Prop(comment = "Text alignment for the modifier and item name below the timer. INHERIT (follows anchor point), LEFT, CENTER, or RIGHT")
        public TextAlignment textAlignment = TextAlignment.INHERIT;
    }

    public static class WheelsCategory {
        public float itemRollTime = 4.5f;
        public float modifierRollTime = 1.45f;
        public float scaleItemWheel = 1f;
        public float scaleModifierWheel = 1f;

        @Prop(comment = "Removes the item reveal animation at the item wheel screen")
        public boolean removeItemReveal = false;

        @Prop(comment = "Allows spinning the item and modifier wheels by clicking anywhere on the screen instead of only the wheel widget")
        public boolean clickAnywhereToSpin = false;
    }

    public static class GameplayCategory {
        @Prop(comment = "Item ids (for example 'minecraft:diamond') used to limit random item rolls")
        public ArrayList<String> rollableItems = new ArrayList<>();

        @Prop(comment = "If true, rollableItems is a blacklist. If false, rollableItems is a whitelist")
        public boolean rollableItemsIsBlacklist = true;

        @Prop(comment = "Modifier ids that should never be rolled (for example 'scavenger:noir')")
        public ArrayList<String> modifierBlacklist = new ArrayList<>(List.of("scavenger:none"));

        @Prop(comment = "Removes items from the pool after winning by adding or removing them from the rollableItems list")
        public boolean removeItemAfterWin = true;

        @Prop(comment = "Skips the modifier wheel and creates the world immediately with no modifier")
        public boolean skipModifierWheel = false;
    }

    public static class MiscCategory {
        @Prop(comment = "Makes the Victory! title static instead of bobbing")
        public boolean staticVictoryTitle = false;

        @Prop(comment = "Moves the buttons on the world create screen horizontally")
        public int menuButtonsXOffset = 0;
    }
}
