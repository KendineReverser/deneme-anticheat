package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "ClickTiming", stableKey = "deneme.combat.click_timing", description = "Attacked with machine precise intervals", decay = 0.01, experimental = true)
public class ClickTiming extends Check implements PacketReceiveListener {

    private static final int WINDOW = 20;
    private static final long IDLE_RESET_NS = 1_200_000_000L;
    private static final double MAX_MEAN_MS = 260.0;
    private static final double MAX_DEVIATION_MS = 4.5;

    private final long[] intervals = new long[WINDOW];

    private long lastAttack;
    private int samples;

    public ClickTiming(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!isAttack(event)) return;

        long now = System.nanoTime();

        if (lastAttack != 0) {
            long interval = now - lastAttack;

            if (interval > IDLE_RESET_NS) {
                samples = 0;
            } else {
                intervals[samples++] = interval;
                if (samples == WINDOW) {
                    evaluate();
                    samples = 0;
                }
            }
        }

        lastAttack = now;
    }

    private void evaluate() {
        double mean = 0.0;
        for (long interval : intervals) mean += interval;
        mean = mean / WINDOW / 1.0e6;

        double variance = 0.0;
        for (long interval : intervals) {
            double difference = interval / 1.0e6 - mean;
            variance += difference * difference;
        }

        double deviation = Math.sqrt(variance / WINDOW);

        if (mean > MAX_MEAN_MS) {
            reward();
            return;
        }

        if (deviation <= MAX_DEVIATION_MS) {
            flag(String.format("interval=%.2fms deviation=%.2fms clicks=%d", mean, deviation, WINDOW));
        } else {
            reward();
        }
    }

    private static boolean isAttack(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) return true;
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return false;

        return new WrapperPlayClientInteractEntity(event).getAction()
                == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
    }
}
