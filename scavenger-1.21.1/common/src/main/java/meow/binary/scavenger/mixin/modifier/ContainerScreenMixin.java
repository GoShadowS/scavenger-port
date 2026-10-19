package meow.binary.scavenger.mixin.modifier;

import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMixin {
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    /**
     * 26.x rendered the barriers at the end of {@code extractSlots}, that method does not exist in
     * 1.21.1, so they are drawn on top of everything at the end of {@code render} instead.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void scavenger$renderBarriers(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        for (Slot slot : this.menu.slots) {
            if (slot.container instanceof Inventory inventory && Scavenger.isSlotBlocked(slot.getContainerSlot(), inventory.player.level())) {
                guiGraphics.renderItem(Items.BARRIER.getDefaultInstance(), slot.x, slot.y);
            }
        }
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("HEAD"), cancellable = true)
    private void scavenger$disableAdvancedTooltip(ItemStack item, CallbackInfoReturnable<List<Component>> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null) {
            return;
        }

        if (Modifiers.isActive(Modifiers.TOURIST, level)) {
            cir.setReturnValue(item.getTooltipLines(Item.TooltipContext.of(level), minecraft.player, TooltipFlag.NORMAL));
            cir.cancel();
        }
    }
}
