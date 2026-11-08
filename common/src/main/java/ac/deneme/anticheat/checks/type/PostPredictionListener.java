package ac.deneme.anticheat.checks.type;

import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;

public interface PostPredictionListener {
    void onPredictionComplete(PredictionComplete predictionComplete);
}
