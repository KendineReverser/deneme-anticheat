
package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntitySizeable;
import ac.deneme.anticheat.utils.data.packetentity.dragon.PacketEntityEnderDragonPart;
import ac.deneme.anticheat.utils.math.Vector3dm;
import ac.deneme.anticheat.utils.nmsutil.ReachUtils;
import ac.deneme.anticheat.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemAttackRange;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.viaversion.viaversion.api.Via;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CheckData(name = "Reach", stableKey = "deneme.combat.reach", description = "Attacked an entity from too far away")
public class Reach extends Check implements PacketReceiveListener {
    private static final Verbose V = Verbose.of("{f64:%.5f} blocks, type={entity}");

    private static final List<EntityType> blacklisted = Arrays.asList(
            EntityTypes.BOAT,
            EntityTypes.CHEST_BOAT,
            EntityTypes.SHULKER);
    private static final CheckResult NONE = new CheckResult(ResultType.NONE, 0, 0, false);

    private final Int2ObjectMap<InteractionData> playerAttackQueue = new Int2ObjectOpenHashMap<>();
    private boolean cancelImpossibleHits;
    private double threshold;
    private double cancelBuffer;

    public Reach(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!player.disableDeneme && event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            onInteract(event, packet.getEntityId(), InteractionHand.MAIN_HAND);
        }

        if (!player.disableDeneme && event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            onInteract(event, packet.getEntityId(), packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK ? InteractionHand.MAIN_HAND : packet.getHand());
        }

