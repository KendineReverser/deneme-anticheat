package ac.deneme.anticheat.checks.impl.aim;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.RotationListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimDuplicateLook", stableKey = "deneme.aim.duplicate_look", description = "Sent a duplicate rotation update without changing look direction")
public class AimDuplicateLook extends Check implements RotationListener {
    private boolean exempt;

    public AimDuplicateLook(DenemePlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate || player.compensatedEntities.self.getRiding() != null) {
            exempt = true;
            return;
        }

        if (exempt) { // Exempt for a tick on teleport
            exempt = false;
            return;
        }

        if (rotationUpdate.oldYaw() == rotationUpdate.newYaw() && rotationUpdate.oldPitch() == rotationUpdate.newPitch()) {
            flag();
        }
    }
}
