package meow.binary.scavenger.mixin.modifier;

import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.client.ClientScavengerData;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VEGETARIAN and CARNIVORE.
 * <p>
 * 26.x hooked {@code Consumable#canConsume}; that component does not exist in 1.21.1, where eating
 * starts in {@code Item#use}, so the use action is simply passed through for forbidden food.
 */
@Mixin(Item.class)
public class FoodMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void scavenger$preventEating(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.get(DataComponents.FOOD) == null) {
            return;
        }

        if (ClientScavengerData.is(Modifiers.VEGETARIAN)) {
            if (!stack.is(Scavenger.VEGETARIAN_FOOD)) {
                cir.setReturnValue(InteractionResultHolder.pass(stack));
            }
        } else if (ClientScavengerData.is(Modifiers.CARNIVORE)) {
            if (!stack.is(ItemTags.MEAT) && !stack.is(ItemTags.FISHES)) {
                cir.setReturnValue(InteractionResultHolder.pass(stack));
            }
        }
    }
}
