package meow.binary.scavenger.mixin.modifier;

import com.mojang.datafixers.util.Either;
import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PreventSprintingMixin {
    /**
     * SNAIL. 26.x hooked {@code Player#isMobilityRestricted}, that method does not exist in 1.21.1,
     * so sprinting is cancelled directly instead.
     */
    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void scavenger$preventSprinting(boolean sprinting, CallbackInfo ci) {
        if (sprinting) {
            Player player = (Player) (Object) this;
            if (Modifiers.isActive(Modifiers.SNAIL, player.level())) {
                ci.cancel();
            }
        }
    }

    /**
     * INSOMNIA. {@code startSleepInBed} only takes the bed position in 1.21.1.
     */
    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void scavenger$cancelSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        Player player = (Player) (Object) this;
        if (Modifiers.isActive(Modifiers.INSOMNIA, player.level())) {
            // BedSleepingProblem is an enum in 1.21.1, so the custom message is shown by hand.
            player.displayClientMessage(Scavenger.INSOMNIA_MESSAGE, true);
            cir.setReturnValue(Either.left(Scavenger.INSOMNIA_PROBLEM));
        }
    }
}
