package ac.deneme.anticheat.command.commands;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.command.BuildableCommand;
import ac.deneme.anticheat.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.deneme.anticheat.platform.api.sender.Sender;
import ac.deneme.anticheat.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

public class DenemeSendAlert implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder("denemeac", "dac")
                        .literal("sendalert")
                        .permission("deneme.sendalert")
                        .required("message", StringParser.greedyStringParser())
                        .handler(this::handleSendAlert)
        );
    }

    private void handleSendAlert(@NotNull CommandContext<Sender> context) {
        String string = context.get("message");
        string = MessageUtil.replacePlaceholders((Sender) null, string);
        Component message = MessageUtil.miniMessage(string);
        DenemeAPI.INSTANCE.getAlertManager().sendAlert(message, null);
    }
}
