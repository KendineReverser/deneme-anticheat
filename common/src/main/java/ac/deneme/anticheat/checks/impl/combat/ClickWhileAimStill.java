package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.checks.type.RotationListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.RotationUpdate;
import ac.deneme.anticheat.utils.data.combat.CombatUtil;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "ClickWhileAimStill", stableKey = "deneme.combat.click_aim_still", description = "Attacked a moving target without moving the view", decay = 0.02)
public class ClickWhileAimStill extends Check implements PacketReceiveListener, RotationListener {

    private static final float STILL_ROTATION = 0.05f;
    private static final int STILL_ROTATIONS = 5;
    private static final double MINIMUM_TARGET_SPEED = 0.08;
    private static final long WINDOW_MS = 10000L;
    private static final int FLAG_HITS = 3;

    private final Int2ObjectMap<Vector3d> lastTargetPositions = new Int2ObjectOpenHashMap<>();

    private int stillRotations;
    private int hits;
    private long firstHitAt;

    public ClickWhileAimStill(DenemePlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (rotationUpdate.deltaYawABS() < STILL_ROTATION && rotationUpdate.deltaPitchABS() < STILL_ROTATION) {
            stillRotations = Math.min(STILL_ROTATIONS, stillRotations + 1);
        } else {
            stillRotations = 0;
        }
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

        Vector3d position = target.trackedServerPosition.getPos();
        Vector3d previous = lastTargetPositions.put(entityId, position);

        if (previous == null || stillRotations < STILL_ROTATIONS) {
            return;
        }

        double speed = Math.sqrt(
                Math.pow(position.getX() - previous.getX(), 2)
                        + Math.pow(position.getY() - previous.getY(), 2)
                        + Math.pow(position.getZ() - previous.getZ(), 2));

        if (speed < MINIMUM_TARGET_SPEED) {
            return;
        }

        long now = System.currentTimeMillis();
        if (hits == 0 || now - firstHitAt > WINDOW_MS) {
            hits = 0;
            firstHitAt = now;
        }
        hits++;
        CombatEvidence.report(player, "click_aim_still", 1.0);

        if (hits >= FLAG_HITS) {
            flag(String.format("targetSpeed=%.3f still=%d", speed, stillRotations));
            hits = 0;
            firstHitAt = now;
        }
    }
}
