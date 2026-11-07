package ac.deneme.anticheat.platform.fabric.initables;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.manager.init.start.AbstractTickEndEvent;
import ac.deneme.anticheat.platform.fabric.FabricServerEvents;
import ac.deneme.anticheat.player.DenemePlayer;

public class FabricTickEndEvent extends AbstractTickEndEvent {

    @Override
    public void start() {
        if (!super.shouldInjectEndTick()) {
            return;
        }

        FabricServerEvents.onEndTick(server -> tickAllPlayers());
    }

    private void tickAllPlayers() {
        for (DenemePlayer player : DenemeAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player.disableDeneme) continue;
            super.onEndOfTick(player, true);
        }
    }
}
