package meow.binary.scavenger.fabric.client;

import meow.binary.scavenger.client.ScavengerClient;
import net.fabricmc.api.ClientModInitializer;

public final class ScavengerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // The HUD element itself is registered in common code through
        // Architectury's ClientGuiEvent.RENDER_HUD (see ScavengerClient#init).
        ScavengerClient.init();
    }
}
