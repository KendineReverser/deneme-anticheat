package ac.deneme.anticheat.checks.impl.scaffolding;

import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.BlockPlaceCheck;
import ac.deneme.anticheat.checks.type.BlockPlaceListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.util.Vector3f;

@CheckData(name = "InvalidPlaceCursor", stableKey = "deneme.scaffolding.invalid_place_a", description = "Sent invalid cursor position")
public class InvalidPlaceCursor extends BlockPlaceCheck implements BlockPlaceListener {
    public InvalidPlaceCursor(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        Vector3f cursor = place.cursor;
        if (cursor == null) return;
        if (!Float.isFinite(cursor.x) || !Float.isFinite(cursor.y) || !Float.isFinite(cursor.z)) {
            if (flag() && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
