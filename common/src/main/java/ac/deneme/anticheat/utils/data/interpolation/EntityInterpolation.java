package ac.deneme.anticheat.utils.data.interpolation;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import ac.deneme.anticheat.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath;
import org.jetbrains.annotations.Nullable;

public interface EntityInterpolation {

    void begin(@Nullable PositionPath path, boolean relative, boolean hasPos, double packetX, double packetY, double packetZ, @Nullable Float yaw, @Nullable Float pitch, boolean positionSync, int transaction);

    void confirm(int transaction);

    void tick(boolean tickingReliably);

    void reset(SimpleCollisionBox position);

    SimpleCollisionBox position();

    default SimpleCollisionBox hitbox(DenemePlayer player, PacketEntity entity) {
        SimpleCollisionBox hitbox = position();
        GetBoundingBox.expandBoundingBoxByEntityDimensions(hitbox, player, entity);
        return hitbox;
    }

    default void initializeRotation(float yaw, float pitch) {
    }

}
