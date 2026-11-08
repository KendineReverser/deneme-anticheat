package ac.deneme.anticheat.checks.impl.aim;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.combat.CombatEvidence;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.combat.CombatUtil;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AimTargetAngle", stableKey = "deneme.aim.target_angle", description = "Attacked an entity outside the reachable look angle", decay = 0.02)
public class AimTargetAngle extends Check implements PacketReceiveListener {

    private static final double ANGLE_MARGIN = 6.0;
    private static final double MIN_ATTACK_DISTANCE = 0.35;
    private static final double MAX_ATTACK_DISTANCE = 4.5;
    private static final int WINDOW_ATTACKS = 14;
    private static final int WINDOW_MISSES = 4;
    private static final long WINDOW_RESET_MS = 8000L;

    private int attacks;
    private int misses;
    private long windowStart;

    public AimTargetAngle(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            handle(new WrapperPlayClientAttack(event).getEntityId());
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                handle(packet.getEntityId());
            }
        }
    }

    private void handle(int entityId) {
        PacketEntity target = player.compensatedEntities.entityMap.get(entityId);
        if (!CombatUtil.usable(player, target)) {
            return;
        }

        SimpleCollisionBox box = target.getPossibleLocationBoxes();
        if (box == null) {
            return;
        }

        double centerX = (box.minX + box.maxX) * 0.5;
        double centerY = (box.minY + box.maxY) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;

        double offsetX = centerX - player.x;
        double offsetY = centerY - CombatUtil.eyeY(player);
        double offsetZ = centerZ - player.z;

        double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        if (distance < MIN_ATTACK_DISTANCE || distance > MAX_ATTACK_DISTANCE) {
            return;
        }

        double radius = CombatUtil.halfDiagonal(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ);
        double allowed = CombatUtil.angularRadius(radius, distance);
        double actual = CombatUtil.angle(CombatUtil.look(player), new double[]{offsetX, offsetY, offsetZ});

        long now = System.currentTimeMillis();
        if (now - windowStart > WINDOW_RESET_MS) {
            attacks = 0;
            misses = 0;
            windowStart = now;
        }

        attacks++;

        if (actual - allowed <= ANGLE_MARGIN) {
            return;
        }

        misses++;
        CombatEvidence.report(player, "aim_angle", 1.0);

        if (misses >= WINDOW_MISSES && attacks >= WINDOW_ATTACKS) {
            flag(String.format("angle=%.2f allowed=%.2f distance=%.2f", actual, allowed, distance));
            attacks = 0;
            misses = 0;
            windowStart = now;
        }
    }
}
