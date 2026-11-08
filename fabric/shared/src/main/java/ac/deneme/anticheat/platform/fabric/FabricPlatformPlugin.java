package ac.deneme.anticheat.platform.fabric;

import ac.deneme.anticheat.platform.api.PlatformPlugin;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FabricPlatformPlugin implements PlatformPlugin {
    private final @NotNull ModContainer modContainer;

    @Contract(pure = true)
    public FabricPlatformPlugin(@NotNull ModContainer modContainer) {
        this.modContainer = Objects.requireNonNull(modContainer, "modContainer");
    }

    @Override
    public boolean isEnabled() {

        return true;
    }

    @Override
    public String getName() {

        return modContainer.getMetadata().getId();
    }

    @Override
    public String getVersion() {

        return modContainer.getMetadata().getVersion().getFriendlyString();
    }
}
