package meow.binary.scavenger.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import meow.binary.scavenger.client.ClientScavengerData;
import meow.binary.scavenger.client.screen.ScavengerConfigScreen;
import meow.binary.scavenger.client.screen.ScavengerWorldCreateScreen;
import meow.binary.scavenger.client.screen.VictoryScreen;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    @Unique
    private int scavenger$timesPressed = 0;

    @Shadow
    private void onDisconnect() {
    }

    /**
     * Adds the scavenger buttons to the pause menu right before the grid gets arranged.
     * <p>
     * 26.x had a dedicated {@code LinearLayout} row of small icon buttons to put the config button
     * into, that layout does not exist in 1.21.1, so the config button is a regular full width row.
     */
    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout;arrangeElements()V"))
    private void scavenger$addButtons(CallbackInfo ci, @Local GridLayout.RowHelper helper) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = (Screen) (Object) this;

        Button victoryButton = Button.builder(Component.translatable("scavenger.open_victory_screen"), button ->
                        minecraft.setScreen(new VictoryScreen()))
                .width(98)
                .build();
        victoryButton.active = ClientScavengerData.winTimestamp != 0;
        helper.addChild(victoryButton, 1);

        helper.addChild(Button.builder(Component.translatable("scavenger.restart_run"), button -> {
            if (scavenger$timesPressed < 1) {
                button.setMessage(Component.translatable("scavenger.sure").withStyle(ChatFormatting.RED));
                scavenger$timesPressed++;
                return;
            }

            button.active = false;
            ScavengerWorldCreateScreen.queueRestart(minecraft, ClientScavengerData.item, ClientScavengerData.modifier);
            minecraft.getReportingContext().draftReportHandled(minecraft, screen, this::onDisconnect, true);
        }).width(98).build(), 1);

        helper.addChild(Button.builder(Component.translatable("scavenger.config"), button ->
                        minecraft.setScreen(new ScavengerConfigScreen(screen)))
                .width(204)
                .tooltip(Tooltip.create(Component.translatable("scavenger.config.tooltip")))
                .build(), 2);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void scavenger$renderInfo(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ClientScavengerData.isEmpty()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        Component modifierName = Modifiers.getName(ClientScavengerData.modifier).withStyle(ChatFormatting.BOLD);
        Component itemName = Component.empty().append(new ItemStack(ClientScavengerData.item).getHoverName()).withStyle(ChatFormatting.BOLD);
        Component activeModifier = Component.translatable("scavenger.active_modifier").append(": ").append(modifierName);
        Component itemToFind = Component.translatable("scavenger.item_to_find").append(": ").append(itemName);
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        int modifierWidth = font.width(activeModifier);
        int itemWidth = font.width(itemToFind);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(width / 2f, height - 32, 0);
        if (ClientScavengerData.is(Modifiers.TOURIST)) {
            Component language = Component.translatable("options.language.title").append(": ")
                    .append(Component.translatable("language.name").withStyle(ChatFormatting.BOLD));
            int languageWidth = font.width(language);
            guiGraphics.drawString(font, language, -languageWidth / 2, -2, 0xffffffff, true);
        }
        guiGraphics.drawString(font, activeModifier, -modifierWidth / 2, 8, 0xffffffff, true);
        guiGraphics.drawString(font, itemToFind, -itemWidth / 2, 18, 0xffffffff, true);
        guiGraphics.pose().popPose();

        float modifierPos = (width - modifierWidth) / 2f;
        float itemPos = (width - itemWidth) / 2f;
        int yPos = height - 32;

        if (mouseX >= modifierPos && mouseX < modifierPos + modifierWidth && mouseY >= yPos + 8 && mouseY < yPos + 17) {
            guiGraphics.renderTooltip(font, List.of(Modifiers.getDescription(ClientScavengerData.modifier)), Optional.empty(), mouseX, mouseY);
        } else if (mouseX >= itemPos && mouseX < itemPos + itemWidth && mouseY >= yPos + 18 && mouseY < yPos + 27) {
            guiGraphics.renderTooltip(font, ClientScavengerData.item.getDefaultInstance(), mouseX, mouseY);
        }
    }
}
