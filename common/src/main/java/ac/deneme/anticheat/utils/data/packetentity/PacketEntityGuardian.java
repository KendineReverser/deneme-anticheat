package ac.deneme.anticheat.utils.data.packetentity;

import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntityGuardian extends PacketEntity {

    public boolean isElder;

    public PacketEntityGuardian(DenemePlayer player, UUID uuid, EntityType type, double x, double y, double z, boolean isElder) {
        super(player, uuid, type, x, y, z);
        this.isElder = isElder;
    }
}
