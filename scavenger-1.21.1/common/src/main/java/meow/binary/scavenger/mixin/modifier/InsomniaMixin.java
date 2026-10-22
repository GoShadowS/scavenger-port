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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * INSOMNIA. {@code startSleepInBed} only takes the bed position in 1.21.1, and
 * {@code Player.BedSleepingProblem} is a plain enum, so the custom reason from 26.x cannot be added -
 * the vanilla {@code OTHER_PROBLEM} is used and the message is shown by hand.
 * <p>
 * The SNAIL half of the old {@code PreventSprintingMixin} moved to {@link LivingEntityMixin}, because
 * {@code setSprinting} is declared by {@code LivingEntity} and mixin refmaps cannot remap a target
 * method that the mixin's own target class merely inherits.
 */
@Mixin(Player.class)
public abstract class InsomniaMixin {
    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void scavenger$cancelSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        Player player = (Player) (Object) this;
        if (Modifiers.isActive(Modifiers.INSOMNIA, player.level())) {
            player.displayClientMessage(Scavenger.INSOMNIA_MESSAGE, true);
            cir.setReturnValue(Either.left(Scavenger.INSOMNIA_PROBLEM));
        }
    }
}
