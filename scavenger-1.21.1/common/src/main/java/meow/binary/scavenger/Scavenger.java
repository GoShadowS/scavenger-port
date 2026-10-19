package meow.binary.scavenger;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import it.hurts.octostudios.octolib.module.config.ConfigManager;
import meow.binary.scavenger.client.Config;
import meow.binary.scavenger.data.ScavengerSavedData;
import meow.binary.scavenger.data.modifier.ScavengerModifier;
import meow.binary.scavenger.network.SyncRunRecordPacket;
import meow.binary.scavenger.network.SyncScavengerDataPacket;
import meow.binary.scavenger.registry.Modifiers;
import net.fabricmc.api.EnvType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;

import java.util.Collection;
import java.util.Optional;

public final class Scavenger {
    public static final Config CONFIG = new Config();
    public static final TemporaryData TEMP_DATA = new TemporaryData();
    public static final String MOD_ID = "scavenger";

    /**
     * {@code Player.BedSleepingProblem} is an enum in 1.21.1 (26.x allowed custom instances), so the
     * INSOMNIA modifier reuses {@code OTHER_PROBLEM} and shows its own message on the action bar.
     */
    public static final Player.BedSleepingProblem INSOMNIA_PROBLEM = Player.BedSleepingProblem.OTHER_PROBLEM;
    public static final Component INSOMNIA_MESSAGE = Component.translatable("scavenger.insomnia");

    public static final TagKey<Item> VEGETARIAN_FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "vegetarian_food"));
    public static final TagKey<Item> UNROLLABLE_BY_DEFAULT = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "unrollable_by_default"));

    public static void init() {
        ConfigManager.registerConfig(MOD_ID, CONFIG);

        // On a dedicated server only the payload types have to be registered, the receivers
        // are registered client side in ScavengerClient#init (they touch client only classes).
        if (Platform.getEnv() == EnvType.SERVER) {
            NetworkManager.registerS2CPayloadType(SyncScavengerDataPacket.TYPE, SyncScavengerDataPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(SyncRunRecordPacket.TYPE, SyncRunRecordPacket.STREAM_CODEC);
        }

        // 26.x wired these up per loader, Architectury's common events cover both.
        PlayerEvent.PLAYER_JOIN.register(Scavenger::onPlayerJoin);

        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                onPlayerTick(serverPlayer);
            }
        });

        LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> {
            if (level.dimension() == Level.OVERWORLD) {
                onServerLevelLoad(level);
            }
        });
    }

    public static void onPlayerTick(ServerPlayer serverPlayer) {
        MinecraftServer server = serverPlayer.level().getServer();
        if (server == null) {
            return;
        }

        ScavengerSavedData data = ScavengerSavedData.get(server);
        if (data.isEmpty()) {
            return;
        }

        ScavengerModifier modifier = data.getModifier();

        if (!data.hasWon()) {
            checkWinCondition(serverPlayer, data);
        }

        if (modifier.hasPlayerTick()) {
            modifier.playerTick(serverPlayer);
        }
    }

    public static void onPlayerJoin(ServerPlayer serverPlayer) {
        MinecraftServer server = serverPlayer.level().getServer();
        if (server == null) {
            return;
        }

        ScavengerSavedData data = ScavengerSavedData.get(server);
        if (data.isEmpty()) return;
        SyncScavengerDataPacket packet = new SyncScavengerDataPacket(data.getItem(), data.getModifierId(), data.getWinTimestamp(), false);
        NetworkManager.sendToPlayer(serverPlayer, packet);
    }

    public static void onServerLevelLoad(ServerLevel level) {
        ScavengerSavedData data = ScavengerSavedData.get(level.getServer());
        if (data.isEmpty()) {
            return;
        }

        ScavengerModifier modifier = data.getModifier();

        if (modifier.hasWorldStart()) {
            modifier.onWorldStart(level);
        }
    }

    private static void checkWinCondition(ServerPlayer player, ScavengerSavedData data) {
        int itemCount = getItemCount(data.getModifierId());

        boolean hasItem = player.getInventory().countItem(data.getItem()) >= itemCount;
        boolean hasWon = hasItem && (!data.getModifierId().equals(Modifiers.FINALIST.getId()) || hasKilledDragon(player));
        if (hasWon) {
            data.win(player.level().getGameTime());
            SyncScavengerDataPacket packet = new SyncScavengerDataPacket(data.getItem(), data.getModifierId(), data.getWinTimestamp(), true);

            ServerLevel level = player.serverLevel();
            MinecraftServer server = level.getServer();
            server.saveEverything(true, false, false);

            Collection<ServerPlayer> players = server.getPlayerList().getPlayers();
            NetworkManager.sendToPlayers(players, packet);

            SyncRunRecordPacket recordPacket = new SyncRunRecordPacket(
                    server.getWorldData().getLevelName(),
                    data.getItemId(),
                    data.getModifierId(),
                    data.getWinTimestamp(),
                    Optional.of(level.getSeed()),
                    players.size() > 1
            );
            NetworkManager.sendToPlayers(players, recordPacket);
        }
    }

    private static boolean hasKilledDragon(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }

        ServerLevel end = server.getLevel(Level.END);
        EndDragonFight fight = null;

        if (end != null) {
            fight = end.getDragonFight();
        }

        if (fight == null) {
            return false;
        }

        return fight.hasPreviouslyKilledDragon();
    }

    public static int getItemCount(ResourceLocation modifier) {
        int itemCount = 1;
        if (modifier.equals(Modifiers.TWICE.getId())) itemCount = 2;
        else if (modifier.equals(Modifiers.THRICE.getId())) itemCount = 3;

        return itemCount;
    }

    public static boolean isSlotBlocked(int index, Level level) {
        if (Modifiers.isActive(Modifiers.HOLEY_POCKETS, level) && index > 8 && index < 36) {
            return true;
        }

        return false;
    }

    public static void saveConfig() {
        ConfigManager.uploadDataToConfig(MOD_ID);
    }
}
