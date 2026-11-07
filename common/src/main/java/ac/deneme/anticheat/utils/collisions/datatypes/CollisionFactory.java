package ac.deneme.anticheat.utils.collisions.datatypes;

import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public interface CollisionFactory {
    CollisionBox fetch(DenemePlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z);
}
