package ac.deneme.anticheat.checks.impl.velocity;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.checks.type.PacketSendListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.reflection.GeyserUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "SprintTamper", stableKey = "deneme.velocity.sprint_tamper", description = "Rewrote sprint state inside the knockback tick", decay = 0.02, experimental = true)
public class SprintTamper extends Check implements PacketReceiveListener, PacketSendListener {

    private static final long TAMPER_WINDOW_NS = 180_000_000L;
    private static final int FLIPS_TO_FLAG = 2;

    private long knockbackAt;
    private int flips;

    public SprintTamper(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY) return;

        WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(event);
        int vehicle = player.compensatedEntities.serverPlayerVehicle == null ? -1 : player.compensatedEntities.serverPlayerVehicle;
        int expected = vehicle == -1 ? player.entityID : vehicle;

        if (velocity.getEntityId() != expected) return;

        knockbackAt = System.nanoTime();
        flips = 0;
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.ENTITY_ACTION) return;
        if (knockbackAt == 0 || exempt()) return;

        WrapperPlayClientEntityAction action = new WrapperPlayClientEntityAction(event);
        if (action.getAction() != WrapperPlayClientEntityAction.Action.START_SPRINTING
                && action.getAction() != WrapperPlayClientEntityAction.Action.STOP_SPRINTING) return;

        long elapsed = System.nanoTime() - knockbackAt;
        if (elapsed > TAMPER_WINDOW_NS) {
            kill();
            return;
        }

        if (++flips >= FLIPS_TO_FLAG) {
            flag(String.format("flips=%d elapsed=%.1fms", flips, elapsed / 1.0e6));
            kill();
        }
    }

    private void kill() {
        knockbackAt = 0;
        flips = 0;
    }

    private boolean exempt() {
        return player.compensatedEntities.self.getRiding() != null
                || GeyserUtil.isBedrockPlayer(player.user.getUUID())
                || player.getTransactionPing() > 300
                || player.disableDeneme;
    }
}
