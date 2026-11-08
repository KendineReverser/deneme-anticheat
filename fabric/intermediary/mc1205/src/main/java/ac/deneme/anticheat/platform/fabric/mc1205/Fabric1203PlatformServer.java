package ac.deneme.anticheat.platform.fabric.mc1205;

import ac.deneme.anticheat.platform.api.sender.Sender;
import ac.deneme.anticheat.platform.fabric.DenemeACFabricIntermediaryLoaderPlugin;
import ac.deneme.anticheat.platform.fabric.mc1194.Fabric1190PlatformServer;
import net.minecraft.commands.CommandSourceStack;

public class Fabric1203PlatformServer extends Fabric1190PlatformServer {

    @Override
    public double getTPS() {
        return Math.min(1000.0 / DenemeACFabricIntermediaryLoaderPlugin.FABRIC_SERVER.getCurrentSmoothedTickTime(), DenemeACFabricIntermediaryLoaderPlugin.FABRIC_SERVER.tickRateManager().tickrate());
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSourceStack commandSource = DenemeACFabricIntermediaryLoaderPlugin.LOADER.getFabricSenderFactory().unwrap(sender);
        DenemeACFabricIntermediaryLoaderPlugin.FABRIC_SERVER.getCommands().performPrefixedCommand(commandSource, command);
    }
}
