package ac.deneme.anticheat.manager.init.start;

import ac.deneme.anticheat.platform.api.command.CommandService;
import ac.deneme.anticheat.utils.anticheat.LogUtil;

public record CommandRegister(CommandService service) implements StartableInitable {

    @Override
    public void start() {
        try {
            if (service != null) {
                service.registerCommands();
            }
        } catch (RuntimeException t) {

            LogUtil.error("Failed to register commands! Deneme will run without command support.", t);
        }
    }
}
