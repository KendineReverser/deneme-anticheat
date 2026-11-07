package ac.deneme.anticheat.checks.impl.baritone;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.aim.processor.AimProcessor;
import ac.deneme.anticheat.checks.type.RotationListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.RotationUpdate;
import ac.deneme.anticheat.utils.math.DenemeMath;

@CheckData(name = "Baritone", stableKey = "deneme.baritone.baritone", description = "Detected Baritone like behavior")
public class Baritone extends Check implements RotationListener {
    private static final Verbose V = Verbose.of("divisor={f64}");

    private int verbose;

    public Baritone(DenemePlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        final float deltaPitch = Math.abs(rotationUpdate.newPitch() - rotationUpdate.oldPitch());

        if (rotationUpdate.deltaYaw() == 0 && deltaPitch > 0 && deltaPitch < 1 && Math.abs(rotationUpdate.newPitch()) != 90.0f) {
            AimProcessor processor = player.checkManager.get(AimProcessor.class);
            if (processor.divisorPitch < DenemeMath.MINIMUM_DIVISOR) {
                verbose++;
                if (verbose > 8) {
                    double divisor = AimProcessor.convertToSensitivity(processor.divisorYaw);
                    flag(V.write(verbose()).f64(divisor));
                }
            } else {
                verbose = 0;
            }
        }
    }
}
