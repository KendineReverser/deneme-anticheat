package ac.deneme.anticheat.predictionengine.predictions.input;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.math.Vector3dm;

public record FloatInput(float sideways, float vertical, float forward) implements Input {
    @Override
    public Vector3dm vector() {
        return new Vector3dm(sideways, vertical, forward);
    }

    @Override
    public Input normalize(DenemePlayer player) {

        return this;
    }
}
