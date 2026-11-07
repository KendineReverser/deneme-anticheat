package ac.deneme.anticheat.manager.init.start;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;

public class ExemptOnlinePlayersOnReload implements StartableInitable {

    @Override
    public void start() {
        for (PlatformPlayer player : DenemeAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player.getNative());
            DenemeAPI.INSTANCE.getPlayerDataManager().exemptUser(user);
        }
    }
}
