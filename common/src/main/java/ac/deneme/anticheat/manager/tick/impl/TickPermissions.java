package ac.deneme.anticheat.manager.tick.impl;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.manager.config.BaseConfigManager;
import ac.deneme.anticheat.manager.tick.Tickable;
import ac.deneme.anticheat.player.DenemePlayer;

public class TickPermissions implements Tickable {

    @Override
    public void tick() {
        BaseConfigManager config = DenemeAPI.INSTANCE.getConfigManager();
        int interval = config.getUpdatePermissionTicks();
        if (interval <= 0 || DenemeAPI.INSTANCE.getTickManager().currentTick % interval != 0) return;

        for (DenemePlayer player : DenemeAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.updatePermissions();
        }
    }
}
