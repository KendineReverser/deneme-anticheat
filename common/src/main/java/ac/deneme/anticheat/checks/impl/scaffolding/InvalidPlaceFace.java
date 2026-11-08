package ac.deneme.anticheat.checks.impl.scaffolding;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.BlockPlaceCheck;
import ac.deneme.anticheat.checks.type.BlockPlaceListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

@CheckData(name = "InvalidPlaceFace", stableKey = "deneme.scaffolding.invalid_place_b", description = "Sent impossible block face id")
public class InvalidPlaceFace extends BlockPlaceCheck implements BlockPlaceListener {
    private static final Verbose V = Verbose.of("direction={sint}");

    public InvalidPlaceFace(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (place.getFaceId() == 255 && PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8)) {
            return;
        }

        if (place.getFaceId() < 0 || place.getFaceId() > 5) {

            int direction = place.getFaceId();
            if (flag(V.write(verbose()).sint(direction)) && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
