package ac.deneme.anticheat.platform.fabric.mc1205;

import ac.deneme.anticheat.platform.fabric.DenemeACFabricIntermediaryLoaderPlugin;
import ac.deneme.anticheat.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import ac.deneme.anticheat.platform.fabric.mc1194.Fabric1190PlatformServer;
import ac.deneme.anticheat.platform.fabric.mc1194.DenemeACFabric1190LoaderPlugin;
import ac.deneme.anticheat.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import ac.deneme.anticheat.platform.fabric.mc1205.convert.Fabric1200MessageUtil;
import ac.deneme.anticheat.platform.fabric.mc1205.convert.Fabric1205ConversionUtil;
import ac.deneme.anticheat.platform.fabric.mc1194.entity.Fabric1194DenemeEntity;
import ac.deneme.anticheat.platform.fabric.mc1205.player.Fabric1202PlatformPlayer;
import ac.deneme.anticheat.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import ac.deneme.anticheat.platform.fabric.player.FabricPlatformPlayerFactory;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;

public class DenemeACFabric1200LoaderPlugin extends DenemeACFabric1190LoaderPlugin {

    public DenemeACFabric1200LoaderPlugin() {
        super(
                DenemeACFabricIntermediaryLoaderPlugin::createCommandArguments,
                new FabricPlatformPlayerFactory(
                        FabricPacketEventsAPI.getServerAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_1)
                                ? Fabric1202PlatformPlayer::new : Fabric1170PlatformPlayer::new,
                        Fabric1194DenemeEntity::new,
                        Fabric1193PlatformInventory::new
                ),
                FabricPacketEventsAPI.getServerAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_2)
                        ? new Fabric1203PlatformServer() : new Fabric1190PlatformServer(),
                new Fabric1200MessageUtil(),
                FabricPacketEventsAPI.getServerAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_4)
                        ? new Fabric1205ConversionUtil() : new Fabric1140ConversionUtil()
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_20_5;
    }
}
