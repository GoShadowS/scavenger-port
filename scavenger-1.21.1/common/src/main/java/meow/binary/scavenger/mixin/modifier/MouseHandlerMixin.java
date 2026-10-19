package meow.binary.scavenger.mixin.modifier;

import meow.binary.scavenger.client.ClientScavengerData;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;

    /**
     * 1.21.1 has no "invert mouse X" option, so the horizontal flip for DRUNK is done by negating
     * the accumulated movement right before it is used. The field is reset to 0 by vanilla right
     * after {@code turnPlayer} returns, so this does not stack up.
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void scavenger$flipHorizontal(double partialTick, CallbackInfo ci) {
        if (ClientScavengerData.is(Modifiers.DRUNK)) {
            this.accumulatedDX = -this.accumulatedDX;
        }
    }

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0))
    private Object scavenger$modifySensitivity(OptionInstance<?> option) {
        if (ClientScavengerData.is(Modifiers.TURTLE)) {
            return 0d;
        }

        if (ClientScavengerData.is(Modifiers.SONIC)) {
            return 1d;
        }

        return option.get();
    }

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 1))
    private Object scavenger$modifyInvertMouseY(OptionInstance<?> option) {
        boolean inverted = (Boolean) option.get();
        return ClientScavengerData.is(Modifiers.DRUNK) != inverted;
    }
}
