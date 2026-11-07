package ac.deneme.anticheat.predictionengine.predictions.rideable;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.predictionengine.predictions.PredictionEngineNormal;
import ac.deneme.anticheat.predictionengine.predictions.input.Input;
import ac.deneme.anticheat.utils.data.VectorData;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineRideableNormal extends PredictionEngineNormal {
    private final Input movementVector;

    @Override
    public void addJumpsToPossibilities(DenemePlayer player, Set<VectorData> existingVelocities) {
        PredictionEngineRideableUtils.handleJumps(player, existingVelocities);
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(DenemePlayer player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(movementVector, player, possibleVectors, speed);
    }

}
