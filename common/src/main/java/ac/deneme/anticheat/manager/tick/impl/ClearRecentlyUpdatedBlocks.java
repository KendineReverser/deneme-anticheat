package ac.deneme.anticheat.manager.tick.impl;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.manager.tick.Tickable;
import ac.deneme.anticheat.player.DenemePlayer;

public class ClearRecentlyUpdatedBlocks implements Tickable {

    private static final int maxTickAge = 2;

    @Override
    public void tick() {
        for (DenemePlayer player : DenemeAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.blockHistory.cleanup(DenemeAPI.INSTANCE.getTickManager().currentTick - maxTickAge);
        }
    }
}
