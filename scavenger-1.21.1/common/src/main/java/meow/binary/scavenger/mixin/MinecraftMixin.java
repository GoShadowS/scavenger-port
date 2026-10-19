package meow.binary.scavenger.mixin;

import meow.binary.scavenger.client.ScavengerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    /**
     * Every way of leaving a world in 1.21.1 funnels through {@code disconnect(Screen, boolean)},
     * so this is where the client side modifier state gets cleared.
     */
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    private void scavenger$resetClientModifierState(Screen screen, boolean transfer, CallbackInfo ci) {
        ScavengerClient.onClientDisconnect();
    }
}
