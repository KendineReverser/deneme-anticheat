package ac.deneme.anticheat.checks.impl.badpackets;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.player.DenemePlayer;

@CheckData(name = "BadPacketsN", stableKey = "deneme.badpackets.invalid_teleport", description = "Ignored or failed to accept a required server teleport")
public class BadPacketsN extends Check {
    public BadPacketsN(final DenemePlayer player) {
        super(player);
    }
}
