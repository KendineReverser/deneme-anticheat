package ac.deneme.anticheat.manager.init.start;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.command.commands.DenemeVersion;

public class UpdateChecker implements StartableInitable {
    @Override
    public void start() {
        if (DenemeAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            DenemeVersion.checkForUpdatesAsync(DenemeAPI.INSTANCE.getPlatformServer().getConsoleSender());
        }
    }
}
