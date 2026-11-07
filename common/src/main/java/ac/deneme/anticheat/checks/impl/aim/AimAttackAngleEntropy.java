package ac.deneme.anticheat.checks.impl.aim;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.combat.CombatEvidence;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.checks.type.RotationListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.RotationUpdate;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AimAttackAngleEntropy", stableKey = "deneme.aim.attack_angle_entropy", description = "Attacked at a machine constant rotation angle", decay = 0.01)
public class AimAttackAngleEntropy extends Check implements PacketReceiveListener, RotationListener {

    private static final int BINS = 24;
    private static final double BIN_SIZE = 1.0;
    private static final int MINIMUM_SAMPLES = 36;
    private static final double MINIMUM_ENTROPY = 2.2;
    private static final double MAXIMUM_MEAN_ANGLE = 4.0;
    private static final int MINIMUM_SWITCHES = 3;

    private final int[] bins = new int[BINS - 1];
    private float lastDeltaYaw;
    private int samples;
    private int switches;
    private int lastTarget = -1;
    private double totalAngle;
    private long cooldownUntil;

    public AimAttackAngleEntropy(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.ATTACK || player.disableDeneme) {
            return;
        }

        if (System.currentTimeMillis() < cooldownUntil) {
            return;
        }

        int entityId = new WrapperPlayClientAttack(event).getEntityId();

        if (lastTarget != -1 && lastTarget != entityId) {
            switches++;
        }
        lastTarget = entityId;

        double angle = Math.abs(lastDeltaYaw);
        if (angle > BINS * BIN_SIZE) {
            return;
        }

        bins[Math.min(bins.length - 1, (int) (angle / BIN_SIZE))]++;
        samples++;
        totalAngle += angle;

        if (samples < MINIMUM_SAMPLES || switches < MINIMUM_SWITCHES) {
            return;
        }

        double entropy = ac.deneme.anticheat.utils.data.combat.CombatUtil.entropy(bins);
        double mean = totalAngle / samples;

        if (entropy < MINIMUM_ENTROPY && mean < MAXIMUM_MEAN_ANGLE) {
            CombatEvidence.report(player, "aim_constant_angle", 1.0);
            flag(String.format("entropy=%.2f mean=%.2f samples=%d switches=%d", entropy, mean, samples, switches));
            reset();
            cooldownUntil = System.currentTimeMillis() + 5000L;
            return;
        }

        if (samples >= MINIMUM_SAMPLES * 2) {
            reset();
        }
    }

    private void reset() {
        java.util.Arrays.fill(bins, 0);
        samples = 0;
        switches = 0;
        totalAngle = 0.0;
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        lastDeltaYaw = rotationUpdate.deltaYaw();
    }
}
