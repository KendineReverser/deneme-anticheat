package ac.deneme.anticheat.checks.impl.aim;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.checks.type.RotationListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.RotationUpdate;
import ac.deneme.anticheat.utils.reflection.GeyserUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AimSnap", stableKey = "deneme.aim.snap", description = "Rotated instantly onto a target right before attacking", decay = 0.02, experimental = true)
public class AimSnap extends Check implements RotationListener, PacketReceiveListener {

    private static final int HISTORY = 6;
    private static final double SNAP_YAW = 35.0;
    private static final double SNAP_PITCH = 22.0;
    private static final double CALM_DELTA = 6.0;
    private static final int STREAK_TO_FLAG = 3;

    private final double[] yawHistory = new double[HISTORY];
    private final double[] pitchHistory = new double[HISTORY];

    private int cursor;
    private int samples;
    private int streak;

    public AimSnap(DenemePlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (exempt()) {
            streak = 0;
            samples = 0;
            return;
        }

        yawHistory[cursor] = rotationUpdate.deltaYawABS();
        pitchHistory[cursor] = rotationUpdate.deltaPitchABS();
        cursor = (cursor + 1) % HISTORY;
        if (samples < HISTORY) samples++;
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        boolean attack = event.getPacketType() == PacketType.Play.Client.ATTACK;

        if (!attack && event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        if (!attack) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
        }

        if (exempt() || samples < HISTORY) {
            samples = 0;
            return;
        }

        double snapYaw = Math.max(yawHistory[previous(1)], yawHistory[previous(2)]);
        double snapPitch = Math.max(pitchHistory[previous(1)], pitchHistory[previous(2)]);
        double calmYaw = meanOfRest();

        boolean snapped = snapYaw > SNAP_YAW || snapPitch > SNAP_PITCH;

        if (snapped && calmYaw < CALM_DELTA) {
            if (++streak >= STREAK_TO_FLAG) {
                flag(String.format("yaw=%.2f pitch=%.2f baseline=%.2f streak=%d", snapYaw, snapPitch, calmYaw, streak));
                streak = 0;
            }
        } else {
            streak = Math.max(0, streak - 1);
        }

        samples = 0;
    }

    private double meanOfRest() {
        double total = 0.0;
        int count = 0;
        for (int i = 3; i < HISTORY; i++) {
            total += yawHistory[previous(i)];
            count++;
        }
        return count == 0 ? 0.0 : total / count;
    }

    private int previous(int offset) {
        return (cursor - offset + HISTORY * offset) % HISTORY;
    }

    private boolean exempt() {
        return player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.vehicleData.wasVehicleSwitch
                || player.compensatedEntities.self.getRiding() != null
                || GeyserUtil.isBedrockPlayer(player.user.getUUID());
    }
}
