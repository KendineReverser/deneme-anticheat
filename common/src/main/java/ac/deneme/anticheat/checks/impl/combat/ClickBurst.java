package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "ClickBurst", stableKey = "deneme.combat.click_burst", description = "Clicked with a machine constant interval", decay = 0.02)
public class ClickBurst extends Check implements PacketReceiveListener {

    private static final int BURST_INTERVALS = 6;
    private static final double MAXIMUM_SPREAD_MS = 1.0;
    private static final double MINIMUM_INTERVAL_MS = 40.0;
    private static final double MAXIMUM_INTERVAL_MS = 300.0;
    private static final long RESET_MS = 1200L;
    private static final int FLAG_BURSTS = 2;
    private static final long FLAG_WINDOW_MS = 20000L;

    private final long[] intervals = new long[BURST_INTERVALS];
    private int intervalIndex;
    private int intervalCount;
    private long lastClickAt;
    private int bursts;
    private long firstBurstAt;

    public ClickBurst(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        boolean attack = event.getPacketType() == PacketType.Play.Client.ATTACK
                || (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                && new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK);

        if (!attack || player.disableDeneme) {
            return;
        }

        if (new WrapperPlayClientAttack(event).getEntityId() == -1) {
            return;
        }

        long now = System.nanoTime();

        if (lastClickAt != 0L) {
            long deltaMs = (now - lastClickAt) / 1_000_000L;

            if (deltaMs > RESET_MS) {
                intervalCount = 0;
                intervalIndex = 0;
            } else if (deltaMs > 0L) {
                intervals[intervalIndex] = deltaMs;
                intervalIndex = (intervalIndex + 1) % BURST_INTERVALS;
                intervalCount = Math.min(BURST_INTERVALS, intervalCount + 1);

                if (intervalCount == BURST_INTERVALS) {
                    evaluate();
                }
            }
        }

        lastClickAt = now;
    }

    private void evaluate() {
        long minimum = Long.MAX_VALUE;
        long maximum = Long.MIN_VALUE;
        long total = 0L;

        for (long interval : intervals) {
            minimum = Math.min(minimum, interval);
            maximum = Math.max(maximum, interval);
            total += interval;
        }

        double mean = (double) total / (double) intervals.length;

        if (mean < MINIMUM_INTERVAL_MS || mean > MAXIMUM_INTERVAL_MS) {
            intervalCount = 0;
            return;
        }

        if (maximum - minimum > MAXIMUM_SPREAD_MS) {
            intervalCount = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (bursts == 0 || now - firstBurstAt > FLAG_WINDOW_MS) {
            bursts = 0;
            firstBurstAt = now;
        }
        bursts++;
        CombatEvidence.report(player, "click_burst", 1.0);

        if (bursts >= FLAG_BURSTS) {
            flag(String.format("mean=%.1f spread=%dms", mean, maximum - minimum));
            bursts = 0;
            firstBurstAt = now;
        }

        intervalCount = 0;
        intervalIndex = 0;
    }
}
