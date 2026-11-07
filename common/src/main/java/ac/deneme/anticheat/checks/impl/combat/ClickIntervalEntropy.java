package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.data.combat.CombatUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@CheckData(name = "ClickIntervalEntropy", stableKey = "deneme.combat.click_entropy", description = "Clicked with a machine narrow interval distribution", decay = 0.01)
public class ClickIntervalEntropy extends Check implements PacketReceiveListener {

    private static final int BIN_COUNT = 40;
    private static final double BIN_SIZE_MS = 6.0;
    private static final int MINIMUM_SAMPLES = 30;
    private static final double MINIMUM_ENTROPY = 2.4;
    private static final double MINIMUM_MEAN_MS = 45.0;
    private static final double MAXIMUM_MEAN_MS = 220.0;
    private static final long RESET_MS = 1500L;

    private final int[] bins = new int[BIN_COUNT];
    private long lastClickAt;
    private int samples;
    private double totalInterval;
    private long cooldownUntil;

    public ClickIntervalEntropy(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        boolean attack = event.getPacketType() == PacketType.Play.Client.ATTACK
                || (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                && new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK);

        if (!attack || player.disableDeneme || System.currentTimeMillis() < cooldownUntil) {
            return;
        }

        long now = System.nanoTime();

        if (lastClickAt == 0L) {
            lastClickAt = now;
            return;
        }

        long intervalMs = (now - lastClickAt) / 1_000_000L;
        lastClickAt = now;

        if (intervalMs <= 0L || intervalMs > RESET_MS) {
            reset();
            return;
        }

        int bin = (int) (intervalMs / BIN_SIZE_MS);
        if (bin >= BIN_COUNT) {
            reset();
            return;
        }

        bins[bin]++;
        samples++;
        totalInterval += intervalMs;

        if (samples < MINIMUM_SAMPLES) {
            return;
        }

        double mean = totalInterval / samples;
        double entropy = CombatUtil.entropy(bins);

        if (mean >= MINIMUM_MEAN_MS && mean <= MAXIMUM_MEAN_MS && entropy < MINIMUM_ENTROPY) {
            CombatEvidence.report(player, "click_entropy", 1.0);
            flag(String.format("entropy=%.2f mean=%.1f samples=%d", entropy, mean, samples));
            reset();
            cooldownUntil = System.currentTimeMillis() + 10000L;
        }
    }

    private void reset() {
        Arrays.fill(bins, 0);
        samples = 0;
        totalInterval = 0.0;
    }
}
