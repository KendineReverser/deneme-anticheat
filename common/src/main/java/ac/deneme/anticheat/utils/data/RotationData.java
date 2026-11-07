package ac.deneme.anticheat.utils.data;

import ac.deneme.anticheat.utils.math.DenemeMath;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Contract;

public record RotationData(
        float yaw, float pitch,
        boolean relativeYaw, boolean relativePitch,
        int transaction) {

    public static RotationData fromServer(ClientVersion version, float yaw, float pitch, boolean relativeYaw, boolean relativePitch, int transaction) {
        if (!relativePitch) {

            if (version.isOlderThan(ClientVersion.V_1_21_9)) pitch %= 360.0F;
            pitch = DenemeMath.clamp(pitch, -90.0F, 90.0F);
        }

        return new RotationData(yaw, pitch, relativeYaw, relativePitch, transaction);
    }

    @Contract(pure = true)
    public boolean allowRotation(float yaw, float pitch) {

        return (this.relativeYaw || this.yaw == yaw) && (this.relativePitch || this.pitch == pitch);
    }
}
