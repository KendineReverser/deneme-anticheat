package ac.deneme.anticheat.checks.impl.aim;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.RotationListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimModulo360", stableKey = "deneme.aim.modulo_360", description = "Produced rotation jumps no mouse movement can create", decay = 0.005)
public class AimModulo360 extends Check implements RotationListener {

    private static final double INSTANT_JUMP = 320.0;
    private static final double CALM_BASELINE = 30.0;
    private static final long REPEAT_WINDOW_MS = 2500L;
    private static final double FIRST_EVIDENCE = 1.25;
    private static final double REPEAT_EVIDENCE = 1.75;
    private static final double FLAG_EVIDENCE = 3.0;
    private static final double DECAY_PER_ROTATION = 0.05;

    private float lastDeltaYaw;
    private long lastAnomalyAt;
    private double evidence;

    public AimModulo360(DenemePlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {

        if (player.packetStateData.lastPacketWasTeleport || player.vehicleData.wasVehicleSwitch
                || player.packetStateData.horseInteractCausedForcedRotation) {
            lastDeltaYaw = rotationUpdate.deltaYaw();
            return;
        }

        double deltaYaw = Math.abs(rotationUpdate.deltaYaw());
        double baseline = Math.abs(lastDeltaYaw);

        boolean anomaly = deltaYaw > INSTANT_JUMP && baseline < CALM_BASELINE
                && player.yaw < 360 && player.yaw > -360;

        if (anomaly) {
            long now = System.currentTimeMillis();
            boolean repeated = lastAnomalyAt != 0 && now - lastAnomalyAt <= REPEAT_WINDOW_MS;

            evidence += repeated ? REPEAT_EVIDENCE : FIRST_EVIDENCE;
            lastAnomalyAt = now;

            if (evidence >= FLAG_EVIDENCE) {
                flag(String.format("delta=%.1f baseline=%.1f evidence=%.2f", deltaYaw, baseline, evidence));
                evidence -= FLAG_EVIDENCE;
            }
        } else {
            evidence = Math.max(0.0, evidence - DECAY_PER_ROTATION);
            reward();
        }

        lastDeltaYaw = rotationUpdate.deltaYaw();
    }
}
