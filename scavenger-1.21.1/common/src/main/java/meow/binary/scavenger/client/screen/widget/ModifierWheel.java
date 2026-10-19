package meow.binary.scavenger.client.screen.widget;

import it.hurts.octostudios.octolib.client.animation.Tween;
import it.hurts.octostudios.octolib.client.animation.easing.EaseType;
import it.hurts.octostudios.octolib.client.animation.easing.TransitionType;
import it.hurts.octostudios.octolib.client.particle.UIParticle;
import dev.architectury.platform.Platform;
import it.hurts.octostudios.octolib.util.OctoColor;
import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.client.particle.StarUIParticle;
import meow.binary.scavenger.client.screen.ScavengerWorldCreateScreen;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ModifierWheel extends AbstractWidget {
    public static final ResourceLocation SEPARATOR = ResourceLocation.fromNamespaceAndPath(Scavenger.MOD_ID, "textures/gui/separator.png");
    public static final ResourceLocation MACHINE = ResourceLocation.fromNamespaceAndPath(Scavenger.MOD_ID, "textures/gui/machine.png");
    public static final ResourceLocation MACHINE_BG = ResourceLocation.fromNamespaceAndPath(Scavenger.MOD_ID, "textures/gui/machine_bg.png");
    public static final ResourceLocation ARROW_LEFT = ResourceLocation.fromNamespaceAndPath(Scavenger.MOD_ID, "textures/gui/left_ar.png");
    public static final ResourceLocation ARROW_RIGHT = ResourceLocation.fromNamespaceAndPath(Scavenger.MOD_ID, "textures/gui/right_ar.png");

    public static final int SLOT_HEIGHT = 47;

    private List<ResourceLocation> modifiers = List.of();
    private List<ResourceLocation> modifiersReversed = List.of();

    private boolean isDone;
    private boolean rolling;

    public void setRotation(float rotation) {
        int count = modifiers.size();

        rotation %= count;
        if (rotation < 0) {
            rotation += count;
        }

        int currentSegment = Mth.floor(rotation);

        if (lastSegment != currentSegment) {
            lastSegment = currentSegment;
            shouldPlaySound = true;
            spawnArrowSparkles();
        }

        this.rotation = rotation;
    }

    private void spawnArrowSparkles() {
        float scale = Scavenger.CONFIG.wheels.scaleModifierWheel;
        float pivotX = this.getX() + this.width / 2f;
        float pivotY = this.getY() + this.height / 2f;
        float centerY = this.getY() + 95f;
        float leftX = this.getX() + 17f;
        float rightX = this.getX() + this.width - 18f;
        float leftY = centerY;
        float rightY = centerY;

        if (scale != 1f) {
            leftX = pivotX + (leftX - pivotX) * scale;
            rightX = pivotX + (rightX - pivotX) * scale;
            leftY = pivotY + (leftY - pivotY) * scale;
            rightY = pivotY + (rightY - pivotY) * scale;
        }

        leftX += xOffset;
        rightX += xOffset;
        leftY += yOffset;
        rightY += yOffset;

        spawnArrowSparkleBurst(leftX+4, leftY, 1f);
        spawnArrowSparkleBurst(rightX-4, rightY, -1f);
    }

    private void spawnArrowSparkleBurst(float originX, float originY, float directionSign) {
        for (int i = 0; i < 2; i++) {
            StarUIParticle particle = new StarUIParticle(
                    sparkleRandom.nextFloat(0.6f, 1.6f),
                    sparkleRandom.nextInt(9, 15),
                    originX + sparkleRandom.nextFloat(-3f, 3f),
                    originY + sparkleRandom.nextFloat(-5f, 5f),
                    UIParticle.Layer.SCREEN,
                    2
            );

            OctoColor color = new OctoColor(0xfff8e7b5);
            particle.getTransform().setSize(new Vector2f(1, 1).mul(sparkleRandom.nextFloat(0.55f, 0.95f)));
            particle.getTransform().setRoll(sparkleRandom.nextFloat(-10f, 10f));
            particle.setGravity(0.38f);
            particle.setFriction(sparkleRandom.nextFloat(0.004f, 0.012f));
            particle.setRollVelocity(sparkleRandom.nextFloat(-0.35f, 0.35f));
            particle.setDirection(directionSign * sparkleRandom.nextFloat(0.32f, 0.7f), sparkleRandom.nextFloat(-0.5f, 1f));
            particle.setColors(color, color);
            particle.setScreen(this.screen);
            particle.getTransform().updateOldValues();
            particle.instantiate();
        }
    }

    boolean shouldPlaySound;
    float rotation = 0.5f;
    int lastSegment = -1;
    Random sparkleRandom = new Random();
    Tween rotationTween = Tween.create();
    Tween finishingTween = Tween.create();

    final ScavengerWorldCreateScreen screen;
    float xOffset;
    private float yOffset;

    public void setxOffset(float xOffset) {
        this.xOffset = xOffset;
    }

    public ModifierWheel(int x, int y, int width, int height, ScavengerWorldCreateScreen screen) {
        super(x, y, width, height, Component.empty());
        this.screen = screen;
        this.refreshModifiers();
    }

    public void refreshModifiers() {
        List<ResourceLocation> filteredModifiers = new ArrayList<>(Modifiers.getIds().stream()
                .filter(id -> !Scavenger.CONFIG.gameplay.modifierBlacklist.contains(id.toString()))
                .toList());

        if (screen.getChosenItem() == Items.DRAGON_EGG) {
            filteredModifiers.remove(Modifiers.TWICE.getId());
            filteredModifiers.remove(Modifiers.THRICE.getId());
        }

        this.modifiers = List.copyOf(filteredModifiers);
        this.modifiersReversed = modifiers.reversed();
        this.lastSegment = -1;
        this.rotation = 0.5f;
    }

    public int getModifierCount() {
        return modifiers.size();
    }

    public ResourceLocation getSingleModifierOrNone() {
        if (modifiers.size() == 1) {
            return modifiers.getFirst();
        }

        return Modifiers.NONE.getId();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(xOffset, yOffset, 0);

        if (Scavenger.CONFIG.wheels.scaleModifierWheel != 1f) {
            float wheelScale = Scavenger.CONFIG.wheels.scaleModifierWheel;
            guiGraphics.pose().translate(this.getX() + this.width / 2f, this.getY() + this.height / 2f, 0);
            guiGraphics.pose().scale(wheelScale, wheelScale, wheelScale);
            guiGraphics.pose().translate(-this.getX() - this.width / 2f, -this.getY() - this.height / 2f, 0);
        }

        guiGraphics.blit(MACHINE_BG, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);

        int count = modifiers.size();

        int baseIndex = (int)Math.floor(rotation);
        float fraction = rotation - baseIndex;

        float pixelOffset = fraction * SLOT_HEIGHT;

        guiGraphics.enableScissor(this.getX() + 16, this.getY() + 50, this.getX() + 184, this.getY() + 140);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, pixelOffset, 0);

        int centerY = this.getY() + 95;

        for (int i = -2; i <= 1; i++) {
            int index = Math.floorMod(-baseIndex + i, count);
            ResourceLocation modifier = modifiersReversed.get(index);

            int y = 50 + i * SLOT_HEIGHT;
            int centerX = this.getX() + this.width / 2;

            float slotCenter = this.getY() + y + pixelOffset + SLOT_HEIGHT / 2f;

            float distance = Math.abs(slotCenter - centerY);

            float maxDist = SLOT_HEIGHT * 1.3f;
            float t = Mth.clamp(distance / maxDist, 0f, 1f);

            float factor = (float)Math.cos(t * Math.PI / 2f);
            factor = 0.5f + factor * 0.5f;
            float xScale = 0.95f + factor * 0.1f;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, this.getY() + y + (slotCenter > centerY ? 0 : SLOT_HEIGHT * (1 - factor)), 0);
            guiGraphics.pose().scale(xScale, factor, 1f);

            Component name = Modifiers.getName(modifier).withStyle(ChatFormatting.BOLD);
            Component description = Modifiers.getDescription(modifier);


            guiGraphics.drawString(
                    font,
                    name,
                     - font.width(name) / 2,
                    11,
                    0xff491825,
                    false
            );

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(-font.width(description) * 0.75f / 2f, 23, 0);
            guiGraphics.pose().scale(0.75f, 0.75f, 0.75f);

            guiGraphics.drawString(font, description, 0, 0, 0xff61322e, false);
            guiGraphics.pose().popPose();
            guiGraphics.pose().popPose();

            int sepY = 50 + i * SLOT_HEIGHT + SLOT_HEIGHT - 6;
            float sepCenter = this.getY() + sepY + pixelOffset + 4;

            float sepDistance = Math.abs(sepCenter - centerY);
            float sepT = Mth.clamp(sepDistance / maxDist, 0f, 1f);

            float sepFactor = (float)Math.cos(sepT * Math.PI / 2f);
            sepFactor = 0.5f + sepFactor * 0.5f;

            float sepXScale = 0.95f + sepFactor * 0.1f;

            guiGraphics.pose().pushPose();

            guiGraphics.pose().translate(
                    centerX,
                    this.getY() + sepY + (sepCenter > centerY ? -8 * (1 - sepFactor) : 8 * (1 - sepFactor)),
                    0
            );

            guiGraphics.pose().scale(sepXScale, sepFactor, 1f);

            guiGraphics.blit(SEPARATOR, -84, 0, 0, 0, 168, 8, 168, 8);

            guiGraphics.pose().popPose();
        }

        guiGraphics.pose().popPose();


        guiGraphics.fillGradient(this.getX() + 16, this.getY() + 50, this.getX() + 192, this.getY() + 70, 0xaa59443c, 0x0);
        guiGraphics.fillGradient(this.getX() + 16, this.getY() + 120, this.getX() + 192, this.getY() + 140, 0x0, 0xaa59443c);
        guiGraphics.disableScissor();

        //guiGraphics.hLine(this.getX(), this.getX()+this.width, this.getY() + 80, 0xffff0000);
        guiGraphics.blit(MACHINE, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
        guiGraphics.blit(ARROW_LEFT, this.getX() + 2, centerY - 8, 0, 0, 21, 16, 21, 16);
        guiGraphics.blit(ARROW_RIGHT, this.getX() + this.width - 22 - 1, centerY - 8, 0, 0, 21, 16, 21, 16);

        Component title = Component.translatable("scavenger.modifier").withStyle(ChatFormatting.BOLD);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.getX() + this.width / 2f - font.width(title), this.getY() + 16, 0);
        guiGraphics.pose().scale(2f, 2f, 2f);
        guiGraphics.drawString(font, title, 0, 0, 0xffEADBD0, true);
        guiGraphics.pose().popPose();
        //guiGraphics.drawString(font, this.getCurrentModifier().getPath(), this.getX() + this.width, this.getY(), 0xffffffff, true);

        guiGraphics.pose().popPose();

        if (shouldPlaySound) {
            shouldPlaySound = false;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 2f));
        }
    }

    private void finish() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VAULT_OPEN_SHUTTER, 1f));
        this.isDone = true;
        this.rolling = false;
        this.screen.setChosenModifier(this.getCurrentModifier());
        this.screen.createWidget.active = true;
    }

    private ResourceLocation getCurrentModifier() {
        if (lastSegment >= 0 && lastSegment < modifiers.size()) {
            int index = Math.floorMod(lastSegment - 1, modifiers.size());
            return modifiers.get(index);
        }

        return Modifiers.NONE.getId();
    }

    public void spin() {
        rotationTween.kill();
        finishingTween.kill();
        rotationTween = Tween.create();
        rotationTween.setTransitionType(TransitionType.QUAD);
        rotationTween.setEase(EaseType.EASE_OUT);
        rotationTween.tweenMethod(this::setRotation, rotation, rotation + screen.random.nextFloat(16, modifiers.size()+16), Scavenger.CONFIG.wheels.modifierRollTime+0.05d);
        rotationTween.parallel().tweenRunnable(() -> {
            rotationTween.kill();
            finishingTween.kill();
            finishingTween = Tween.create();
            finishingTween.setTransitionType(TransitionType.BACK);
            finishingTween.setEase(EaseType.EASE_OUT);
            finishingTween.tweenRunnable(() -> Minecraft.getInstance().submit(this::finish));
            finishingTween.tweenMethod(this::setRotation, rotation, Mth.floor(rotation)+0.5f, 0.5d);
            finishingTween.start();
        }).setDelay(Scavenger.CONFIG.wheels.modifierRollTime);
        //rotationTween.tweenMethod(this::setDarken, 0f, 1f, 0.4d).setEaseType(EaseType.EASE_IN_OUT).setTransitionType(TransitionType.SINE);
        rotationTween.start();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        this.trySpin();
    }

    public boolean trySpin() {
        if ((isDone || rolling) && !Platform.isDevelopmentEnvironment()) {
            return false;
        }

        isDone = false;
        screen.createWidget.active = false;
        rolling = true;

        this.spin();
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public float getyOffset() {
        return yOffset;
    }

    public void setyOffset(float yOffset) {
        this.yOffset = yOffset;
    }
}
