package ac.deneme.anticheat.utils.data.packetentity;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.data.attribute.ValuedAttribute;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntityRideable extends PacketEntity {

    public boolean hasSaddle = false;
    public int boostTimeMax = 0;
    public int currentBoostTime = 0;

    public PacketEntityRideable(DenemePlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
        setAttribute(Attributes.STEP_HEIGHT, 1.0f);
        trackAttribute(ValuedAttribute.ranged(Attributes.MOVEMENT_SPEED, 0.1f, 0, 1024));
    }
}
