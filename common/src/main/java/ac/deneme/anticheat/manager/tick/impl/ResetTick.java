package ac.deneme.anticheat.manager.tick.impl;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.manager.tick.Tickable;
import ac.deneme.anticheat.player.DenemePlayer;

public class ResetTick implements Tickable {
    @Override
    public void tick() {
        for (DenemePlayer player : DenemeAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.packetEntityReplication.tickStartTick();
        }
    }
}
