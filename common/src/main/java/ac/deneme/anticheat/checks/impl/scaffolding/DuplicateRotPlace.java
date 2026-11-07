package ac.deneme.anticheat.checks.impl.scaffolding;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.aim.processor.AimProcessor;
import ac.deneme.anticheat.checks.type.BlockPlaceCheck;
import ac.deneme.anticheat.checks.type.PostFlyingBlockPlaceListener;
import ac.deneme.anticheat.checks.type.RotationListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockPlace;
import ac.deneme.anticheat.utils.anticheat.update.RotationUpdate;

@CheckData(name = "DuplicateRotPlace", stableKey = "deneme.scaffolding.duplicate_rot_place", description = "Repeated the same rotation delta while placing blocks", experimental = true)
public class DuplicateRotPlace extends BlockPlaceCheck implements RotationListener, PostFlyingBlockPlaceListener {
    private static final Verbose V = Verbose.of("x={f64} xdots={f64} y={f64}");

    private float deltaX, deltaY;
    private float lastPlacedDeltaX;
    private double lastPlacedDeltaDotsX;
    private double deltaDotsX;
    private boolean rotated = false;

    public DuplicateRotPlace(DenemePlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        deltaX = rotationUpdate.deltaYawABS();
        deltaY = rotationUpdate.deltaPitchABS();
        deltaDotsX = player.checkManager.get(AimProcessor.class).deltaDotsYaw;
        rotated = true;
    }

    @Override
    public void onPostFlyingBlockPlace(BlockPlace place) {
        if (rotated && !player.inVehicle()) {
            if (deltaX > 2) {
                float xDiff = Math.abs(deltaX - lastPlacedDeltaX);
                double xDiffDots = Math.abs(deltaDotsX - lastPlacedDeltaDotsX);

                if (xDiff < 0.0001) {
                    flag(V.write(verbose()).f64(xDiff).f64(xDiffDots).f64(deltaY));
                } else {
                    reward();
                }
            } else {
                reward();
            }
            this.lastPlacedDeltaX = deltaX;
            this.lastPlacedDeltaDotsX = deltaDotsX;
            rotated = false;
        }
    }
}
