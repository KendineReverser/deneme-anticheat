package ac.deneme.anticheat.command.commands;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.command.BuildableCommand;
import ac.deneme.anticheat.platform.api.command.PlayerSelector;
import ac.deneme.anticheat.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.deneme.anticheat.platform.api.sender.Sender;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DenemeDebug implements BuildableCommand {

    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> denemeCommand = commandManager.commandBuilder("denemeac", "dac");

        Command.Builder<Sender> debugCommand = denemeCommand
                .literal("debug", Description.of("Toggle debug output for a player"))
                .permission("deneme.debug")
                .optional("target", arguments.singlePlayerSelectorParser())
                .handler(this::handleDebug);

        Command.Builder<Sender> consoleDebugCommand = denemeCommand
                .literal("consoledebug", Description.of("Toggle console debug output for a player"))
                .permission("deneme.consoledebug")
                .required("target", arguments.singlePlayerSelectorParser())
                .handler(this::handleConsoleDebug);

        commandManager.command(debugCommand);
        commandManager.command(consoleDebugCommand);
    }

    private void handleDebug(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector playerSelector = context.getOrDefault("target", null);

        DenemePlayer targetDenemePlayer = parseTarget(sender, playerSelector == null ? sender : playerSelector.getSinglePlayer());
        if (targetDenemePlayer == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!"));
            return;
        }

        if (sender.isConsole()) {
            targetDenemePlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        } else if (sender.isPlayer()) {
            DenemePlayer senderDenemePlayer = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(sender.getUniqueId());
            if (senderDenemePlayer == null) {
                sender.sendMessage(MessageUtil.getParsedComponent(sender, "sender-not-found", "%prefix% &cYou cannot be exempt to use this command!"));
                return;
            }
            targetDenemePlayer.checkManager.getDebugHandler().toggleListener(senderDenemePlayer);
        } else {
            sender.sendMessage(MessageUtil.getParsedComponent(sender,
                    "run-as-player-or-console",
                    "%prefix% &cThis command can only be used by players or the console!")
            );
        }
    }

    private void handleConsoleDebug(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector targetName = context.getOrDefault("target", null);

        DenemePlayer denemePlayer = parseTarget(sender, targetName.getSinglePlayer());
        if (denemePlayer == null) return;

        boolean isOutput = denemePlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        String playerName = denemePlayer.user.getProfile().getName();

        Component message = Component.text()
                .append(Component.text("Console output for ", NamedTextColor.GRAY))
                .append(Component.text(playerName, NamedTextColor.WHITE))
                .append(Component.text(" is now ", NamedTextColor.GRAY))
                .append(Component.text(isOutput ? "enabled" : "disabled", NamedTextColor.WHITE))
                .build();

        sender.sendMessage(message);
    }

    private @Nullable DenemePlayer parseTarget(@NotNull Sender sender, @Nullable Sender t) {
        if (sender.isConsole() && t == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "console-specify-target", "%prefix% &cYou must specify a target as the console!"));
            return null;
        }
        Sender target = t == null ? sender : t;

        DenemePlayer denemePlayer = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getUniqueId());
        if (denemePlayer == null) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(sender.getPlatformPlayer().getNative());
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!"));

            if (user == null) {
                sender.sendMessage(Component.text("Unknown PacketEvents user", NamedTextColor.RED));
            } else {
                boolean isExempt = DenemeAPI.INSTANCE.getPlayerDataManager().shouldCheck(user);
                if (!isExempt) {
                    sender.sendMessage(Component.text("User connection state: " + user.getConnectionState(), NamedTextColor.RED));
                }
            }
        }

        return denemePlayer;
    }
}
