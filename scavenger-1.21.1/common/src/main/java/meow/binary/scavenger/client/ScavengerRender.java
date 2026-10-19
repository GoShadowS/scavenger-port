package meow.binary.scavenger.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Small rendering helpers that replace the 26.x {@code GuiGraphics} blit overloads which took a
 * render pipeline and a color. 1.21.1 blits through {@code BufferUploader} right away, so the
 * shader color and the blend function can simply be set around the call.
 */
public final class ScavengerRender {
    private ScavengerRender() {
    }

    /** Draws a texture tinted with {@code color} using regular alpha blending. */
    public static void blitTinted(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height,
                                  int textureWidth, int textureHeight, int color) {
        setShaderColor(color);
        guiGraphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /**
     * Draws a texture with the {@code DST_COLOR, ONE} blend function, the 1.21.1 equivalent of the
     * "multiply" pipeline that was used for the wheel selection highlight. With a black color the
     * texture is invisible, brighter colors brighten what is already on screen.
     */
    public static void blitDodge(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height,
                                 int textureWidth, int textureHeight, int color) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE);
        setShaderColor(color);

        guiGraphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
    }

    private static void setShaderColor(int argb) {
        float alpha = ((argb >> 24) & 0xFF) / 255f;
        float red = ((argb >> 16) & 0xFF) / 255f;
        float green = ((argb >> 8) & 0xFF) / 255f;
        float blue = (argb & 0xFF) / 255f;
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }
}
