package ac.deneme.anticheat.checks.impl.velocity;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.combat.CombatEvidence;
import ac.deneme.anticheat.checks.type.PacketSendListener;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;
import ac.deneme.anticheat.utils.reflection.GeyserUtil;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "VelocityVector", stableKey = "deneme.velocity.vector", description = "Moved too little for the knockback it received", decay = 0.02)
public class VelocityVector extends Check implements PacketSendListener, PostPredictionListener {

    private static final double MINIMUM_EXPECTED = 0.12;
    private static final double MINIMUM_RATIO = 0.55;
    private static final int TRACKED_TICKS = 3;
    private static final int FLAG_STREAK = 3;

    private double expectedHorizontal;
    private int remainingTicks;
    private int streak;

    public VelocityVector(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY) {
            return;
        }

        WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(event);
        int vehicle = player.compensatedEntities.serverPlayerVehicle == null ? -1 : player.compensatedEntities.serverPlayerVehicle;
        int expectedEntity = vehicle == -1 ? player.entityID : vehicle;

        if (velocity.getEntityId() != expectedEntity) {
            return;
        }

        Vector3d motion = velocity.getVelocity();
        double horizontal = Math.sqrt(motion.getX() * motion.getX() + motion.getZ() * motion.getZ());

        if (horizontal < MINIMUM_EXPECTED) {
            expectedHorizontal = 0.0;
            remainingTicks = 0;
            return;
        }

        expectedHorizontal = horizontal;
        remainingTicks = TRACKED_TICKS;
    }

    @Override
    public void onPredictionComplete(@NotNull PredictionComplete predictionComplete) {
        if (expectedHorizontal <= 0.0 || remainingTicks <= 0) {
            return;
        }

        if (!predictionComplete.isChecked() || predictionComplete.getData() == null || predictionComplete.getData().isTeleport()) {
            reset();
            return;
        }

        double actual = Math.sqrt(
                player.actualMovement.getX() * player.actualMovement.getX()
                        + player.actualMovement.getZ() * player.actualMovement.getZ());

        remainingTicks--;

        if (exempt()) {
            reset();
            return;
        }

        double ratio = actual / expectedHorizontal;

        if (ratio < MINIMUM_RATIO) {
            streak++;
            CombatEvidence.report(player, "velocity_vector", 1.0);

            if (streak >= FLAG_STREAK) {
                flag(String.format("expected=%.3f actual=%.3f ratio=%.2f", expectedHorizontal, actual, ratio));
                streak = 0;
            }
        } else {
            streak = 0;
            reward();
        }

        if (remainingTicks <= 0) {
            expectedHorizontal = 0.0;
        }
    }

    private void reset() {
        expectedHorizontal = 0.0;
        remainingTicks = 0;
    }

    private boolean exempt() {
        return player.compensatedEntities.self.getRiding() != null
                || player.isGliding
                || player.isSwimming
                || player.isClimbing
                || player.disableDeneme
                || GeyserUtil.isBedrockPlayer(player.user.getUUID());
    }
}
