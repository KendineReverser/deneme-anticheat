package ac.deneme.anticheat.checks.impl.aim;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.combat.CombatEvidence;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.data.combat.CombatUtil;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AimSwitchTime", stableKey = "deneme.aim.switch_time", description = "Switched targets faster than a mouse can move", decay = 0.02)
public class AimSwitchTime extends Check implements PacketReceiveListener {

    private static final double MINIMUM_SWITCH_ANGLE = 55.0;
    private static final double MAXIMUM_DEGREES_PER_MS = 1.35;
    private static final long SWITCH_WINDOW_MS = 2500L;
    private static final int FLAG_SWITCHES = 3;

    private int lastTarget = -1;
    private long lastAttackAt;
    private float lastYaw;
    private float lastPitch;
    private int switches;
    private long windowStart;

    public AimSwitchTime(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        int entityId = -1;

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            entityId = new WrapperPlayClientAttack(event).getEntityId();
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                entityId = packet.getEntityId();
            }
        }

        if (entityId == -1) {
            return;
        }

        PacketEntity target = player.compensatedEntities.entityMap.get(entityId);
        if (!CombatUtil.usable(player, target)) {
            lastTarget = entityId;
            lastAttackAt = System.currentTimeMillis();
            lastYaw = player.yaw;
            lastPitch = player.pitch;
            return;
        }

        long now = System.currentTimeMillis();

        if (lastTarget != -1 && lastTarget != entityId) {
            long elapsed = now - lastAttackAt;

            if (elapsed >= 5L && elapsed <= SWITCH_WINDOW_MS) {
                double angle = CombatUtil.angle(CombatUtil.look(lastYaw, lastPitch), CombatUtil.look(player));

                if (angle >= MINIMUM_SWITCH_ANGLE) {
                    double degreesPerMillisecond = angle / (double) elapsed;

                    if (degreesPerMillisecond > MAXIMUM_DEGREES_PER_MS) {
                        if (windowsReset(now)) {
                            switches = 0;
                            windowStart = now;
                        }
                        switches++;
                        CombatEvidence.report(player, "aim_switch", 1.0);

                        if (switches >= FLAG_SWITCHES) {
                            flag(String.format("angle=%.1f elapsed=%dms speed=%.2f deg/ms", angle, elapsed, degreesPerMillisecond));
                            switches = 0;
                            windowStart = now;
                        }
                    }
                }
            }
        }

        lastTarget = entityId;
        lastAttackAt = now;
        lastYaw = player.yaw;
        lastPitch = player.pitch;
    }

    private boolean windowsReset(long now) {
        return windowStart == 0L || now - windowStart > 10000L;
    }
}
