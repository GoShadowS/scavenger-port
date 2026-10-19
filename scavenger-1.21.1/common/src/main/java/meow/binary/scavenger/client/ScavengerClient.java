package meow.binary.scavenger.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import it.hurts.octostudios.octolib.module.post_effect.init.OctoLibPostEffects;
import it.hurts.octostudios.octolib.util.OctoColor;
import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.client.post.NoirPostEffect;
import meow.binary.scavenger.client.screen.VictoryScreen;
import meow.binary.scavenger.mixin.ToastComponentAccessor;
import meow.binary.scavenger.mixin.ToastInstanceAccessor;
import meow.binary.scavenger.network.SyncRunRecordPacket;
import meow.binary.scavenger.network.SyncScavengerDataPacket;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static meow.binary.scavenger.Scavenger.CONFIG;

public final class ScavengerClient {
    private static final Random TOURIST_RANDOM = new Random();
    private static boolean touristLanguageApplied;
    private static String previousTouristLanguage;
    private static boolean pendingTouristLanguageRestore;

    public static void init() {
        // OctoLib copies its post effect registry into the active map during OctoLibClient#init,
        // which may already have happened by the time we get here, so the effect is added to the
        // live map directly to stay independent of the mod initialisation order.
        NoirPostEffect noir = new NoirPostEffect();
        OctoLibPostEffects.getPostEffects().put(noir.getPath(), noir);

        ClientGuiEvent.RENDER_HUD.register(ScavengerClient::renderHudInfo);

        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            Level level = minecraft.level;
            if (level != null) {
                onClientTick(level);
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncScavengerDataPacket.TYPE, SyncScavengerDataPacket.STREAM_CODEC,
                (packet, context) -> context.queue(() -> handleScavengerData(packet)));

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncRunRecordPacket.TYPE, SyncRunRecordPacket.STREAM_CODEC,
                (packet, context) -> context.queue(() -> handleRunRecord(packet)));
    }

    private static void handleScavengerData(SyncScavengerDataPacket packet) {
        ClientScavengerData.item = packet.getItem();
        ClientScavengerData.modifier = packet.getModifier();
        ClientScavengerData.winTimestamp = packet.getWinTimestamp();

        if (!packet.isWin) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                enforceClientModifiers(minecraft.level);
            }
            return;
        }

        Minecraft.getInstance().setScreen(new VictoryScreen());

        if (!CONFIG.gameplay.removeItemAfterWin) {
            return;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(ClientScavengerData.item).toString();

        if (CONFIG.gameplay.rollableItemsIsBlacklist && !CONFIG.gameplay.rollableItems.contains(itemId)) {
            CONFIG.gameplay.rollableItems.add(itemId);
            Scavenger.saveConfig();
            return;
        }

        if (!CONFIG.gameplay.rollableItemsIsBlacklist) {
            CONFIG.gameplay.rollableItems.remove(itemId);
            Scavenger.saveConfig();
        }
    }

    private static void handleRunRecord(SyncRunRecordPacket packet) {
        RunRecord record = RunRecord.create(packet.levelId, packet.itemId, packet.modifierId, packet.winTimestamp, packet.seed, packet.multiplayer);
        RunHistory.addRemoteRecord(record);
    }

    public static void onClientTick(Level level) {
        if (!level.isClientSide()) return;
        ScavengerClient.enforceClientModifiers(level);
    }

    public static boolean enforceClientModifiers(Level level) {
        if (ClientScavengerData.isEmpty()) {
            restoreTouristLanguage();
            touristLanguageApplied = false;
            return false;
        }

        enforceTouristLanguage();

        if (Modifiers.isActive(Modifiers.MAIN_CHARACTER, level)) {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            return true;
        }

        if (Modifiers.isActive(Modifiers.NPC, level)) {
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
            return true;
        }

        return false;
    }

    private static void enforceTouristLanguage() {
        if (!ClientScavengerData.is(Modifiers.TOURIST)) {
            touristLanguageApplied = false;
            return;
        }

        if (touristLanguageApplied) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LanguageManager languageManager = minecraft.getLanguageManager();
        if (previousTouristLanguage == null) {
            previousTouristLanguage = languageManager.getSelected();
        }
        List<String> languages = new ArrayList<>(languageManager.getLanguages().keySet());
        if (languages.isEmpty()) {
            return;
        }

        String currentLanguage = languageManager.getSelected();
        if (languages.size() > 1) {
            languages.remove(currentLanguage);
        }

        if (languages.isEmpty()) {
            return;
        }

        String randomLanguage = languages.get(TOURIST_RANDOM.nextInt(languages.size()));
        languageManager.setSelected(randomLanguage);
        minecraft.options.languageCode = randomLanguage;
        minecraft.options.save();
        minecraft.reloadResourcePacks();
        touristLanguageApplied = true;
    }

    private static void restoreTouristLanguage() {
        if (previousTouristLanguage == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LanguageManager languageManager = minecraft.getLanguageManager();
        if (languageManager.getLanguage(previousTouristLanguage) == null) {
            previousTouristLanguage = null;
            return;
        }

        if (!previousTouristLanguage.equals(languageManager.getSelected())) {
            languageManager.setSelected(previousTouristLanguage);
            minecraft.options.languageCode = previousTouristLanguage;
            minecraft.options.save();
            minecraft.reloadResourcePacks();
        }

        previousTouristLanguage = null;
    }

    public static void onClientDisconnect() {
        pendingTouristLanguageRestore = previousTouristLanguage != null;
        touristLanguageApplied = false;
        ClientScavengerData.clear();
    }

    public static void onTitleScreenShown() {
        if (!pendingTouristLanguageRestore) {
            return;
        }

        pendingTouristLanguageRestore = false;
        restoreTouristLanguage();
    }

    public static void renderHudInfo(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (ClientScavengerData.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Player player = mc.player;
        Level level = mc.level;

        if (player == null || level == null) {
            return;
        }

        boolean won = ClientScavengerData.winTimestamp > 0;

        float tickrate = level.tickRateManager().tickrate();
        double ticks = level.getGameTime();
        if (won) {
            ticks = ClientScavengerData.winTimestamp;
        }

        double totalSeconds = ticks / tickrate;

        OctoColor bgColor = new OctoColor(0, 0, 0, CONFIG.timer.backgroundOpacity);

        int itemCount = Scavenger.getItemCount(ClientScavengerData.modifier);

        AnchorPoint anchor = CONFIG.timer.anchorPoint;
        int configX = CONFIG.timer.xOffset;
        int configY = CONFIG.timer.yOffset;

        if (anchor.equals(AnchorPoint.TOP_RIGHT) && CONFIG.timer.moveTimerUnderToasts) {
            List<?> toasts = ((ToastComponentAccessor) mc.getToasts()).scavenger$getVisibleToasts();
            if (toasts != null && !toasts.isEmpty()) {
                for (Object obj : toasts) {
                    configY += ((ToastInstanceAccessor) obj).scavenger$getToast().height();
                }
            }
        }

        int padding = CONFIG.timer.sidePadding + 4;

        int hours = (int)(totalSeconds / 3600);
        int minutes = (int)((totalSeconds % 3600) / 60);
        int seconds = (int)(totalSeconds % 60);
        int millis = (int)((totalSeconds - Math.floor(totalSeconds)) * 100);

        String time = String.format("%d:%02d:%02d", hours, minutes, seconds);
        String ms = CONFIG.timer.showMs ? String.format(".%02d", millis) : "";

        int noMillisWidth = font.width(time) * 2;
        int millisWidth = font.width(ms);
        int timeWidth = noMillisWidth + millisWidth;

        boolean itemLeft = CONFIG.timer.moveItemLeft;

        int width = timeWidth + 6 + 16;
        int height = 16;

        int screenW = guiGraphics.guiWidth() - padding * 2;
        int screenH = guiGraphics.guiHeight() - padding * 2;

        float pivotX = screenW * anchor.xFactor;
        float pivotY = screenH * anchor.yFactor;

        float offsetX = -width * anchor.xFactor;
        float offsetY = -height * anchor.yFactor;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(padding, padding, 0);
        guiGraphics.pose().translate(pivotX + offsetX + configX, pivotY + offsetY + configY, 0);

        guiGraphics.fill(-4, -4, width + 4, height + 4, bgColor.getARGB());

        int timeX = itemLeft ? (width - timeWidth) : 1;
        OctoColor timerColor = won
                ? new OctoColor(CONFIG.getVictoryAccentColorArgb())
                : new OctoColor(CONFIG.getTimerDefaultColorArgb());
        renderTimerText(guiGraphics, font, totalSeconds, timeX, 1, CONFIG.timer.showMs, timerColor);
        int itemX = itemLeft ? 0 : timeX + timeWidth + 5;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(itemX, 0, 0);
        ItemStack stack = new ItemStack(ClientScavengerData.item, itemCount);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.renderItemDecorations(font, stack, 0, 0);
        guiGraphics.pose().popPose();

        guiGraphics.vLine(itemLeft ? 18 : itemX - 3, -3, height + 2, CONFIG.timer.outlineColorMatch ? timerColor.getARGB() : 0xffffffff);

        guiGraphics.renderOutline(-3, -3, width + 6, height + 6, CONFIG.timer.outlineColorMatch ? timerColor.getARGB() : 0xffffffff);

        int textY;
        if (anchor.yFactor > 0.5f) {
            textY = -4 - 2;
        } else {
            textY = height + 4 + 2;
        }

        if (CONFIG.timer.showModifierText && !ClientScavengerData.modifier.equals(Modifiers.NONE.getId())) {
            Component modifierText = Component.translatable("scavenger.victory.modifier_label")
                    .withStyle(style -> style.withColor(0xffdddddd))
                    .append(" ")
                    .append(Modifiers.getName(ClientScavengerData.modifier).withStyle(ChatFormatting.WHITE));
            float alignFactor = CONFIG.timer.textAlignment == TextAlignment.INHERIT
                    ? anchor.xFactor
                    : CONFIG.timer.textAlignment.xFactor;
            int textX;
            if (alignFactor == 0f) {
                textX = -4;
            } else if (alignFactor == 1f) {
                textX = width + 4 - font.width(modifierText);
            } else {
                textX = (width) / 2 - font.width(modifierText) / 2;
            }

            if (anchor.yFactor > 0.5f) {
                textY -= font.lineHeight;
            }

            guiGraphics.drawString(font, modifierText, textX, textY, 0xFFFFFFFF, true);

            if (!(anchor.yFactor > 0.5f)) {
                textY += font.lineHeight + 1;
            } else {
                textY -= 1;
            }
        }

        if (CONFIG.timer.showItemName) {
            ItemStack nameStack = ClientScavengerData.item.getDefaultInstance();
            Component itemComponent = Component.translatable("scavenger.victory.item_label")
                    .withStyle(style -> style.withColor(0xffdddddd))
                    .append(" ")
                    .append(styledHoverName(nameStack));
            float alignFactor = CONFIG.timer.textAlignment == TextAlignment.INHERIT
                    ? anchor.xFactor
                    : CONFIG.timer.textAlignment.xFactor;

            List<FormattedCharSequence> lines = font.split(itemComponent, 172);
            boolean reverse = anchor.yFactor > 0.5f;
            for (int i = 0; i < lines.size(); i++) {
                FormattedCharSequence line = lines.get(reverse ? lines.size() - 1 - i : i);
                int textX;
                int lineWidth = font.width(line);
                if (alignFactor == 0f) {
                    textX = -4;
                } else if (alignFactor == 1f) {
                    textX = width + 4 - lineWidth;
                } else {
                    textX = (width) / 2 - lineWidth / 2;
                }

                if (anchor.yFactor > 0.5f) {
                    textY -= font.lineHeight;
                }

                guiGraphics.drawString(font, line, textX, textY, 0xFFFFFFFF, true);

                if (anchor.yFactor > 0.5f) {
                    textY -= 1;
                } else {
                    textY += font.lineHeight + 1;
                }
            }
        }

        guiGraphics.pose().popPose();
    }

    /**
     * 1.21.1 has no {@code ItemStack#getStyledHoverName}, so the rarity colored name is built the same
     * way vanilla builds the first tooltip line.
     */
    public static Component styledHoverName(ItemStack stack) {
        return Component.empty().append(stack.getHoverName()).withStyle(stack.getRarity().color());
    }

    public static void renderTimerText(GuiGraphics guiGraphics, Font font, double totalSeconds, int x, int y, boolean showMs, OctoColor color, boolean showShadow) {
        int hours = (int)(totalSeconds / 3600);
        int minutes = (int)((totalSeconds % 3600) / 60);
        int seconds = (int)(totalSeconds % 60);
        int millis = (int)((totalSeconds - Math.floor(totalSeconds)) * 100);

        String time = String.format("%d:%02d:%02d", hours, minutes, seconds);
        String ms = showMs ? String.format(".%02d", millis) : "";

        int noMillisWidth = font.width(time) * 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2, 2, 2);
        if (showShadow) {
            OctoColor shadow = color.multiply(0.25f, 0.25f, 0.25f, 1f);
            guiGraphics.pose().translate(0.5f, 0.5f, 0);
            guiGraphics.drawString(font, time, 0, 0, shadow.getARGB(), false);
            guiGraphics.pose().translate(-0.5f, -0.5f, 0);
        }
        guiGraphics.drawString(font, time, 0, 0, color.getARGB(), false);
        guiGraphics.pose().popPose();

        guiGraphics.drawString(font, ms, noMillisWidth, 8, color.getARGB(), showShadow);

        guiGraphics.pose().popPose();
    }

    public static void renderTimerText(GuiGraphics guiGraphics, Font font, double totalSeconds, int x, int y, boolean showMs, OctoColor color) {
        renderTimerText(guiGraphics, font, totalSeconds, x, y, showMs, color, true);
    }
}
