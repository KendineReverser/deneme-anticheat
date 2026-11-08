package ac.deneme.anticheat.platform.fabric.manager;

import ac.deneme.anticheat.platform.api.manager.MessagePlaceHolderManager;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FabricMessagePlaceHolderManager implements MessagePlaceHolderManager {

    @Override
    public @NotNull String replacePlaceholders(@Nullable PlatformPlayer player, @NotNull String string) {
        return string;
    }
}
