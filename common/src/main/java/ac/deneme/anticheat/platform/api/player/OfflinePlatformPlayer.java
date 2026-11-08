package ac.deneme.anticheat.platform.api.player;

import ac.deneme.anticheat.api.DenemeIdentity;

public interface OfflinePlatformPlayer extends DenemeIdentity {

    boolean isOnline();

    String getName();
}