        if (isUpdate(event.getPacketType())) {
            tickBetterReachCheckWithAngle();
        }
    }

    private void onInteract(PacketReceiveEvent event, int entityId, InteractionHand hand) {

        if (player.getSetbackTeleportUtil().shouldBlockMovement()) {
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);

        if (entity == null || entity instanceof PacketEntityEnderDragonPart) {

            if (shouldModifyPackets() && player.compensatedEntities.serverPositionsMap.containsKey(entityId)) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            return;
        }

        if (entity.isDead) return;

        if (entity.getType() == EntityTypes.ARMOR_STAND && player.getClientVersion().isOlderThan(ClientVersion.V_1_8))
            return;

        if (entity.getType() == EntityTypes.HAPPY_GHAST && player.getClientVersion().isOlderThan(ClientVersion.V_1_21_6))
            return;
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR)
            return;
        if (player.inVehicle()) return;
        if (entity.riding != null) return;

        ItemStack currentStack = player.inventory.getItemInHand(hand);
        ItemStack startStack = player.inventory.getStartOfTickStack();

        boolean hasRange = false;
        float maxReach = 0f;
        float hitboxMargin = 0f;
        Vector3dm attackRangeMovement = null;

        boolean clientAttackRangeExists = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_11);
        boolean clientAndServerAgrees = clientAttackRangeExists && ATTACK_RANGE_COMPONENT_EXISTS;

        boolean viaVersionAvailable = false;
        if (USE_1_8_HITBOX_MARGIN && ViaVersionUtil.isAvailable) {
            viaVersionAvailable = Via.getConfig().getValues().containsKey("use-1_8-hitbox-margin") && Via.getConfig().use1_8HitboxMargin();
        }

        boolean clientAndViaVersion = clientAttackRangeExists && viaVersionAvailable;
        if (clientAndServerAgrees || clientAndViaVersion) {
            ItemAttackRange startRange = startStack.getComponentOr(ComponentTypes.ATTACK_RANGE, null);
            ItemAttackRange currentRange = currentStack.getComponentOr(ComponentTypes.ATTACK_RANGE, null);

            if (clientAndViaVersion) {
                if (startStack != ItemStack.EMPTY) {
                    startRange = new ItemAttackRange(0F, 3F, 0F, 4F, 0.1F, 1F);
                }

                if (currentStack != ItemStack.EMPTY) {
                    currentRange = new ItemAttackRange(0F, 3F, 0F, 4F, 0.1F, 1F);
                }
            }

            if (startRange != null) {
                hasRange = true;
                if (currentRange == null) {

                    if (!clientAndViaVersion) {
                        attackRangeMovement = player.clientVelocity.clone();
                    }
                    maxReach = startRange.getMaxRange();
                    hitboxMargin = startRange.getHitboxMargin();
                } else {

                    maxReach = Math.min(startRange.getMaxRange(), currentRange.getMaxRange());
                    hitboxMargin = Math.min(startRange.getHitboxMargin(), currentRange.getHitboxMargin());
                }
            }
        }

        boolean tooManyAttacks = playerAttackQueue.size() > 10;
        if (!tooManyAttacks) {
            playerAttackQueue.put(entityId, new InteractionData(
                    player.x, player.y, player.z,
                    hasRange, maxReach, hitboxMargin, attackRangeMovement
            ));
        }

        boolean knownInvalid = attackRangeMovement == null && isKnownInvalid(entity, hasRange, maxReach, hitboxMargin);

        if ((shouldModifyPackets() && cancelImpossibleHits && knownInvalid) || tooManyAttacks) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    private boolean isKnownInvalid(PacketEntity reachEntity, boolean hasAttackRange, float itemMaxReach, float itemHitboxMargin) {

        if ((blacklisted.contains(reachEntity.getType()) || !reachEntity.isLivingEntity) && reachEntity.getType() != EntityTypes.END_CRYSTAL)
            return false;

        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR)
            return false;
        if (player.inVehicle()) return false;

        if (cancelBuffer != 0) {
            CheckResult result = checkReach(reachEntity, player.x, player.y, player.z, hasAttackRange, itemMaxReach, itemHitboxMargin, null, true);
            return result.isFlag();
        } else {
            SimpleCollisionBox targetBox = getTargetBox(reachEntity);

            double maxReach = applyReachModifiers(targetBox, hasAttackRange, itemMaxReach, itemHitboxMargin, !player.packetStateData.didLastMovementIncludePosition);
            return ReachUtils.getMinReachToBox(player, targetBox) > maxReach;
        }
    }

    private void tickBetterReachCheckWithAngle() {
        for (Int2ObjectMap.Entry<InteractionData> attack : playerAttackQueue.int2ObjectEntrySet()) {
            PacketEntity reachEntity = player.compensatedEntities.entityMap.get(attack.getIntKey());
            if (reachEntity == null) continue;

            InteractionData interactionData = attack.getValue();
            CheckResult result = checkReach(reachEntity, interactionData.x, interactionData.y, interactionData.z, interactionData.hasAttackRange, interactionData.maxReach, interactionData.hitboxMargin, interactionData.attackRangeMovement, false);
            switch (result.type()) {
                case REACH -> flag(
                        V.write(verbose()).f64(result.minDistance()).uint(Math.max(0, reachEntity.getType().getId(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()))),
                        () -> {
                            String added = ", type=" + reachEntity.getType().getName().getKey();
                            if (reachEntity instanceof PacketEntitySizeable sizeable) {
                                added += ", size=" + sizeable.size;
                            }
                            return result.verbose() + added;
                        });
                case HITBOX -> {
                    String added = "type=" + reachEntity.getType().getName().getKey();
                    if (reachEntity instanceof PacketEntitySizeable sizeable) {
                        added += ", size=" + sizeable.size;
                    }
                    player.checkManager.get(Hitboxes.class).flag(result.verbose() + added);
                }
            }
        }

        playerAttackQueue.clear();
    }

    @NotNull
    private CheckResult checkReach(PacketEntity reachEntity, double x, double y, double z, boolean hasAttackRange, float itemMaxReach, float itemHitboxMargin, Vector3dm attackRangeMovement, boolean isPrediction) {
        SimpleCollisionBox targetBox = getTargetBox(reachEntity);

        double movementAllowance = attackRangeMovement == null ? 0 : getAttackRangeMovementAllowance(attackRangeMovement);
        double maxReach = applyReachModifiers(targetBox, hasAttackRange, itemMaxReach, itemHitboxMargin, !player.packetStateData.didLastLastMovementIncludePosition) + movementAllowance;
        double minDistance = Double.MAX_VALUE;

        List<Vector3dm> possibleLookDirs = new ArrayList<>(Collections.singletonList(ReachUtils.getLook(player, player.yaw, player.pitch)));

        if (!isPrediction) {

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                possibleLookDirs.add(ReachUtils.getLook(player, player.lastYaw, player.pitch));

                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                    possibleLookDirs.add(ReachUtils.getLook(player, player.lastYaw, player.lastPitch));
                }
            }
        }

        final double distance = maxReach + 3;

        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        for (Vector3dm lookVec : possibleLookDirs) {
            lookVec.multiply(distance);

            for (double eye : possibleEyeHeights) {
                final Vector3d eyePos = new Vector3d(x, y + eye, z);
                Vector3d endReachPos = eyePos.add(lookVec.getX(), lookVec.getY(), lookVec.getZ());

                Vector3d intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos).first();

                if (ReachUtils.isVecInside(targetBox, eyePos)) {
                    minDistance = 0;
                    break;
                }

                if (intercept != null) {
                    minDistance = Math.min(eyePos.distance(intercept), minDistance);
                }
            }
        }

        if ((!blacklisted.contains(reachEntity.getType()) && reachEntity.isLivingEntity) || reachEntity.getType() == EntityTypes.END_CRYSTAL) {
            if (minDistance == Double.MAX_VALUE) {
                cancelBuffer = 1;
                return new CheckResult(ResultType.HITBOX, 0, 0, false);
            } else if (minDistance > maxReach) {
                cancelBuffer = 1;
                return new CheckResult(ResultType.REACH, minDistance, movementAllowance, attackRangeMovement != null);
            } else {
                cancelBuffer = Math.max(0, cancelBuffer - 0.25);
            }
        }

        return NONE;
    }

    private SimpleCollisionBox getTargetBox(PacketEntity reachEntity) {
        if (reachEntity.getType() == EntityTypes.END_CRYSTAL) {
            return new SimpleCollisionBox(reachEntity.trackedServerPosition.getPos().subtract(1, 0, 1), reachEntity.trackedServerPosition.getPos().add(1, 2, 1));
        }
        return reachEntity.getPossibleCollisionBoxes();
    }

    private double getAttackRangeMovementAllowance(Vector3dm attackRangeMovement) {
        if (attackRangeMovement.lengthSquared() == 0) {
            return 0;
        }

        return getForwardMovement(attackRangeMovement, player.yaw, player.pitch);
    }

    private double getForwardMovement(Vector3dm movement, float yaw, float pitch) {
        return Math.max(0, movement.dot(ReachUtils.getLook(player, yaw, pitch)));
    }

    private static final boolean ATTACK_RANGE_COMPONENT_EXISTS = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_11);
    private static final boolean USE_1_8_HITBOX_MARGIN = PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8);

    private double applyReachModifiers(SimpleCollisionBox targetBox, boolean hasAttackRange, float itemMaxReach, float itemHitboxMargin, boolean giveMovementThreshold) {
        double maxReach;
        double hitboxMargin = threshold;

        if (hasAttackRange) {
            maxReach = itemMaxReach;
            hitboxMargin += itemHitboxMargin;
        } else {
            maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                hitboxMargin += 0.1f;
            }
        }

        if (giveMovementThreshold || player.canSkipTicks()) {
            hitboxMargin += player.getMovementThreshold();
        }

        targetBox.expand(hitboxMargin);

        return maxReach;
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        this.cancelImpossibleHits = config.getBooleanElse("Reach.block-impossible-hits", true);
        this.threshold = config.getDoubleElse("Reach.threshold", 0.0005);
    }

    private enum ResultType {
        REACH, HITBOX, NONE
    }

    private record CheckResult(ResultType type, double minDistance, double extraMovement, boolean hasExtraMovement) {
        public boolean isFlag() {
            return type != ResultType.NONE;
        }

        public String verbose() {
            if (type != ResultType.REACH) {
                return "";
            }

            String verbose = String.format("%.5f", minDistance) + " blocks";
            if (hasExtraMovement) {
                verbose += String.format(", extraMovement=%.5f", extraMovement);
            }
            return verbose;
        }
    }

    private record InteractionData(double x, double y, double z, boolean hasAttackRange,
                                   float maxReach, float hitboxMargin, Vector3dm attackRangeMovement) {}
}
