package ac.deneme.anticheat.checks.impl.vehicle;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.player.DenemePlayer;

@CheckData(name = "VehicleC", stableKey = "deneme.vehicle.vehicle_control", description = "Moved a vehicle in a way that did not match predicted vehicle control")
public class VehicleC extends Check {
    public VehicleC(DenemePlayer player) {
        super(player);
    }
}
