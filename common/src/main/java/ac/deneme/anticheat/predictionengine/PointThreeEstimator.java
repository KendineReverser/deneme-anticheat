package ac.deneme.anticheat.predictionengine;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.predictionengine.predictions.PredictionEngine;
import ac.deneme.anticheat.utils.collisions.CollisionData;
import ac.deneme.anticheat.utils.collisions.datatypes.CollisionBox;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.VectorData;
import ac.deneme.anticheat.utils.data.VelocityData;
import ac.deneme.anticheat.utils.data.tags.SyncedTags;
import ac.deneme.anticheat.utils.math.Vector3dm;
import ac.deneme.anticheat.utils.nmsutil.Collisions;
import ac.deneme.anticheat.utils.nmsutil.BlockProperties;
import ac.deneme.anticheat.utils.nmsutil.FluidTypeFlowing;
import ac.deneme.anticheat.utils.nmsutil.GetBoundingBox;
import ac.deneme.anticheat.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import lombok.Getter;
import lombok.Setter;

import java.util.OptionalInt;
import java.util.Set;

/**
 * A lot of care was put into handling all the stupid stuff occurring between events
 * <p>
 * Such as: Placing water and lava in a worldguard region to climb up walls within 0.03
 * A single tick of bubble columns
 * Placing ladders in worldguard regions
 * Some plugin thinking it's funny to spam levitation effects rapidly
 * Some plugin thinking it's funny to spam gravity effects rapidly
 * Someone trying to false deneme by using negative levitation effects
 * Open trapdoor, 0.03 upward into closed trapdoor, open trapdoor the tick before the next movement.
 * <p>
 * We must separate horizontal and vertical movement
 * The player can never actually control vertical movement directly
 * Vertically - we must compensate for gravity and for stepping movement
 * <p>
 * Stepping can be compensated for by expanding by 0.03, seting the vector down by the minimum movement allowed
 * and then moving the box up by the collision epsilon, and then pushing the box by 0.03 again
 * avoiding using the isEmpty() and rather using the collision move method, to avoid bypass/abuse
 * <p>
 * Jumping movement IS one of these starting vectors, although the length between the jump and
 * not jumping is outside the allowed vectors - as jumping cannot desync
 * <p>
 * Fluid pushing is quite strange - we simply expand by 0.03 and check for horizontal and vertical flowing.
 * As poses often desync, we cannot actually know the exact value.
 * <p>
 * Additionally, we must recheck for fluid between world updates to see if the player was swimming
 * or in lava at any point within the skipped tick
 * <p>
 * We must also check for a player starting gliding, stopping gliding, all within 0.03, which might
 * be possible due to mojang's implementation of gliding and netcode
 * <p>
 * We must also check for the user placing ladders, which gives them control of vertical movement
 * once again also between world changes
 * <p>
 * We must also be aware of sneaking, which is implemented terribly by mojang
 * There should be a post check for sending sneaking updates, but it's not implemented yet...
 * If the user has been sneaking for 2 movements without stopping, then we know that they are sneaking
 * This is due to poses being done AFTER the player moves, adding a 50 ms delay
 * And due to slowness processing BEFORE poses are updated, adding another 50 ms delay
 * However, on 1.13, the delay is instant because mojang wasn't given a chance to be incompetent -_-
 * <p>
 * We also must be aware of levitation from the last tick
 * We also must be aware of bubble columns
 * <p>
 * Additionally, because poses are done AFTER the previous tick, we must know the minimum height the player's
 * bounding box can be, to avoid noclip falses.  Funnily enough, vanilla falses due to this...
 * This is done because when the player can't have changed their pose for one tick, the second we know their god
 * damn pose.  The third tick fixes the slow movement desync.  Thanks a lot, mojang - for falsing
 * your own anticheat and not caring enough to fix it.  Causing this giant mess that we all know you won't
 * fix for another decade... and if you do fix it... you will only make it worse (remember the bucket desync?)
 * <p>
 * Call me out for the code (in this class) - but please put the blame on Mojang instead.  None of this would be needed
 * if Minecraft's netcode wasn't so terrible.
 * <p>
 * 1.18.2 fixes this issue.  However, this code must now be applied to tick skipping,
 * and I don't feel like writing another rant about tick skipping as mojang will never fix it, as it would
 * increase bandwidth usage.  At least it only causes falses occasionally, and not bypasses.
 */
public class PointThreeEstimator {
    private final DenemePlayer player;

    public boolean isNearFluid = false;

    private boolean headHitter = false;

    @Getter
    private boolean isNearClimbable = false;

