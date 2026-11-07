package ac.deneme.anticheat.checks;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.AbstractProcessor;
import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.config.ConfigReloadable;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.common.ConfigReloadObserver;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

public class DenemeProcessor implements AbstractProcessor, ConfigReloadable, ConfigReloadObserver {

    protected final @NotNull DenemePlayer player;

    public DenemeProcessor(final @NotNull DenemePlayer player) {
        this(player, true);
    }

    public DenemeProcessor(final @NotNull DenemePlayer player, boolean reload) {
        this.player = Objects.requireNonNull(player, "player");
        if (reload) {
            reload();
        }
    }

    @Override
    public final void reload() {
        reload(DenemeAPI.INSTANCE.getConfigManager().getConfig());
    }

    @Override
    public void reload(@NotNull ConfigManager configuration) {
        onReload(configuration);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {}

    @Override
    public String getConfigName() {
        return null;
    }

    public static boolean isTransaction(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PONG ||
                packetType == PacketType.Play.Client.WINDOW_CONFIRMATION;
    }

    public static boolean isAsync(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.KEEP_ALIVE
                || packetType == PacketType.Play.Client.CHUNK_BATCH_ACK
                || packetType == PacketType.Play.Client.RESOURCE_PACK_STATUS;
    }

    public final boolean isUpdate(PacketTypeCommon packetType) {
        return isFlying(packetType)

                || packetType == PacketType.Play.Client.CLIENT_TICK_END && player.supportsEndTickPreVia()
                || isTransaction(packetType);
    }

    public static boolean isAnimation(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.ANIMATION || packetType == PacketType.Play.Client.PUNCH;
    }

    public final boolean isTickPacket(PacketTypeCommon packetType) {
        if (isTickPacketIncludingNonMovement(packetType)) {
            if (isFlying(packetType)) {
                return !player.packetStateData.lastPacketWasTeleport && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate;
            }
            return true;
        }
        return false;
    }

    public final boolean isTickPacketIncludingNonMovement(PacketTypeCommon packetType) {

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2)
                && !player.packetStateData.didSendMovementBeforeTickEnd) {
            if (packetType == PacketType.Play.Client.CLIENT_TICK_END) {
                return true;
            }
        }

        return isFlying(packetType);
    }

    public final boolean canCancel(DiggingAction action) {
        return action != DiggingAction.RELEASE_USE_ITEM

                && (action != DiggingAction.DROP_ITEM && action != DiggingAction.DROP_ITEM_STACK || player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8));
    }
}
