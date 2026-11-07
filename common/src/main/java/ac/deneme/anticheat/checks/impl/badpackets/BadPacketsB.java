package ac.deneme.anticheat.checks.impl.badpackets;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.player.DenemePlayer;

@CheckData(name = "BadPacketsB", stableKey = "deneme.badpackets.ignored_rotation", description = "Ignored set rotation packet")
public class BadPacketsB extends Check {
    public BadPacketsB(final DenemePlayer player) {
        super(player);
    }
}
