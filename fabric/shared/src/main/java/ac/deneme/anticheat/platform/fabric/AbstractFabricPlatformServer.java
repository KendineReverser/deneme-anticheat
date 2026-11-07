package ac.deneme.anticheat.platform.fabric;

import ac.deneme.anticheat.platform.api.PlatformServer;
import ac.deneme.anticheat.platform.api.sender.Sender;
import ac.deneme.anticheat.platform.fabric.player.FabricOfflineProfile;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFabricPlatformServer implements PlatformServer {

    public abstract int getOperatorPermissionLevel();

    public abstract boolean hasPermission(Sender sender, int level);

    @Override
    public String getPlatformImplementationString() {
        return "Fabric " + FabricLoader.getInstance().getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString() + " (MC: " + AbstractDenemeACFabricEntryPoint.server().getServerVersion() + ")";
    }

    @Override
    public Sender getConsoleSender() {
        return AbstractDenemeACFabricEntryPoint.server().createCommandSender();
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
    }

    @Nullable
    public abstract FabricOfflineProfile getProfileByName(String name);
}
