package ac.deneme.anticheat.command.commands;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.command.BuildableCommand;
import ac.deneme.anticheat.command.CloudCommandService;
import ac.deneme.anticheat.command.requirements.PlayerSenderRequirement;
import ac.deneme.anticheat.manager.AlertManagerImpl;
import ac.deneme.anticheat.manager.datastore.PlayerToggleStore;
import ac.deneme.anticheat.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.platform.api.sender.Sender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

public class DenemeAlertToggle implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(subscribeCommand(commandManager, "denemeacalerton", true));
        commandManager.command(subscribeCommand(commandManager, "denemeacalertoff", false));
    }

    private @NotNull Command<Sender> subscribeCommand(CommandManager<Sender> commandManager, String name, boolean enabled) {
        return commandManager.commandBuilder(name)
                .permission("deneme.alerts")
                .apply(CloudCommandService.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.INSTANCE))
                .handler(context -> handle(context, enabled))
                .build();
    }

    private void handle(@NotNull CommandContext<Sender> context, boolean enabled) {
        PlatformPlayer player = context.sender().getPlatformPlayer();
        if (player == null) return;

        AlertManagerImpl alertManager = DenemeAPI.INSTANCE.getAlertManager();
        if (alertManager.hasAlertsEnabled(player) == enabled) return;

        alertManager.setAlertsEnabled(player, enabled, false);
        DenemeAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore()
                .applyUserToggle(player.getUniqueId(), PlayerToggleStore.KEY_ALERTS, enabled);
    }
}
