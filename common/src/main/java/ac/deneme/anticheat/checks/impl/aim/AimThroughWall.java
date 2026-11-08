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

@CheckData(name = "AimThroughWall", stableKey = "deneme.aim.through_wall", description = "Attacked an entity it could not see", decay = 0.02)
public class AimThroughWall extends Check implements PacketReceiveListener {

    private static final long WINDOW_MS = 10000L;
    private static final int FLAG_HITS = 3;

    private int hits;
    private long firstHitAt;

    public AimThroughWall(DenemePlayer player) {
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

        if (distance < 1.5 || distance > 6.0) {
            return;
        }

        if (!CombatUtil.occluded(player, centerX, centerY, centerZ)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (hits == 0 || now - firstHitAt > WINDOW_MS) {
            hits = 0;
            firstHitAt = now;
        }
        hits++;
        CombatEvidence.report(player, "aim_wall", 1.0);

        if (hits >= FLAG_HITS) {
            flag(String.format("distance=%.2f", distance));
            hits = 0;
            firstHitAt = now;
        }
    }
}
