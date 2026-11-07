package ac.deneme.anticheat.platform.fabric.mc1171;

import ac.deneme.anticheat.platform.fabric.AbstractFabricPlatformServer;
import ac.deneme.anticheat.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.deneme.anticheat.platform.fabric.DenemeACFabricIntermediaryLoaderPlugin;
import ac.deneme.anticheat.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import ac.deneme.anticheat.platform.fabric.mc1161.Fabric1140PlatformServer;
import ac.deneme.anticheat.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import ac.deneme.anticheat.platform.fabric.mc1171.entity.Fabric1170DenemeEntity;
import ac.deneme.anticheat.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import ac.deneme.anticheat.platform.fabric.mc1161.util.convert.Fabric1161MessageUtil;
import ac.deneme.anticheat.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.deneme.anticheat.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.deneme.anticheat.platform.fabric.utils.message.IFabricMessageUtil;
import ac.deneme.anticheat.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;


public class DenemeACFabric1170LoaderPlugin extends DenemeACFabricIntermediaryLoaderPlugin {

    public DenemeACFabric1170LoaderPlugin() {
        this(DenemeACFabricIntermediaryLoaderPlugin::createCommandArguments,
                new FabricPlatformPlayerFactory(
                        Fabric1170PlatformPlayer::new,
                        Fabric1170DenemeEntity::new,
                        Fabric1161PlatformInventory::new
                ),
                PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_17)
                        ? new Fabric1171PlatformServer() : new Fabric1140PlatformServer(),
                new Fabric1161MessageUtil(),
                new Fabric1140ConversionUtil()
        );
    }

    protected DenemeACFabric1170LoaderPlugin(LazyHolder<CloudPlatformCommandArguments> commandArguments,
                                           FabricPlatformPlayerFactory playerFactory,
                                           AbstractFabricPlatformServer platformServer,
                                           IFabricMessageUtil fabricMessageUtil,
                                           IFabricConversionUtil fabricConversionUtil) {
        super(
                commandArguments,
                playerFactory,
                platformServer,
                fabricMessageUtil,
                fabricConversionUtil
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_17_1;
    }
}
