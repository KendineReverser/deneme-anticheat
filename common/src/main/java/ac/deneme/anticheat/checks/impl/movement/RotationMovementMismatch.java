package ac.deneme.anticheat.checks.impl.movement;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.combat.CombatEvidence;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;
import ac.deneme.anticheat.utils.reflection.GeyserUtil;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "RotationMovementMismatch", stableKey = "deneme.combat.rotation_movement", description = "Rotated without its movement direction following", decay = 0.02)
public class RotationMovementMismatch extends Check implements PostPredictionListener {

    private static final float MINIMUM_YAW_CHANGE = 25.0f;
    private static final double MINIMUM_SPEED = 0.15;
    private static final double FOLLOW_RATIO = 0.35;
    private static final long WINDOW_MS = 10000L;
    private static final int FLAG_HITS = 3;

    private float lastYaw;
    private double lastDirectionX;
    private double lastDirectionZ;
    private boolean primed;
    private int hits;
    private long firstHitAt;

    public RotationMovementMismatch(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(@NotNull PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked() || predictionComplete.getData() == null) {
            primed = false;
            return;
        }

        if (exempt()) {
            primed = false;
            return;
        }

        double movementX = player.actualMovement.getX();
        double movementZ = player.actualMovement.getZ();
        double speed = Math.sqrt(movementX * movementX + movementZ * movementZ);

        if (speed < MINIMUM_SPEED) {
            primed = false;
            return;
        }

        double directionX = movementX / speed;
        double directionZ = movementZ / speed;

        if (!primed) {
            lastYaw = player.yaw;
            lastDirectionX = directionX;
            lastDirectionZ = directionZ;
            primed = true;
            return;
        }

        float yawChange = Math.abs(wrapDegrees(player.yaw - lastYaw));
        double directionChange = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0,
                lastDirectionX * directionX + lastDirectionZ * directionZ))));

        boolean flagged = yawChange > MINIMUM_YAW_CHANGE && directionChange < yawChange * FOLLOW_RATIO;

        lastYaw = player.yaw;
        lastDirectionX = directionX;
        lastDirectionZ = directionZ;

        if (!flagged) {
            return;
        }

        long now = System.currentTimeMillis();
        if (hits == 0 || now - firstHitAt > WINDOW_MS) {
            hits = 0;
            firstHitAt = now;
        }
        hits++;
        CombatEvidence.report(player, "rotation_movement", 1.0);

        if (hits >= FLAG_HITS) {
            flag(String.format("yaw=%.1f movement=%.1f", yawChange, directionChange));
            hits = 0;
            firstHitAt = now;
        }
    }

    private float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private boolean exempt() {
        return player.disableDeneme
                || player.inVehicle()
                || player.isGliding
                || player.isSwimming
                || player.isClimbing
                || player.packetStateData.lastPacketWasTeleport
                || GeyserUtil.isBedrockPlayer(player.user.getUUID());
    }
}
