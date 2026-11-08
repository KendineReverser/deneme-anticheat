package ac.deneme.anticheat.utils.data.packetentity;

import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntitySizeable extends PacketEntity {

    public int size = 1;

    public PacketEntitySizeable(DenemePlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
    }
}
