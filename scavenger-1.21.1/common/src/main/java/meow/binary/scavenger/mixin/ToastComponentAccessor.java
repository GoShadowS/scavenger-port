package meow.binary.scavenger.mixin;

import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ToastComponent.class)
public interface ToastComponentAccessor {
    @Accessor("visible")
    List<?> scavenger$getVisibleToasts();
}