    private boolean isGliding = false;

    private boolean gravityChanged = false;

    private boolean isNearHorizontalFlowingLiquid = false;
    private boolean isNearVerticalFlowingLiquid = false;
    private boolean isNearBubbleColumn = false;

    private int maxPositiveLevitation = Integer.MIN_VALUE;
    private int minNegativeLevitation = Integer.MAX_VALUE;

    @Setter
    @Getter
    private boolean isPushing = false;

    @Getter
    private boolean wasAlwaysCertain = true;

    public PointThreeEstimator(DenemePlayer player) {
        this.player = player;
    }

    public void handleChangeBlock(int x, int y, int z, WrappedBlockState state) {
        final StateType stateType = state.getType();
        CollisionBox data = CollisionData.getData(stateType).getMovementCollisionBox(player, player.getClientVersion(), state, x, y, z);
        SimpleCollisionBox normalBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player, player.x, player.y, player.z, 0.6f, 1.8f);

        final double movementThreshold = player.getMovementThreshold();
        SimpleCollisionBox slightlyExpanded = normalBox.copy().expand(movementThreshold, 0, movementThreshold);
        if (!slightlyExpanded.isIntersected(data) && slightlyExpanded.offset(0, movementThreshold, 0).isIntersected(data)) {
            headHitter = true;
        }

