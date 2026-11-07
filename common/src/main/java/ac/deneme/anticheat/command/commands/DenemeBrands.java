package ac.deneme.anticheat.command.commands;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.command.BuildableCommand;
import ac.deneme.anticheat.manager.AlertManagerImpl;
import ac.deneme.anticheat.manager.datastore.PlayerToggleStore;
import ac.deneme.anticheat.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DenemeBrands implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("denemeac", "dac")
                        .literal("brands", Description.of("Toggle brands for the sender"))
                        .permission("deneme.brand")
                        .handler(this::handleBrands)
        );
    }

    private void handleBrands(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            PlatformPlayer player = Objects.requireNonNull(context.sender().getPlatformPlayer(), "player");
            AlertManagerImpl am = DenemeAPI.INSTANCE.getAlertManager();
            boolean newState = !am.hasBrandsEnabled(player);
            am.setBrandsEnabled(player, newState, false);
            DenemeAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore()
                    .applyUserToggle(player.getUniqueId(), PlayerToggleStore.KEY_BRANDS, newState);
        } else if (sender.isConsole()) {
            DenemeAPI.INSTANCE.getAlertManager().toggleConsoleBrands();
        }
    }
}
