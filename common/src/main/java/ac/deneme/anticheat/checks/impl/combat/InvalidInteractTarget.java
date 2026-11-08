package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.player.DenemePlayer;

@CheckData(name = "InvalidInteractTarget", stableKey = "deneme.badpackets.invalid_entity_target", description = "Interacted with non-existent entity", experimental = true)
public class InvalidInteractTarget extends Check {
    public InvalidInteractTarget(DenemePlayer player) {
        super(player);
    }
}
