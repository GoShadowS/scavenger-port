package meow.binary.scavenger.mixin;

import meow.binary.scavenger.client.ScavengerClient;
import meow.binary.scavenger.client.screen.RunHistoryScreen;
import meow.binary.scavenger.client.screen.ScavengerConfigScreen;
import meow.binary.scavenger.client.screen.ScavengerWorldCreateScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Unique
    private boolean scavenger$queuedRestartLaunch;

    @Inject(method = "init", at = @At("TAIL"))
    private void scavenger$launchPendingRestart(CallbackInfo ci) {
        ScavengerClient.onTitleScreenShown();

        if (scavenger$queuedRestartLaunch || !ScavengerWorldCreateScreen.hasPendingRestart()) {
            return;
        }

        scavenger$queuedRestartLaunch = true;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.submit(() -> ScavengerWorldCreateScreen.launchPendingRestart(minecraft));
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void scavenger$addButtons(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        ScreenInvoker screenInvoker = (ScreenInvoker) screen;

        screenInvoker.scavenger$addRenderableWidget(Button.builder(Component.literal("\u2605"), button ->
                        Minecraft.getInstance().setScreen(new RunHistoryScreen(screen)))
                .bounds(screen.width / 2 + 104, screen.height / 4 + 48, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("scavenger.run_history")))
                .build());

        screenInvoker.scavenger$addRenderableWidget(Button.builder(Component.literal("\uD83D\uDD27"), button ->
                        Minecraft.getInstance().setScreen(new ScavengerConfigScreen(screen)))
                .bounds(screen.width / 2 + 104, screen.height / 4 + 72, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("scavenger.config")))
                .build());
    }
}
