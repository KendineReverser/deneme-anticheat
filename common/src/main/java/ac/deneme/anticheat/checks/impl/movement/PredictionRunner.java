package ac.deneme.anticheat.checks.impl.movement;

import ac.deneme.anticheat.checks.DenemeProcessor;
import ac.deneme.anticheat.checks.type.PositionListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PositionUpdate;

public class PredictionRunner extends DenemeProcessor implements PositionListener {

    public PredictionRunner(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPositionUpdate(final PositionUpdate positionUpdate) {
        if (!player.inVehicle()) {
            player.movementCheckRunner.processAndCheckMovementPacket(positionUpdate);
        }
    }
}
