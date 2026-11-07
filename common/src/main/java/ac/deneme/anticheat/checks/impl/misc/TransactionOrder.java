package ac.deneme.anticheat.checks.impl.misc;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.player.DenemePlayer;

@CheckData(name = "TransactionOrder", stableKey = "deneme.ping.invalid_transaction_order", description = "Sent transaction or ping responses in an invalid order")
public class TransactionOrder extends Check {
    public TransactionOrder(DenemePlayer player) {
        super(player);
    }
}