        final float collisionBoxThreshold = (float) (movementThreshold * 2.f);
        SimpleCollisionBox pointThreeBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player, player.x, player.y - movementThreshold, player.z, 0.6f + collisionBoxThreshold, 1.8f + collisionBoxThreshold);
        if ((Materials.isWater(player.getClientVersion(), state) || stateType == StateTypes.LAVA) &&
                pointThreeBox.isIntersected(new SimpleCollisionBox(x, y, z))) {

            if (stateType == StateTypes.BUBBLE_COLUMN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
                isNearBubbleColumn = true;
            }

            Vector3dm fluidVector = FluidTypeFlowing.getFlow(player, x, y, z);
            if (fluidVector.getX() != 0 || fluidVector.getZ() != 0) {
                isNearHorizontalFlowingLiquid = true;
            }
            if (fluidVector.getY() != 0) {
                isNearVerticalFlowingLiquid = true;
            }

            isNearFluid = true;
        }

        if (pointThreeBox.isIntersected(new SimpleCollisionBox(x, y, z)) && !data.isNull()) {

            int controllingEntityId = player.inVehicle() ? player.getRidingVehicleId() : player.entityID;

            final VelocityData oldFirstBreadKB = player.firstBreadKB;
            final VelocityData oldLikelyKB = player.likelyKB;
            player.firstBreadKB = player.checkManager.getKnockbackHandler().calculateFirstBreadKnockback(controllingEntityId, player.lastTransactionReceived.get());
            player.likelyKB = player.checkManager.getKnockbackHandler().calculateRequiredKB(controllingEntityId, player.lastTransactionReceived.get(), true);

            final VelocityData oldFirstBreadEx = player.firstBreadExplosion;
            final VelocityData oldLikelyEx = player.likelyExplosions;
            player.firstBreadExplosion = player.checkManager.getExplosionHandler().getFirstBreadAddedExplosion(player.lastTransactionReceived.get());
            player.likelyExplosions = player.checkManager.getExplosionHandler().getPossibleExplosions(player.lastTransactionReceived.get(), true);

            player.updateVelocityMovementSkipping();

            if (player.couldSkipTick) {
                player.uncertaintyHandler.lastPointThree.reset();
            } else {

                player.firstBreadKB = oldFirstBreadKB;
                player.likelyKB = oldLikelyKB;
                player.firstBreadExplosion = oldFirstBreadEx;
                player.likelyExplosions = oldLikelyEx;
            }
        }

        if (!player.inVehicle() && ((stateType == StateTypes.POWDER_SNOW && player.inventory.getBoots().getType() == ItemTypes.LEATHER_BOOTS)
                || player.tagManager.block(SyncedTags.CLIMBABLE).contains(stateType)) && pointThreeBox.isIntersected(new SimpleCollisionBox(x, y, z))) {
            isNearClimbable = true;
        }
    }

    /**
     * If a player's gravity changed, or they have levitation effects, it's safer to not predict their next gravity
     * and to just give them lenience
     */
    public boolean canPredictNextVerticalMovement() {
        return !gravityChanged && maxPositiveLevitation == Integer.MIN_VALUE && minNegativeLevitation == Integer.MAX_VALUE;
    }

    public double positiveLevitation(double y) {
        if (maxPositiveLevitation == Integer.MIN_VALUE) return y;
        return (0.05 * (maxPositiveLevitation + 1) - y * 0.2);
    }

    public double negativeLevitation(double y) {
        if (minNegativeLevitation == Integer.MAX_VALUE) return y;
        return (0.05 * (minNegativeLevitation + 1) - y * 0.2);
    }

    public boolean controlsVerticalMovement() {
        return isNearFluid || isNearClimbable || isNearHorizontalFlowingLiquid || isNearVerticalFlowingLiquid || isNearBubbleColumn || isGliding || player.uncertaintyHandler.influencedByBouncyBlock()
                || player.checkManager.getKnockbackHandler().isKnockbackPointThree() || player.checkManager.getExplosionHandler().isExplosionPointThree();
    }

    public void updatePlayerPotions(PotionType potion, Integer level) {
        if (potion == PotionTypes.LEVITATION) {
            maxPositiveLevitation = Math.max(level == null ? Integer.MIN_VALUE : level, maxPositiveLevitation);
            minNegativeLevitation = Math.min(level == null ? Integer.MAX_VALUE : level, minNegativeLevitation);
        }
    }

    public void updatePlayerGliding() {
        isGliding = true;
    }

    public void updatePlayerGravity() {
        gravityChanged = true;
    }

    public void endOfTickTick() {
        double movementThreshold = player.getMovementThreshold();
        float collisionBoxThreshold = (float) (movementThreshold * 2.f);
        SimpleCollisionBox pointThreeBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player, player.x, player.y - movementThreshold, player.z, 0.6f + collisionBoxThreshold, 1.8f + collisionBoxThreshold);

        SimpleCollisionBox oldBB = player.boundingBox;

        headHitter = false;

        for (float sizes : (player.skippedTickInActualMovement ? new float[]{0.6f, 1.5f, 1.8f} : new float[]{player.pose.height})) {

            player.boundingBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player, player.x, player.y + (sizes - 0.01f), player.z, 0.6f, 0.01f);
            headHitter = headHitter || Collisions.collide(player, 0, movementThreshold, 0).getY() != movementThreshold;
        }

        player.boundingBox = oldBB;

        checkNearbyBlocks(pointThreeBox);

        maxPositiveLevitation = Integer.MIN_VALUE;
        minNegativeLevitation = Integer.MAX_VALUE;

        isGliding = player.isGliding;
        gravityChanged = false;
        wasAlwaysCertain = true;
        isPushing = false;
    }

    private void checkNearbyBlocks(SimpleCollisionBox pointThreeBox) {

        isNearHorizontalFlowingLiquid = false;
        isNearVerticalFlowingLiquid = false;
        isNearClimbable = false;
        isNearBubbleColumn = false;
        isNearFluid = false;

        Collisions.hasMaterial(player, pointThreeBox, (block, x, y, z) -> {
            final StateType stateType = block.getType();
            if (player.tagManager.block(SyncedTags.CLIMBABLE).contains(stateType) || (stateType == StateTypes.POWDER_SNOW && !player.inVehicle() && player.inventory.getBoots().getType() == ItemTypes.LEATHER_BOOTS)) {
                isNearClimbable = true;
            }

            if (BlockTags.TRAPDOORS.contains(stateType)) {
                isNearClimbable = isNearClimbable || Collisions.trapdoorUsableAsLadder(player, x, y, z, block);
            }

            if (stateType == StateTypes.BUBBLE_COLUMN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
                isNearBubbleColumn = true;
            }

            if (Materials.isWater(player.getClientVersion(), block) || block.getType() == StateTypes.LAVA) {
                isNearFluid = true;
            }

            Vector3dm fluidVector = FluidTypeFlowing.getFlow(player, x, y, z);
            if (fluidVector.getX() != 0 || fluidVector.getZ() != 0) {
                isNearHorizontalFlowingLiquid = true;
            }

            if (fluidVector.getY() != 0) {
                isNearVerticalFlowingLiquid = true;
            }

            return false;
        });
    }

    public boolean closeEnoughToGroundToStepWithPointThree(VectorData data, double originalY) {
        if (player.inVehicle()) return false;
        if (!player.isPointThree()) return false;

        if (player.clientControlledVerticalCollision && data != null && data.isZeroPointZeroThree()) {
            return checkForGround(originalY);
        }

        return false;
    }

    private boolean checkForGround(double y) {
        SimpleCollisionBox playerBox = player.boundingBox;
        double threshold = player.getMovementThreshold();
        player.boundingBox = player.boundingBox.copy().expand(threshold, 0, threshold).offset(0, threshold, 0);

        double searchDistance = -0.2 + Math.min(0, y);
        Vector3dm collisionResult = Collisions.collide(player, 0, searchDistance, 0);
        player.boundingBox = playerBox;
        return collisionResult.getY() != searchDistance;
    }

    public boolean determineCanSkipTick(float speed, Set<VectorData> init) {

        if (!player.canSkipTicks() && player.packetStateData.didLastMovementIncludePosition && !player.uncertaintyHandler.isSteppingOnSlime) {
            return false;
        }

        double minimum = Double.MAX_VALUE;

        if ((player.isGliding || player.wasGliding) && !player.packetStateData.didLastMovementIncludePosition) {
            return true;
        }

        if (player.inVehicle()) {
            return false;
        }

        if (isNearClimbable() || isPushing || player.fireworks.getMaxFireworksAppliedPossible() > 0) {
            return true;
        }

        boolean couldStep = player.isPointThree() && checkForGround(player.clientVelocity.getY());

        for (VectorData data : init) {

            Vector3dm toZeroVec = new PredictionEngine().handleStartingVelocityUncertainty(player, data, new Vector3dm());

            Vector3dm collisionResult = Collisions.collide(player, toZeroVec.getX(), toZeroVec.getY(), toZeroVec.getZ(), Integer.MIN_VALUE, null);

            boolean likelyStepSkip = player.isPointThree() && (data.vector.getY() > -0.08 && data.vector.getY() < 0.06) && couldStep;

            double minHorizLength = Math.max(0, Math.hypot(collisionResult.getX(), collisionResult.getZ()) - speed);

            boolean forcedNo003 = data.isExplosion() || data.isKnockback();

            double length = Math.hypot((!forcedNo003 && player.lastOnGround) || (likelyStepSkip || controlsVerticalMovement()) ? 0 : Math.abs(collisionResult.getY()), minHorizLength);

            minimum = Math.min(minimum, length);

            if (minimum < player.getMovementThreshold()) break;
        }

        return minimum < player.getMovementThreshold();
    }

    public double getHorizontalFluidPushingUncertainty(VectorData vector) {

        return isNearHorizontalFlowingLiquid && vector.isZeroPointZeroThree() ? 0.014 * 2 : 0;
    }

    public double getVerticalFluidPushingUncertainty(VectorData vector) {

        return (isNearBubbleColumn || isNearVerticalFlowingLiquid) && vector.isZeroPointZeroThree() ? 0.014 * 2 : 0;
    }

    public double getVerticalBubbleUncertainty(VectorData vectorData) {
        return isNearBubbleColumn && vectorData.isZeroPointZeroThree() ? 0.35 : 0;
    }

    public double getAdditionalVerticalUncertainty(VectorData vector) {
        double fluidAddition = vector.isZeroPointZeroThree() ? 0.014 : 0;

        if (player.inVehicle()) return 0;

        if (headHitter) {
            wasAlwaysCertain = false;

            return -Math.max(0, vector.vector.getY()) - 0.1 - fluidAddition;
        }

        if (!vector.isZeroPointZeroThree()) return 0;

        double minMovement = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 0.003 : 0.005;

        double yVel = vector.vector.getY();
        double maxYTraveled = 0;
        boolean first = true;
        do {

            if (Math.abs(yVel) < minMovement) yVel = 0;

            if (!first) {
                maxYTraveled += yVel;
            }

            if (!first && yVel == 0) {
                break;
            }

            first = false;

            yVel = iterateGravity(player, yVel);

            if (yVel == 0) break;
        } while (Math.abs(maxYTraveled + vector.vector.getY()) < player.getMovementThreshold());

        if (maxYTraveled != 0) {
            wasAlwaysCertain = false;
        }

        return maxYTraveled;
    }

    private double iterateGravity(DenemePlayer player, double y) {
        final OptionalInt levitation = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.LEVITATION);
        if (levitation.isPresent()) {

            y += (0.05 * (levitation.getAsInt() + 1) - y) * 0.2;
        } else if (player.hasGravity) {

            double gravity = player.compensatedEntities.self.getAttributeValue(Attributes.GRAVITY);
            if (y <= 0 && player.compensatedEntities.getSlowFallingAmplifier().isPresent()) {
                gravity = player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5) ? 0.01 : Math.min(gravity, 0.01);
            }
            y -= gravity;
        }

        return y * BlockProperties.getModifiedAirDrag(0.98F, player);
    }
}
