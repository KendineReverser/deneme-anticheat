package ac.deneme.anticheat.checks.impl.aim;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.aim.processor.AimProcessor;
import ac.deneme.anticheat.checks.type.RotationListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.RotationUpdate;
import ac.deneme.anticheat.utils.reflection.GeyserUtil;

@CheckData(name = "AimSensitivity", stableKey = "deneme.aim.sensitivity", description = "Sent rotations that no mouse sensitivity can produce", decay = 0.01, experimental = true)
public class AimSensitivity extends Check implements RotationListener {

    private static final int WINDOW = 120;
    private static final int MIN_SAMPLES = 70;
    private static final int EVALUATION_INTERVAL = 40;
    private static final double MIN_VALID_DELTA = 0.05;
    private static final double MAX_VALID_DELTA = 12.0;
    private static final double MAX_VANILLA_SENSITIVITY = 1.05;
    private static final double ALIGNMENT_TOLERANCE = 0.1;
    private static final double MIN_EXPLAINED_FRACTION = 0.55;
    private static final double MAX_EXPLAINED_FRACTION = 0.995;
    private static final int STREAK_TO_FLAG = 2;

    private final double[] deltas = new double[WINDOW];

    private int index;
    private int size;
    private int sinceEvaluation;
    private double unexplainedStreak;
    private double explainedFraction = 1.0;
    private double divisor;
    private double sensitivity;

    public AimSensitivity(DenemePlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (exempt()) {
            reset();
            return;
        }

        double delta = Math.max(rotationUpdate.deltaYawABS(), rotationUpdate.deltaPitchABS());
        if (delta < MIN_VALID_DELTA || delta > MAX_VALID_DELTA) return;

        deltas[index] = delta;
        index = (index + 1) % WINDOW;
        if (size < WINDOW) size++;

        if (++sinceEvaluation < EVALUATION_INTERVAL || size < MIN_SAMPLES) return;
        sinceEvaluation = 0;
        evaluate();
    }

    private void evaluate() {
        AimProcessor processor = player.checkManager.get(AimProcessor.class);
        divisor = processor.modeYaw;
        sensitivity = processor.sensitivityYaw;

        if (divisor <= 0.0) return;

        int explained = 0;
        for (int i = 0; i < size; i++) {
            double ratio = deltas[i] / divisor;
            if (Math.abs(ratio - Math.rint(ratio)) < ALIGNMENT_TOLERANCE) explained++;
        }

        explainedFraction = (double) explained / size;

        boolean implausible = explainedFraction < MIN_EXPLAINED_FRACTION
                || sensitivity > MAX_VANILLA_SENSITIVITY
                || sensitivity < -ALIGNMENT_TOLERANCE;

        if (implausible) {
            unexplainedStreak++;
        } else if (explainedFraction > MAX_EXPLAINED_FRACTION) {
            unexplainedStreak = Math.max(0, unexplainedStreak - 1);
        }

        if (unexplainedStreak >= STREAK_TO_FLAG) {
            flag(String.format("unexplained=%.0f%% divisor=%.6f sensitivity=%.3f samples=%d",
                    (1.0 - explainedFraction) * 100.0, divisor, sensitivity, size));
            unexplainedStreak = 0;
        } else {
            reward();
        }
    }

    private boolean exempt() {
        return player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.vehicleData.wasVehicleSwitch
                || player.compensatedEntities.self.getRiding() != null
                || GeyserUtil.isBedrockPlayer(player.user.getUUID());
    }

    private void reset() {
        index = 0;
        size = 0;
        sinceEvaluation = 0;
        unexplainedStreak = 0;
    }
}
