package ac.deneme.anticheat.events.packets;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.data.SprintingState;
import ac.deneme.anticheat.utils.nmsutil.WatchableIndexUtil;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class PacketSelfMetadataListener extends PacketListenerAbstract {

    public PacketSelfMetadataListener() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

        DenemePlayer player = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        WrapperPlayServerEntityMetadata entityMetadata = new WrapperPlayServerEntityMetadata(event);
        if (entityMetadata.getEntityId() != player.entityID) return;

        List<EntityData<?>> metadata = entityMetadata.getEntityMetadata();

        boolean hasSendTransaction = false;

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {

            metadata.removeIf(element -> element.getIndex() == 6);
            entityMetadata.setEntityMetadata(metadata);
            event.markForReEncode(true);
        }

        if (WatchableIndexUtil.getIndexValue(metadata, 0) instanceof Byte v) {
            byte field = v;
            boolean isGliding = (field & 0x80) != 0 && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);
            boolean isSwimming = (field & 0x10) != 0;
            boolean isSprinting = (field & 0x8) != 0;

            if (!hasSendTransaction) player.sendTransaction();
            hasSendTransaction = true;

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                player.isSwimming = isSwimming;
                player.lastSprinting = isSprinting;
                if (!isSprinting) {
                    player.vehicleData.camelSprintingState = SprintingState.STOPPING;
                }

                if (player.isGliding != isGliding) {
                    player.pointThreeEstimator.updatePlayerGliding();
                }
                player.isGliding = isGliding;
            });
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_10)) {
            if (WatchableIndexUtil.getIndexValue(metadata, 5) instanceof Boolean hasNoGravity) {

                boolean hasGravity = !hasNoGravity;

                if (!hasSendTransaction) player.sendTransaction();
                hasSendTransaction = true;
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.playerEntityHasGravity = hasGravity);
            }
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            if (WatchableIndexUtil.getIndexValue(metadata, 7) instanceof Integer v) {
                int ticksFrozen = v;

                if (!hasSendTransaction) player.sendTransaction();
                hasSendTransaction = true;
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.powderSnowFrozenTicks = ticksFrozen);
            }
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            int id;

            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_14_4)) {
                id = 12;
            } else if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_4)) {
                id = 13;
            } else {
                id = 14;
            }

            if (WatchableIndexUtil.getIndexValue(metadata, id) instanceof Optional<?> optional) {
                if (!hasSendTransaction) player.sendTransaction();
                hasSendTransaction = true;

                if (optional.orElse(null) instanceof Vector3i bedPos) {
                    player.addRealTimeTaskNow(() -> {
                        player.isInBed = true;
                        player.bedPosition = new Vector3d(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5);
                    });
                } else {

                    player.addRealTimeTaskNow(() -> player.isInBed = false);
                }
            }
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            int id;

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
                id = 8;
            } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
                id = 7;
            } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_10)) {
                id = 6;
            } else {
                id = 5;
            }

            if (WatchableIndexUtil.getIndexValue(metadata, id) instanceof Byte v) {
                byte data = v;

                if (!hasSendTransaction) player.sendTransaction();
                hasSendTransaction = true;

                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
                    boolean isRiptiding = (data & 0x4) != 0;

                    player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                            () -> player.isRiptidePose = isRiptiding);
                }

                boolean isActive = (data & 0x1) != 0;
                boolean isOffhand = (data & 0x2) != 0;

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                        () -> player.packetStateData.setSlowedByUsingItem(false));

                int markedTransaction = player.lastTransactionSent.get();

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get() + 1, () -> {

                    if (player.packetStateData.slowedByUsingItemTransaction < markedTransaction) {
                        player.packetStateData.setSlowedByUsingItem(isActive);

                        if (isActive) {
                            player.packetStateData.itemInUseHand = isOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                        }
                    }
                });

                event.getTasksAfterSend().add(player::sendTransaction);
            }
        }
    }
}
