package meow.binary.scavenger.neoforge.client;

import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.client.ScavengerClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Scavenger.MOD_ID, dist = Dist.CLIENT)
public final class ScavengerNeoForgeClient {
    public ScavengerNeoForgeClient() {
        // The HUD element itself is registered in common code through
        // Architectury's ClientGuiEvent.RENDER_HUD (see ScavengerClient#init).
        ScavengerClient.init();
    }
}
