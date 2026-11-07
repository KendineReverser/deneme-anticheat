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
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DenemeVerbose implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("denemeac", "dac")
                        .literal("verbose")
                        .permission("deneme.verbose")
                        .handler(this::handleVerbose)
        );
    }

    private void handleVerbose(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            PlatformPlayer player = Objects.requireNonNull(context.sender().getPlatformPlayer(), "player");
            AlertManagerImpl am = DenemeAPI.INSTANCE.getAlertManager();
            boolean newState = !am.hasVerboseEnabled(player);
            am.setVerboseEnabled(player, newState, false);
            PlayerToggleStore toggles = DenemeAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore();
            toggles.applyUserToggle(player.getUniqueId(), PlayerToggleStore.KEY_VERBOSE, newState);

            if (newState) toggles.applyUserToggle(player.getUniqueId(), PlayerToggleStore.KEY_ALERTS, true);
        } else if (sender.isConsole()) {
            DenemeAPI.INSTANCE.getAlertManager().toggleConsoleVerbose();
        }
    }
}
