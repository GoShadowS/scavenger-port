package meow.binary.scavenger.mixin.modifier;

import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    /**
     * SNAIL. 26.x hooked {@code Player#isMobilityRestricted}, which does not exist in 1.21.1.
     * {@code setSprinting} is declared by {@link LivingEntity} rather than {@link Player}, so the
     * injection has to live here: a mixin refmap can only remap methods declared by the target class
     * itself, and an inherited target ends up as
     * "Cannot remap setSprinting because it does not exists in any of the targets [Player]".
     */
    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void scavenger$preventSprinting(boolean sprinting, CallbackInfo ci) {
        if (!sprinting) {
            return;
        }

        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player && Modifiers.isActive(Modifiers.SNAIL, entity.level())) {
            ci.cancel();
        }
    }

    @Inject(method = "canUseSlot", at = @At("HEAD"), cancellable = true)
    private void inject(EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player)) {
            return;
        }

        Level level = entity.level();
        if (Modifiers.isActive(Modifiers.BRITTLE_BONES, level) && slot.getType().equals(EquipmentSlot.Type.HUMANOID_ARMOR)) {
            cir.setReturnValue(false);
            return;
        }

        if (Modifiers.isActive(Modifiers.ONE_ARM, level) && slot.equals(EquipmentSlot.OFFHAND)) {
            cir.setReturnValue(false);
            return;
        }
    }
}
