package ac.deneme.anticheat.checks.impl.timer;

import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

@CheckData(name = "VehicleTimer", stableKey = "deneme.timer.vehicle", description = "Sent vehicle movement packets faster than the expected client tick rate", setback = 10)
public class VehicleTimer extends Timer {
    private boolean isDummy = false;

    public VehicleTimer(DenemePlayer player) {
        super(player);
    }

    @Override
    public boolean shouldCountPacketForTimer(PacketTypeCommon packetType) {

        if (player.packetStateData.lastPacketWasTeleport) return false;

        if (packetType == PacketType.Play.Client.VEHICLE_MOVE) {
            isDummy = false;
            return true;
        }

        if (packetType == PacketType.Play.Client.STEER_VEHICLE) {
            if (isDummy) {
                return true;
            }
            isDummy = true;
        }

        return false;
    }
}
