package meow.binary.scavenger.data;

import meow.binary.scavenger.Scavenger;
import meow.binary.scavenger.data.modifier.ScavengerModifier;
import meow.binary.scavenger.registry.Modifiers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ScavengerSavedData extends SavedData {
    private static final String DATA_NAME = "scavenger_data";

    public static final SavedData.Factory<ScavengerSavedData> FACTORY = new SavedData.Factory<>(
            ScavengerSavedData::new,
            (tag, provider) -> load(tag),
            DataFixTypes.LEVEL
    );

    private ResourceLocation itemId;
    private ResourceLocation modifierId;
    private boolean hasWon;
    private long winTimestamp;

    public ScavengerSavedData() {
        this.itemId = BuiltInRegistries.ITEM.getKey(Scavenger.TEMP_DATA.item);
        this.modifierId = Scavenger.TEMP_DATA.modifier;
        this.hasWon = false;
        this.winTimestamp = 0L;

        Scavenger.TEMP_DATA.item = Items.AIR;
        Scavenger.TEMP_DATA.modifier = Modifiers.NONE.getId();

        this.setDirty();
    }

    private ScavengerSavedData(ResourceLocation itemId, ResourceLocation modifierId, boolean hasWon, long winTimestamp) {
        this.itemId = itemId;
        this.modifierId = modifierId;
        this.hasWon = hasWon;
        this.winTimestamp = winTimestamp;
    }

    public static ScavengerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString("itemId", this.itemId.toString());
        tag.putString("modifier", this.modifierId.toString());
        tag.putBoolean("hasWon", this.hasWon);
        tag.putLong("winTimestamp", this.winTimestamp);
        return tag;
    }

    private static ScavengerSavedData load(CompoundTag tag) {
        return new ScavengerSavedData(
                parseId(tag.getString("itemId"), BuiltInRegistries.ITEM.getKey(Items.AIR)),
                parseId(tag.getString("modifier"), Modifiers.NONE.getId()),
                tag.getBoolean("hasWon"),
                tag.getLong("winTimestamp")
        );
    }

    private static ResourceLocation parseId(String value, ResourceLocation fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }

        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? fallback : id;
    }

    public ResourceLocation getItemId() {
        return itemId;
    }

    public Item getItem() {
        return BuiltInRegistries.ITEM.getOptional(this.itemId).orElse(Items.AIR);
    }

    public void setItem(Item item) {
        this.itemId = BuiltInRegistries.ITEM.getKey(item);

        this.setDirty();
    }

    public long getWinTimestamp() {
        return winTimestamp;
    }

    public void setWinTimestamp(long winTimestamp) {
        this.winTimestamp = winTimestamp;
        this.setDirty();
    }

    public ResourceLocation getModifierId() {
        return modifierId;
    }

    public ScavengerModifier getModifier() {
        return Modifiers.get(modifierId);
    }

    public void setModifierId(ResourceLocation modifierId) {
        this.modifierId = modifierId;

        this.setDirty();
    }

    public boolean hasWon() {
        return hasWon;
    }

    public void win(long winTimestamp) {
        this.hasWon = true;
        this.setWinTimestamp(winTimestamp);
        this.setDirty();
    }

    /**
     * Reads a world's scavenger data straight from disk, without loading the world.
     * Used by the world list tooltip and by the run history scanner.
     */
    public static Optional<ScavengerSavedData> readFromDisk(Path path) {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }

        try {
            CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            CompoundTag data = root.contains("data", net.minecraft.nbt.Tag.TAG_COMPOUND) ? root.getCompound("data") : root;

            String itemId = data.getString("itemId");
            String modifierId = data.getString("modifier");

            if (itemId.isEmpty() || modifierId.isEmpty()) {
                return Optional.empty();
            }

            ResourceLocation item = ResourceLocation.tryParse(itemId);
            ResourceLocation modifier = ResourceLocation.tryParse(modifierId);

            if (item == null || modifier == null) {
                return Optional.empty();
            }

            return Optional.of(new ScavengerSavedData(item, modifier, data.getBoolean("hasWon"), data.getLong("winTimestamp")));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    public boolean isEmpty() {
        return this.modifierId.equals(Modifiers.NONE.getId()) && itemId.equals(BuiltInRegistries.ITEM.getKey(Items.AIR));
    }
}
