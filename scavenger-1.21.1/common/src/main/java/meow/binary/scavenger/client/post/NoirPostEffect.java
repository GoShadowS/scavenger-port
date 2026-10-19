package meow.binary.scavenger.client.post;

import it.hurts.octostudios.octolib.module.post_effect.PostEffect;
import it.hurts.octostudios.octolib.module.post_effect.RenderStage;
import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.client.ClientScavengerData;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Black and white post effect that is applied while the NOIR modifier is running.
 *
 * <p>In 26.x the effect was pushed into the player's active post effect list, that API is gone in
 * 1.21.1, so OctoLib's post effect registry is used instead. It decides on its own when to render,
 * which means the client data only has to be cleared on disconnect.</p>
 */
public class NoirPostEffect extends PostEffect {
    public static final ResourceLocation PATH = ResourceLocation.fromNamespaceAndPath(Scavenger.MOD_ID, "shaders/post/noir.json");

    @Override
    public boolean shouldRender() {
        return ClientScavengerData.is(Modifiers.NOIR) && Minecraft.getInstance().player != null;
    }

    @Override
    public ResourceLocation getPath() {
        return PATH;
    }

    @Override
    public RenderStage getStage() {
        return RenderStage.LEVEL;
    }
}
