package ac.deneme.anticheat.checks.impl.aim.processor;

import ac.deneme.anticheat.checks.DenemeProcessor;
import ac.deneme.anticheat.checks.type.RotationListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.RotationUpdate;
import ac.deneme.anticheat.utils.data.Pair;
import ac.deneme.anticheat.utils.lists.RunningMode;
import ac.deneme.anticheat.utils.math.DenemeMath;

public class AimProcessor extends DenemeProcessor implements RotationListener {

    private static final int SIGNIFICANT_SAMPLES_THRESHOLD = 15;
    private static final int TOTAL_SAMPLES_THRESHOLD = 80;
    public double sensitivityYaw;
    public double sensitivityPitch;
    public double divisorYaw;
    public double divisorPitch;
    public double modeYaw, modePitch;
    public double deltaDotsYaw, deltaDotsPitch;
    private final RunningMode yawMode = new RunningMode(TOTAL_SAMPLES_THRESHOLD);
    private final RunningMode pitchMode = new RunningMode(TOTAL_SAMPLES_THRESHOLD);
    private float lastYaw;
    private float lastPitch;

    public AimProcessor(DenemePlayer player) {
        super(player);
    }

    public static double convertToSensitivity(double var13) {
        double var11 = var13 / 0.15F / 8.0D;
        double var9 = Math.cbrt(var11);
        return (var9 - 0.2f) / 0.6f;
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        float deltaYaw = rotationUpdate.deltaYawABS();

        this.divisorYaw = DenemeMath.gcd(deltaYaw, lastYaw);
        if (deltaYaw > 0 && deltaYaw < 5 && divisorYaw > DenemeMath.MINIMUM_DIVISOR) {
            this.yawMode.add(divisorYaw);
            this.lastYaw = deltaYaw;
        }

        float deltaPitch = rotationUpdate.deltaPitchABS();

        this.divisorPitch = DenemeMath.gcd(deltaPitch, lastPitch);

        if (deltaPitch > 0 && deltaPitch < 5 && divisorPitch > DenemeMath.MINIMUM_DIVISOR) {
            this.pitchMode.add(divisorPitch);
            this.lastPitch = deltaPitch;
        }

        if (this.yawMode.size() > SIGNIFICANT_SAMPLES_THRESHOLD) {
            Pair<Double, Integer> modeYaw = this.yawMode.getMode();
            if (modeYaw.second() > SIGNIFICANT_SAMPLES_THRESHOLD) {
                this.modeYaw = modeYaw.first();
                this.sensitivityYaw = convertToSensitivity(this.modeYaw);
            }
        }
        if (this.pitchMode.size() > SIGNIFICANT_SAMPLES_THRESHOLD) {
            Pair<Double, Integer> modePitch = this.pitchMode.getMode();
            if (modePitch.second() > SIGNIFICANT_SAMPLES_THRESHOLD) {
                this.modePitch = modePitch.first();
                this.sensitivityPitch = convertToSensitivity(this.modePitch);
            }
        }

        this.deltaDotsYaw = deltaYaw / modeYaw;
        this.deltaDotsPitch = deltaPitch / modePitch;
    }
}
