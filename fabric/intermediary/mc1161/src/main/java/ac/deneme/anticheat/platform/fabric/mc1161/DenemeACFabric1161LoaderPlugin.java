package ac.deneme.anticheat.platform.fabric.mc1161;

import ac.deneme.anticheat.platform.fabric.AbstractFabricPlatformServer;
import ac.deneme.anticheat.platform.fabric.DenemeACFabricIntermediaryLoaderPlugin;
import ac.deneme.anticheat.platform.fabric.mc1161.entity.Fabric1161DenemeEntity;
import ac.deneme.anticheat.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import ac.deneme.anticheat.platform.fabric.mc1161.player.Fabric1161PlatformPlayer;
import ac.deneme.anticheat.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import ac.deneme.anticheat.platform.fabric.mc1161.util.convert.Fabric1161MessageUtil;
import ac.deneme.anticheat.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.deneme.anticheat.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.deneme.anticheat.platform.fabric.utils.message.IFabricMessageUtil;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class DenemeACFabric1161LoaderPlugin extends DenemeACFabricIntermediaryLoaderPlugin {

    public DenemeACFabric1161LoaderPlugin() {
        this(
            new FabricPlatformPlayerFactory(
                Fabric1161PlatformPlayer::new,
                Fabric1161DenemeEntity::new,
                Fabric1161PlatformInventory::new
            ),
            new Fabric1140PlatformServer(),
            new Fabric1161MessageUtil(),
            new Fabric1140ConversionUtil()
        );
    }

    protected DenemeACFabric1161LoaderPlugin(
            FabricPlatformPlayerFactory playerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil
    ) {
        super(DenemeACFabricIntermediaryLoaderPlugin::createCommandArguments,
            playerFactory,
            platformServer,
            fabricMessageUtil,
            fabricConversionUtil
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_16_1;
    }
}
