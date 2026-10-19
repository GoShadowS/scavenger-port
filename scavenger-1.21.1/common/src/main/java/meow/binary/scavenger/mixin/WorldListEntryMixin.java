package meow.binary.scavenger.mixin;

import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.client.ScavengerTimeFormat;
import meow.binary.scavenger.data.ScavengerSavedData;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shows the scavenger state of a world in the world selection tooltip.
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryMixin {
    @Shadow
    @Final
    LevelSummary summary;

    @Unique
    private final Map<String, Optional<ScavengerWorldTooltipData>> scavenger$tooltipCache = new HashMap<>();

    @Inject(method = "render", at = @At("TAIL"))
    private void scavenger$showTooltip(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                                       int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
        if (!hovered) {
            return;
        }

        Optional<ScavengerWorldTooltipData> data = this.scavenger$getTooltipData(this.summary);
        if (data.isEmpty()) {
            return;
        }

        ScavengerWorldTooltipData tooltipData = data.get();
        List<Component> tooltip = new ArrayList<>();

        Item item = tooltipData.item();
        Component itemName = item == null || item == Items.AIR
                ? Component.literal(BuiltInRegistries.ITEM.getKey(Items.AIR).toString())
                : Component.empty().append(new ItemStack(item).getHoverName());

        tooltip.add(Component.translatable("scavenger.item_to_find")
                .append(": ")
                .append(itemName.withStyle(ChatFormatting.BOLD)));
        tooltip.add(Component.translatable("scavenger.active_modifier")
                .append(": ")
                .append(Modifiers.getName(tooltipData.modifierId()).withStyle(ChatFormatting.BOLD)));

        if (tooltipData.completed()) {
            tooltip.add(Component.translatable("scavenger.world_status")
                    .append(": ")
                    .append(Component.translatable("scavenger.world_status.completed").withStyle(ChatFormatting.GREEN)));
            tooltip.add(Component.translatable("scavenger.completion_time")
                    .append(": ")
                    .append(Component.literal(ScavengerTimeFormat.format(tooltipData.winTimestamp(), tooltipData.modifierId())).withStyle(ChatFormatting.AQUA)));
        } else {
            tooltip.add(Component.translatable("scavenger.world_status")
                    .append(": ")
                    .append(Component.translatable("scavenger.world_status.incomplete").withStyle(ChatFormatting.GRAY)));
        }

        guiGraphics.renderTooltip(Minecraft.getInstance().font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Unique
    private Optional<ScavengerWorldTooltipData> scavenger$getTooltipData(LevelSummary summary) {
        if (summary == null) {
            return Optional.empty();
        }

        return this.scavenger$tooltipCache.computeIfAbsent(summary.getLevelId(), this::scavenger$readTooltipData);
    }

    @Unique
    private Optional<ScavengerWorldTooltipData> scavenger$readTooltipData(String levelId) {
        LevelStorageSource levelSource = Minecraft.getInstance().getLevelSource();
        // DimensionDataStorage keeps its files in <world>/data/<name>.dat
        Path dataPath = levelSource.getLevelPath(levelId).resolve("data").resolve(Scavenger.MOD_ID + "_data.dat");

        Optional<ScavengerSavedData> data = ScavengerSavedData.readFromDisk(dataPath);
        if (data.isEmpty() || data.get().isEmpty()) {
            return Optional.empty();
        }

        ScavengerSavedData savedData = data.get();
        Item item = BuiltInRegistries.ITEM.getOptional(savedData.getItemId()).orElse(Items.AIR);

        return Optional.of(new ScavengerWorldTooltipData(item, savedData.getModifierId(), savedData.hasWon(), savedData.getWinTimestamp()));
    }

    @Unique
    private record ScavengerWorldTooltipData(Item item, ResourceLocation modifierId, boolean completed, long winTimestamp) {
    }
}
