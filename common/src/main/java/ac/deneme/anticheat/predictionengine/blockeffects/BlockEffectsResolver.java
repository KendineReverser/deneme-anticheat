package ac.deneme.anticheat.predictionengine.blockeffects;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.math.Vector3dm;

import java.util.List;

public interface BlockEffectsResolver {

    void applyEffectsFromBlocks(DenemePlayer player, Vector3dm clientVelocity, boolean onlyApplyVelocity, List<DenemePlayer.Movement> movements);

}
