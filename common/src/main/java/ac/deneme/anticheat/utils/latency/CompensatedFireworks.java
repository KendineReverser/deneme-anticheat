package ac.deneme.anticheat.utils.latency;

import ac.deneme.anticheat.checks.DenemeProcessor;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;

import java.util.HashSet;
import java.util.Set;

public class CompensatedFireworks extends DenemeProcessor implements PostPredictionListener {

    private final Set<Integer> activeFireworks = new HashSet<>();
    private final Set<Integer> fireworksToRemoveNextTick = new HashSet<>();

    public CompensatedFireworks(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {

        activeFireworks.removeAll(fireworksToRemoveNextTick);
        fireworksToRemoveNextTick.clear();
    }

    public boolean hasFirework(int entityId) {
        return activeFireworks.contains(entityId);
    }

    public void addNewFirework(int entityID) {
        activeFireworks.add(entityID);
    }

    public void removeFirework(int entityID) {
        if (activeFireworks.contains(entityID)) {
            fireworksToRemoveNextTick.add(entityID);
        }
    }

    public int getMaxFireworksAppliedPossible() {
        return activeFireworks.size();
    }
}
