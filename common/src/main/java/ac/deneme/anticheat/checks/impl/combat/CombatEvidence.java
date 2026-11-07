package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.api.AbstractCheck;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.player.DenemePlayer;

import java.util.HashMap;
import java.util.Map;

@CheckData(name = "CombatEvidence", stableKey = "deneme.combat.evidence", description = "Independent combat signals agreed on the same player", decay = 0.05)
public class CombatEvidence extends Check {

    private static final long WINDOW_MS = 10000L;
    private static final double FLAG_EVIDENCE = 3.0;
    private static final int MINIMUM_SIGNALS = 2;

    private final Map<String, Long> signalTimes = new HashMap<>();
    private final Map<String, Double> signalWeights = new HashMap<>();

    public CombatEvidence(DenemePlayer player) {
        super(player);
    }

    public static void report(DenemePlayer player, String signal, double weight) {
        CombatEvidence evidence = lookup(player);
        if (evidence != null) {
            evidence.add(signal, weight);
        }
    }

    private static CombatEvidence lookup(DenemePlayer player) {
        for (AbstractCheck check : player.getChecks()) {
            if (check instanceof CombatEvidence combatEvidence) {
                return combatEvidence;
            }
        }
        return null;
    }

    private void add(String signal, double weight) {
        long now = System.currentTimeMillis();
        signalTimes.put(signal, now);
        signalWeights.merge(signal, weight, Double::sum);

        signalTimes.entrySet().removeIf(entry -> now - entry.getValue() > WINDOW_MS);
        signalWeights.keySet().retainAll(signalTimes.keySet());

        if (signalTimes.size() < MINIMUM_SIGNALS) {
            return;
        }

        double total = 0.0;
        for (String key : signalTimes.keySet()) {
            total += signalWeights.getOrDefault(key, 0.0);
        }

        if (total < FLAG_EVIDENCE) {
            return;
        }

        flag(String.format("signals=%s evidence=%.1f", signalTimes.keySet(), total));
        signalTimes.clear();
        signalWeights.clear();
    }
}
