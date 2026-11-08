package ac.deneme.anticheat.checks.type;

import ac.deneme.anticheat.utils.anticheat.update.VehiclePositionUpdate;

public interface VehicleListener {
    void process(final VehiclePositionUpdate vehicleUpdate);
}
