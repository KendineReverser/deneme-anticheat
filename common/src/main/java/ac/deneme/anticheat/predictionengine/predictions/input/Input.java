package ac.deneme.anticheat.predictionengine.predictions.input;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public interface Input {

    Vector3dm vector();

    Input normalize(DenemePlayer player);

    static Input createInput(DenemePlayer player, float sideways, float vertical, float forward) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            return new DoubleInput(sideways, vertical, forward);
        } else {
            return new FloatInput(sideways, vertical, forward);
        }
    }

}
