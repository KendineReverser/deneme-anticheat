package ac.deneme.anticheat.platform.fabric.mc1194;

import ac.deneme.anticheat.platform.fabric.AbstractFabricPlatformServer;
import ac.deneme.anticheat.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.deneme.anticheat.platform.fabric.DenemeACFabricIntermediaryLoaderPlugin;
import ac.deneme.anticheat.platform.fabric.mc1171.DenemeACFabric1170LoaderPlugin;
import ac.deneme.anticheat.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import ac.deneme.anticheat.platform.fabric.mc1194.convert.Fabric1190MessageUtil;
import ac.deneme.anticheat.platform.fabric.mc1194.entity.Fabric1194DenemeEntity;
import ac.deneme.anticheat.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import ac.deneme.anticheat.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import ac.deneme.anticheat.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import ac.deneme.anticheat.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.deneme.anticheat.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.deneme.anticheat.platform.fabric.utils.message.IFabricMessageUtil;
import ac.deneme.anticheat.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class DenemeACFabric1190LoaderPlugin extends DenemeACFabric1170LoaderPlugin {

    public DenemeACFabric1190LoaderPlugin() {
        this(
            DenemeACFabricIntermediaryLoaderPlugin::createCommandArguments,
            new FabricPlatformPlayerFactory(
                    Fabric1170PlatformPlayer::new,
                    Fabric1194DenemeEntity::new,
                    PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_19_2)
                            ? Fabric1193PlatformInventory::new : Fabric1161PlatformInventory::new
            ),
            new Fabric1190PlatformServer(),
            new Fabric1190MessageUtil(),
            new Fabric1140ConversionUtil()
        );
    }

    protected DenemeACFabric1190LoaderPlugin(
            LazyHolder<CloudPlatformCommandArguments> commandArguments,
            FabricPlatformPlayerFactory platformPlayerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil) {
        super(commandArguments, platformPlayerFactory, platformServer, fabricMessageUtil, fabricConversionUtil);
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_19_4;
    }
}
