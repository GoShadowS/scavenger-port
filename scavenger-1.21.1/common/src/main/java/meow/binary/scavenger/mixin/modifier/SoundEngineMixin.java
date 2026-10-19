package meow.binary.scavenger.mixin.modifier;

import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SILENCE. {@code SoundManager#play} returns void in 1.21.1 (26.x returned a {@code PlayResult}).
 */
@Mixin(SoundManager.class)
public abstract class SoundEngineMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void scavenger$cancelSound(SoundInstance sound, CallbackInfo ci) {
        if (this.scavenger$silenced()) {
            ci.cancel();
        }
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void scavenger$cancelDelayedSound(SoundInstance sound, int delay, CallbackInfo ci) {
        if (this.scavenger$silenced()) {
            ci.cancel();
        }
    }

    @Inject(method = "queueTickingSound", at = @At("HEAD"), cancellable = true)
    private void scavenger$cancelTickingSound(TickableSoundInstance tickableSound, CallbackInfo ci) {
        if (this.scavenger$silenced()) {
            ci.cancel();
        }
    }

    private boolean scavenger$silenced() {
        Level level = Minecraft.getInstance().level;
        return level != null && Modifiers.isActive(Modifiers.SILENCE, level);
    }
}
