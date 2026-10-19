package meow.binary.scavenger.mixin.modifier;

import meow.binary.scavenger.client.ClientScavengerData;
import meow.binary.scavenger.mixin.OptionsSubScreenAccessor;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseSettingsScreen.class)
public abstract class MouseSettingsScreenMixin {
    @Inject(method = "addOptions", at = @At("TAIL"))
    private void scavenger$disableInvertOptions(CallbackInfo ci) {
        if (!ClientScavengerData.is(Modifiers.DRUNK)) {
            return;
        }

        // 1.21.1 only has the "invert mouse Y" option, "invert mouse X" was added in a later version.
        OptionsSubScreenAccessor accessor = (OptionsSubScreenAccessor) this;
        AbstractWidget invertY = accessor.getList().findOption(accessor.getOptions().invertYMouse());
        if (invertY != null) {
            invertY.active = false;
        }
    }
}
