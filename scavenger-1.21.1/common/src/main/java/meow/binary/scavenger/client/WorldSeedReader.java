package meow.binary.scavenger.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalLong;

public final class WorldSeedReader {
    private WorldSeedReader() {
    }

    /**
     * Reads the world seed straight out of a {@code level.dat} file. The seed lives in
     * {@code Data/WorldGenSettings/seed} since 1.16 and is not stored anywhere else on disk,
     * so the file has to be parsed without loading the world.
     */
    public static OptionalLong readSeed(Path levelDataPath) throws IOException {
        CompoundTag root = NbtIo.readCompressed(levelDataPath, NbtAccounter.unlimitedHeap());

        if (!root.contains("data", Tag.TAG_COMPOUND)) {
            return OptionalLong.empty();
        }

        CompoundTag data = root.getCompound("data");

        if (data.contains("WorldGenSettings", Tag.TAG_COMPOUND) && data.getCompound("WorldGenSettings").contains("seed", Tag.TAG_LONG)) {
            return OptionalLong.of(data.getCompound("WorldGenSettings").getLong("seed"));
        }

        if (data.contains("RandomSeed", Tag.TAG_LONG)) {
            return OptionalLong.of(data.getLong("RandomSeed"));
        }

        return OptionalLong.empty();
    }
}
