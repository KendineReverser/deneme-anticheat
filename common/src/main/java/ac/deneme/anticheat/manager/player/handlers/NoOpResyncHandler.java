package ac.deneme.anticheat.manager.player.handlers;

import ac.deneme.anticheat.api.handler.ResyncHandler;

public enum NoOpResyncHandler implements ResyncHandler {
    INSTANCE;

    @Override
    public void resync(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ) {}

    @Override public void resyncPosition(int x, int y, int z, int sequence) {}
}
