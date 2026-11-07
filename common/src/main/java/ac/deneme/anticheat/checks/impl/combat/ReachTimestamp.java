package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.combat.CombatUtil;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "ReachTimestamp", stableKey = "deneme.combat.reach_timestamp", description = "Attacked a position the target had already left", decay = 0.02)
public class ReachTimestamp extends Check implements PacketReceiveListener {

    private static final double MINIMUM_ATTACK_DISTANCE = 3.0;
    private static final double MINIMUM_JUMP = 1.0;
    private static final double MINIMUM_APPROACH = 0.5;
    private static final long WINDOW_MS = 10000L;
    private static final int FLAG_HITS = 2;

    private int targetId = -1;
    private double attackDistance;
    private Vector3d attackPosition;
    private boolean pending;

    private int hits;
    private long firstHitAt;

    public ReachTimestamp(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.ATTACK || type == PacketType.Play.Client.INTERACT_ENTITY) {
            int entityId = type == PacketType.Play.Client.ATTACK
                    ? new WrapperPlayClientAttack(event).getEntityId()
                    : new WrapperPlayClientInteractEntity(event).getEntityId();

            if (type == PacketType.Play.Client.INTERACT_ENTITY
                    && new WrapperPlayClientInteractEntity(event).getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                return;
            }

            PacketEntity target = player.compensatedEntities.entityMap.get(entityId);

            if (!CombatUtil.usable(player, target)) {
                pending = false;
                return;
            }

            SimpleCollisionBox box = target.getPossibleLocationBoxes();
            if (box == null) {
                pending = false;
                return;
            }

            targetId = entityId;
            attackPosition = target.trackedServerPosition.getPos();
            attackDistance = distanceTo(box);
            pending = attackDistance > MINIMUM_ATTACK_DISTANCE;
            return;
        }

        if (!pending || !isUpdate(type)) {
            return;
        }

        pending = false;

        PacketEntity target = player.compensatedEntities.entityMap.get(targetId);
        if (target == null || attackPosition == null) {
            return;
        }

        Vector3d current = target.trackedServerPosition.getPos();
        double jump = Math.sqrt(
                Math.pow(current.getX() - attackPosition.getX(), 2)
                        + Math.pow(current.getY() - attackPosition.getY(), 2)
                        + Math.pow(current.getZ() - attackPosition.getZ(), 2));

        SimpleCollisionBox box = target.getPossibleLocationBoxes();
        if (box == null) {
            return;
        }

        double currentDistance = distanceTo(box);

        if (jump < MINIMUM_JUMP || attackDistance - currentDistance < MINIMUM_APPROACH) {
            return;
        }

        long now = System.currentTimeMillis();
        if (hits == 0 || now - firstHitAt > WINDOW_MS) {
            hits = 0;
            firstHitAt = now;
        }
        hits++;
        CombatEvidence.report(player, "reach_timestamp", 1.0);

        if (hits >= FLAG_HITS) {
            flag(String.format("jump=%.2f was=%.2f now=%.2f", jump, attackDistance, currentDistance));
            hits = 0;
            firstHitAt = now;
        }
    }

    private double distanceTo(SimpleCollisionBox box) {
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerY = (box.minY + box.maxY) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;

        double offsetX = centerX - player.x;
        double offsetY = centerY - CombatUtil.eyeY(player);
        double offsetZ = centerZ - player.z;

        return Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
    }
}
