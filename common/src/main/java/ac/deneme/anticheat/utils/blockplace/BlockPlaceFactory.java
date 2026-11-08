package ac.deneme.anticheat.utils.blockplace;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockPlace;

public interface BlockPlaceFactory {
    void applyBlockPlaceToWorld(DenemePlayer player, BlockPlace place);
}
