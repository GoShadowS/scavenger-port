package meow.binary.scavenger.network;

import it.hurts.octostudios.octolib.module.network.Packet;
import meow.binary.scavenger.Scavenger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class SyncScavengerDataPacket extends Packet {
    public static final CustomPacketPayload.Type<SyncScavengerDataPacket> TYPE = Packet.createType(Scavenger.MOD_ID, "sync_scavenger_data");
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncScavengerDataPacket> STREAM_CODEC = Packet.createCodec(SyncScavengerDataPacket::write, SyncScavengerDataPacket::new);

    public Item getItem() {
        return item;
    }
    public ResourceLocation getModifier() {
        return modifier;
    }
    public long getWinTimestamp() {
        return winTimestamp;
    }

    Item item;
    ResourceLocation modifier;
    long winTimestamp;
    public boolean isWin;

    public SyncScavengerDataPacket(RegistryFriendlyByteBuf buf) {
        super(buf);
        this.item = BuiltInRegistries.ITEM.getOptional(buf.readResourceLocation()).orElse(Items.AIR);
        this.modifier = buf.readResourceLocation();
        this.winTimestamp = buf.readLong();
        this.isWin = buf.readBoolean();
    }

    public SyncScavengerDataPacket(Item item, ResourceLocation modifier, long winTimestamp, boolean isWin) {
        this.item = item;
        this.modifier = modifier;
        this.winTimestamp = winTimestamp;
        this.isWin = isWin;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
        buf.writeResourceLocation(modifier);
        buf.writeLong(winTimestamp);
        buf.writeBoolean(isWin);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
