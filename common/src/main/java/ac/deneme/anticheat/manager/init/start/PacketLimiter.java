package ac.deneme.anticheat.manager.init.start;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.player.DenemePlayer;

public class PacketLimiter implements StartableInitable {
    @Override
    public void start() {
        DenemeAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(DenemeAPI.INSTANCE.getDenemePlugin(), () -> {
            for (DenemePlayer player : DenemeAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                // Avoid concurrent reading on an integer as it's results are unknown
                player.cancelledPackets.set(0);
            }
        }, 1, 20);
    }
}
