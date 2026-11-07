package ac.deneme.anticheat.manager;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.AbstractCheck;
import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.config.ConfigReloadable;
import ac.deneme.anticheat.api.event.events.CommandExecuteEvent;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.events.packets.ProxyAlertMessenger;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.LogUtil;
import ac.deneme.anticheat.utils.anticheat.CheckCategories;
import ac.deneme.anticheat.utils.anticheat.MessageUtil;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PunishmentManager implements ConfigReloadable {
    private static final CommandExecuteEvent.Channel COMMAND_CHANNEL = DenemeAPI.INSTANCE.getEventBus().get(CommandExecuteEvent.class);
    private final DenemePlayer player;
    private final List<PunishGroup> groups = new ArrayList<>();
    private String experimentalSymbol = "*";
    private String alertString;
    private boolean testMode;
    private String proxyAlertString = "";

    public PunishmentManager(DenemePlayer player) {
        this.player = player;
    }

    @Override
    public void reload(ConfigManager config) {
        List<String> punish = config.getStringListElse("Punishments", new ArrayList<>());
        experimentalSymbol = config.getStringElse("experimental-symbol", "*");

        alertString = config.getStringElse(
                "alerts-format",
                "%prefix% &f[&b%player%&f] &f%category% &7detected &8(&b%check_name%&8)"
        );

        testMode = config.getBooleanElse("test-mode", false);

        proxyAlertString = config.getStringElse(
                "alerts-format-proxy",
                "%prefix% &f[&cproxy&f] &f[&b%player%&f] &f%category% &7detected &8(&b%check_name%&8)"
        );

        try {
            groups.clear();

            for (AbstractCheck check : player.getChecks()) {
                check.setEnabled(false);
            }

            for (Object s : punish) {
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) s;

                List<String> checks = (List<String>) map.getOrDefault("checks", new ArrayList<>());
                List<String> commands = (List<String>) map.getOrDefault("commands", new ArrayList<>());
                int removeViolationsAfter = (int) map.getOrDefault("remove-violations-after", 300);

                List<ParsedCommand> parsed = new ArrayList<>();
                List<AbstractCheck> checksList = new ArrayList<>();
                List<AbstractCheck> excluded = new ArrayList<>();
                for (String command : checks) {
                    command = command.toLowerCase(Locale.ROOT);
                    boolean exclude = false;
                    if (command.startsWith("!")) {
                        exclude = true;
                        command = command.substring(1);
                    }
                    for (AbstractCheck check : player.getChecks()) {
                        if (check.getCheckName().toLowerCase(Locale.ROOT).contains(command)
                                || check.getAlternativeName().toLowerCase(Locale.ROOT).contains(command)) {
                            if (exclude) {
                                excluded.add(check);
                            } else {
                                checksList.add(check);
                            }
                        }
                    }
                    for (AbstractCheck check : excluded) checksList.remove(check);
                }
                for (AbstractCheck check : checksList) {
                    check.setEnabled(true);
                }

                for (String command : commands) {
                    String firstNum = command.substring(0, command.indexOf(":"));
                    String secondNum = command.substring(command.indexOf(":"), command.indexOf(" "));

                    int threshold = Integer.parseInt(firstNum);
                    int interval = Integer.parseInt(secondNum.substring(1));
                    String commandString = command.substring(command.indexOf(" ") + 1);

                    parsed.add(new ParsedCommand(threshold, interval, commandString));
                }

                groups.add(new PunishGroup(checksList, parsed, removeViolationsAfter * 1000));
            }
        } catch (Exception e) {
            LogUtil.error("Error while loading punishments.yml! This is likely your fault!", e);
        }
    }

    private String replaceAlertPlaceholders(String original, int vl, Check check, String verbose) {
        return MessageUtil.replacePlaceholders(player, original
                .replace("[alert]", alertString)
                .replace("[proxy]", proxyAlertString)
                .replace("%check_name%", check.getDisplayName())
                .replace("%category%", CheckCategories.of(check))
                .replace("%experimental%", check.isExperimental() ? experimentalSymbol : "")
                .replace("%vl%", Integer.toString(vl))
                .replace("%description%", check.getDescription())
                .replace("%stable_key%", check.getStableKey())
        ).replace("%verbose%", MessageUtil.miniMessageSafe(verbose));
    }

    public boolean handleAlert(DenemePlayer player, String verbose, Check check) {
        String value = verbose == null ? "" : verbose;
        return handleAlert(player, () -> value, check);
    }

    public boolean handleAlert(DenemePlayer player, Supplier<String> verbose, Check check) {
        boolean sentDebug = false;

        for (PunishGroup group : groups) {
            if (group.checks.contains(check)) {
                final int vl = getViolations(group, check);
                final int violationCount = group.violations.size();
                for (ParsedCommand command : group.commands) {
                    @Nullable Set<@Nullable PlatformPlayer> verboseListeners = null;

                    if (command.command.equals("[alert]") && DenemeAPI.INSTANCE.getAlertManager().hasVerboseListeners()) {
                        sentDebug = true;
                        String verboseForListeners = safeGet(verbose);
                        String listenerCmd = replaceAlertPlaceholders(command.command, vl, check, verboseForListeners);
                        verboseListeners = DenemeAPI.INSTANCE.getAlertManager().sendVerbose(MessageUtil.miniMessage(listenerCmd), null);
                    }
                    if (violationCount >= command.threshold) {
                        boolean shouldRun = command.interval == 0
                                ? command.executeCount == 0
                                : violationCount >= command.nextBoundary;
                        if (shouldRun) {
                            String renderedVerbose = safeGet(verbose);
                            String cmd = replaceAlertPlaceholders(command.command, vl, check, renderedVerbose);
                            boolean canceled = COMMAND_CHANNEL.fire(player, check, renderedVerbose, cmd);
                            if (command.interval == 0) {
                                command.executeCount++;
                            } else {
                                advanceBoundary(command, violationCount);
                            }
                            if (canceled) continue;

                            switch (command.command) {
                                case "[webhook]" -> DenemeAPI.INSTANCE.getDiscordManager().sendAlert(player, renderedVerbose, check.getDisplayName(), vl);
                                case "[log]" -> {

                                    if (!check.isLastFlagStoredBinaryVerbose()) {
                                        String verboseWithoutGl = renderedVerbose.replaceAll(" /gl .*", "");
                                        DenemeAPI.INSTANCE.getDataStoreLifecycle().liveWriteHooks()
                                                .recordFlagFromCheck(player, check, vl, verboseWithoutGl);
                                    }
                                }
                                case "[proxy]" -> ProxyAlertMessenger.sendPluginMessage(cmd);
                                case "[alert]" -> {
                                    sentDebug = true;
                                    Component message = MessageUtil.miniMessage(cmd);
                                    if (testMode) {
                                        if (verboseListeners == null || verboseListeners.contains(player.platformPlayer)) {
                                            player.sendMessage(message);
                                        }
                                    } else {
                                        DenemeAPI.INSTANCE.getAlertManager().sendAlert(message, verboseListeners);
                                    }
                                }
                                default -> DenemeAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(DenemeAPI.INSTANCE.getDenemePlugin(), () ->
                                        DenemeAPI.INSTANCE.getPlatformServer().dispatchCommand(
                                                DenemeAPI.INSTANCE.getPlatformServer().getConsoleSender(),
                                                cmd
                                        )
                                );
                            }
                        }

                        if (command.interval > 0) command.executeCount++;
                    } else {

                        command.nextBoundary = command.threshold;
                    }
                }
            }
        }

        return sentDebug;
    }

    private static String safeGet(Supplier<String> supplier) {
        try {
            String value = supplier.get();
            return value == null ? "" : value;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static void advanceBoundary(ParsedCommand command, int violationCount) {
        command.nextBoundary += ((violationCount - command.nextBoundary) / command.interval + 1) * command.interval;
    }

    public void handleViolation(Check check) {
        for (PunishGroup group : groups) {
            if (group.checks.contains(check)) {
                long currentTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

                group.violations.record(currentTime, check, group.removeViolationsAfter);
            }
        }
    }

    private int getViolations(PunishGroup group, Check check) {
        return group.violations.count(check);
    }
}

@RequiredArgsConstructor
class PunishGroup {
    public final List<AbstractCheck> checks;
    public final List<ParsedCommand> commands;
    public final ViolationHistory<AbstractCheck> violations = new ViolationHistory<>();
    public final int removeViolationsAfter;
}

class ParsedCommand {
    public final int threshold;
    public final int interval;
    public final String command;

    public int executeCount;

    public int nextBoundary;

    public ParsedCommand(int threshold, int interval, String command) {
        this.threshold = threshold;
        this.interval = interval;
        this.command = command;
        this.nextBoundary = threshold;
    }
}
