package meow.binary.scavenger.mixin;

import net.minecraft.client.gui.components.toasts.Toast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.components.toasts.ToastComponent$ToastInstance")
public interface ToastInstanceAccessor {
    @Accessor("toast")
    Toast scavenger$getToast();
}
