package ac.deneme.anticheat.checks.impl.scaffolding;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.BlockPlaceCheck;
import ac.deneme.anticheat.checks.type.BlockPlaceListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockPlace;
import ac.deneme.anticheat.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3f;

@CheckData(name = "FabricatedPlace", stableKey = "deneme.scaffolding.fabricated_place", description = "Sent out of bounds cursor position")
public class FabricatedPlace extends BlockPlaceCheck implements BlockPlaceListener {
    private static final Verbose V = Verbose.of("cursor={cursor} limit={f64:%.16f}");

    /**
     * MAX_DOUBLE_ERROR:
     * Represents the maximum possible floating-point arithmetic error that can occur
     * when calculating vectors at the Minecraft World Border (30,000,000 blocks).
     *
     * Math.ulp(30,000,000.0) is approx 3.72E-9.
     * We multiply by 2.0 to account for subtraction compounding logic in raytracing.
     *
     * This constant is safe to use everywhere; 7.450580596923828E-9 is physically indistinguishable from zero.
     */
    private static final double MAX_DOUBLE_ERROR = Math.ulp(30_000_000.0) * 2.0;

    /**
     * FLOAT_STEP_AT_ONE:
     * Represents the resolution of a Float at the value 1.0 (and 1.5).
     * Math.ulp(1.0f) is approx 1.1920929E-7.
     *
     * When checking the upper bound (1.0), we must allow this variance because a Double
     * slightly larger than 1.0 might get cast/snapped to the next available Float step.
     */
    private static final double FLOAT_STEP_AT_ONE = Math.ulp(1.0f);

    public FabricatedPlace(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        Vector3f cursor = place.cursor;
        if (cursor == null) return;

        boolean isExtended = Materials.isShapeExceedsCube(place.getPlacedAgainstMaterial())
                || place.getPlacedAgainstMaterial() == StateTypes.LECTERN;

        double maxBound = isExtended ? 1.5 : 1.0;
        double minBound = 1.0 - maxBound;

        if (cursor.getX() < minBound - MAX_DOUBLE_ERROR ||
                cursor.getY() < minBound - MAX_DOUBLE_ERROR ||
                cursor.getZ() < minBound - MAX_DOUBLE_ERROR) {

            double limit = minBound - MAX_DOUBLE_ERROR;
            var buf = V.write(verbose()).cursor(cursor.x, cursor.y, cursor.z).f64(limit);
            if (flag(buf) && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
            return;
        }

        double upperTolerance = FLOAT_STEP_AT_ONE;

        if (cursor.getX() > maxBound + upperTolerance ||
                cursor.getY() > maxBound + upperTolerance ||
                cursor.getZ() > maxBound + upperTolerance) {

            double limit = maxBound + upperTolerance;
            var buf = V.write(verbose()).cursor(cursor.x, cursor.y, cursor.z).f64(limit);
            if (flag(buf) && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
