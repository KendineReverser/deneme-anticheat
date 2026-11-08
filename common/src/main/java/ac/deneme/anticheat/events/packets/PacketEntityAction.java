package ac.deneme.anticheat.events.packets;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.checks.impl.elytra.ElytraA;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.data.IntToObjectPair;
import ac.deneme.anticheat.utils.data.SprintingState;
import ac.deneme.anticheat.utils.data.packetentity.JumpableEntity;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

public class PacketEntityAction extends PacketListenerAbstract {

    private static final boolean SERVER_SUPPORTS_GLIDING = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9);

    public PacketEntityAction() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction action = new WrapperPlayClientEntityAction(event);
            DenemePlayer player = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());

            if (player == null) return;

            switch (action.getAction()) {
                case START_SPRINTING -> {
                    player.isSprinting = true;
                    player.vehicleData.camelSprintingState = SprintingState.STARTED;
                }
                case STOP_SPRINTING -> {
                    player.isSprinting = false;
                    player.vehicleData.camelSprintingState = SprintingState.STOPPED;
                }
                case START_SNEAKING -> player.isSneaking = true;
                case STOP_SNEAKING -> player.isSneaking = false;
                case START_FLYING_WITH_ELYTRA -> {
                    if (!SERVER_SUPPORTS_GLIDING) return;

                    if (player.onGround || player.lastOnGround) {
                        player.getSetbackTeleportUtil().executeNonSimulatingForceResync();
                        player.resyncGlidingState();
                        event.setCancelled(true);
                        player.onPacketCancel();
                        return;
                    }

                    player.checkManager.get(ElytraA.class).onStartGliding(event);

                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) return;

                    if (player.canGlide()) {
                        player.isGliding = true;
                        player.pointThreeEstimator.updatePlayerGliding();
                    } else {

                        player.getSetbackTeleportUtil().executeNonSimulatingForceResync();
                        player.resyncGlidingState();
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }
                case START_JUMPING_WITH_HORSE -> {
                    PacketEntity riding = player.compensatedEntities.self.getRiding();
                    if (riding instanceof JumpableEntity jumpable) {
                        if (player.vehicleData.pendingJumps.size() >= 20) return;
                        player.vehicleData.pendingJumps.add(new IntToObjectPair<>(action.getJumpBoost(), jumpable));
                    }
                }
            }
        }
    }
}
