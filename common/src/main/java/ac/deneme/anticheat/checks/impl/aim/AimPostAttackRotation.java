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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AimPostAttackRotation", stableKey = "deneme.aim.post_attack_rotation", description = "Returned the rotation right after attacking", decay = 0.02)
public class AimPostAttackRotation extends Check implements PacketReceiveListener, RotationListener {

    private static final float MINIMUM_SNAP = 8.0f;
    private static final double MINIMUM_RATIO = 0.65;
    private static final double MAXIMUM_RATIO = 1.45;
    private static final int TRACKED_ROTATIONS = 4;
    private static final long TRACK_WINDOW_MS = 400L;
    private static final int FLAG_HITS = 3;
    private static final long FLAG_WINDOW_MS = 10000L;

    private float lastDeltaYaw;
    private float preAttackDeltaYaw;
    private long attackAt;
    private int trackedRotations;
    private int hits;
    private long firstHitAt;

    public AimPostAttackRotation(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        boolean attack = event.getPacketType() == PacketType.Play.Client.ATTACK
                || (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                && new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK);

        if (!attack || player.disableDeneme || player.packetStateData.lastPacketWasTeleport) {
            return;
        }

        preAttackDeltaYaw = lastDeltaYaw;
        attackAt = System.currentTimeMillis();
        trackedRotations = TRACKED_ROTATIONS;
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || player.vehicleData.wasVehicleSwitch) {
            trackedRotations = 0;
            lastDeltaYaw = rotationUpdate.deltaYaw();
            return;
        }

        if (trackedRotations <= 0 || System.currentTimeMillis() - attackAt > TRACK_WINDOW_MS
                || Math.abs(preAttackDeltaYaw) < MINIMUM_SNAP) {
            lastDeltaYaw = rotationUpdate.deltaYaw();
            trackedRotations = Math.max(0, trackedRotations - 1);
            return;
        }

        trackedRotations--;

        float delta = rotationUpdate.deltaYaw();
        boolean reversed = delta != 0.0f && Math.signum(delta) == -Math.signum(preAttackDeltaYaw);

        if (reversed) {
            double ratio = Math.abs(delta) / Math.abs(preAttackDeltaYaw);
            if (ratio >= MINIMUM_RATIO && ratio <= MAXIMUM_RATIO) {
                long now = System.currentTimeMillis();
                if (hits == 0 || now - firstHitAt > FLAG_WINDOW_MS) {
                    hits = 0;
                    firstHitAt = now;
                }
                hits++;
                CombatEvidence.report(player, "aim_return", 1.0);

                if (hits >= FLAG_HITS) {
                    flag(String.format("snap=%.2f return=%.2f ratio=%.2f", preAttackDeltaYaw, delta, ratio));
                    hits = 0;
                    firstHitAt = now;
                }
            }
        }

        lastDeltaYaw = rotationUpdate.deltaYaw();
        trackedRotations = 0;
    }
}
