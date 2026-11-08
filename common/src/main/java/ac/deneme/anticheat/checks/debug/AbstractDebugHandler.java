package ac.deneme.anticheat.checks.debug;

import ac.deneme.anticheat.checks.DenemeProcessor;
import ac.deneme.anticheat.player.DenemePlayer;

public abstract class AbstractDebugHandler extends DenemeProcessor {
    public AbstractDebugHandler(DenemePlayer player) {
        super(player);
    }

    public abstract void toggleListener(DenemePlayer player);

    public abstract boolean toggleConsoleOutput();
}
