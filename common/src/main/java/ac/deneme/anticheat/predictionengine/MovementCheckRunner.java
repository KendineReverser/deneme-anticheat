package ac.deneme.anticheat.predictionengine;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.checks.DenemeProcessor;
import ac.deneme.anticheat.checks.impl.prediction.Phase;
import ac.deneme.anticheat.checks.impl.vehicle.VehicleC;
import ac.deneme.anticheat.manager.SetbackTeleportUtil;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.predictionengine.movementtick.MovementTickerCamel;
import ac.deneme.anticheat.predictionengine.movementtick.MovementTickerHappyGhast;
import ac.deneme.anticheat.predictionengine.movementtick.MovementTickerHorse;
import ac.deneme.anticheat.predictionengine.movementtick.MovementTickerNautilus;
import ac.deneme.anticheat.predictionengine.movementtick.MovementTickerPig;
import ac.deneme.anticheat.predictionengine.movementtick.MovementTickerPlayer;
import ac.deneme.anticheat.predictionengine.movementtick.MovementTickerStrider;
import ac.deneme.anticheat.predictionengine.predictions.PredictionEngineNormal;
import ac.deneme.anticheat.predictionengine.predictions.rideable.PredictionEngineBoat;
import ac.deneme.anticheat.predictionengine.predictions.rideable.PredictionEngineRideableUtils;
import ac.deneme.anticheat.utils.anticheat.update.PositionUpdate;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.IndexedVector3d;
import ac.deneme.anticheat.utils.data.SetBackData;
import ac.deneme.anticheat.utils.data.VectorData;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityCamel;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityHappyGhast;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityHorse;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityNautilus;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityRideable;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityTrackXRot;
import ac.deneme.anticheat.utils.enums.Pose;
import ac.deneme.anticheat.utils.math.DenemeMath;
import ac.deneme.anticheat.utils.math.Vector3dm;
import ac.deneme.anticheat.utils.math.VectorUtils;
import ac.deneme.anticheat.utils.nmsutil.BlockProperties;
import ac.deneme.anticheat.utils.nmsutil.BoundingBoxSize;
import ac.deneme.anticheat.utils.nmsutil.Collisions;
import ac.deneme.anticheat.utils.nmsutil.GetBoundingBox;
import ac.deneme.anticheat.utils.nmsutil.Riptide;
import ac.deneme.anticheat.utils.nmsutil.StuckSpeed;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import org.jetbrains.annotations.NotNull;

public class MovementCheckRunner extends DenemeProcessor {

    public static double predictionNanos = 0.3 * 1e6;

    public static double longPredictionNanos = 0.3 * 1e6;
    private boolean allowSprintJumpingWithElytra = true;

    public MovementCheckRunner(DenemePlayer player) {
        super(player);
    }

    public void processAndCheckMovementPacket(PositionUpdate data) {

        if (player.getSetbackTeleportUtil().insideUnloadedChunk()) {

            final boolean invalidVehicle = player.inVehicle() &&
                    (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9) ||
                            player.getClientVersion().isOlderThan(ClientVersion.V_1_9));

            if (!invalidVehicle && !data.isTeleport()) {

                player.getSetbackTeleportUtil().executeNonSimulatingForceResync();
            }
        }

        long start = System.nanoTime();
        check(data);
        long length = System.nanoTime() - start;

        if (!player.disableDeneme) {
            predictionNanos = (predictionNanos * 499 / 500d) + (length / 500d);
            longPredictionNanos = (longPredictionNanos * 19999 / 20000d) + (length / 20000d);
        }
    }

    private void handleTeleport(PositionUpdate update) {
        player.lastX = player.x;
        player.lastY = player.y;
        player.lastZ = player.z;

        if (!player.inVehicle()) {
            if (update.getTeleportData() == null) {
                player.clientVelocity.setX(0);
                player.clientVelocity.setY(0);
                player.clientVelocity.setZ(0);
                player.lastWasClimbing = 0;
                player.canSwimHop = false;
            } else {
                final SetBackData setback = update.getSetback();
                if (setback == null || setback.getVelocity() == null) {
                    update.getTeleportData().modifyVector(player, player.clientVelocity);
                } else {

                    player.clientVelocity.setX(setback.getVelocity().getX());
                    player.clientVelocity.setY(setback.getVelocity().getY());
                    player.clientVelocity.setZ(setback.getVelocity().getZ());
                }
            }
        }

        player.uncertaintyHandler.lastTeleportTicks.reset();

        player.checkManager.getExplosionHandler().forceExempt();
        player.checkManager.getKnockbackHandler().forceExempt();

        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z);

        PredictionComplete predictionComplete = new PredictionComplete(0, update, true);
        player.getSetbackTeleportUtil().onPredictionComplete(predictionComplete);
        player.checkManager.get(Phase.class).onPredictionComplete(predictionComplete);

        player.uncertaintyHandler.lastHorizontalOffset = 0;
        player.uncertaintyHandler.lastVerticalOffset = 0;
    }

    private void check(PositionUpdate update) {
        if (update.isTeleport()) {
            handleTeleport(update);
            return;
        }

        player.intersectedWithNetherPortal = false;
        player.onGround = update.isOnGround();

        if (!player.isFlying && player.isSneaking && Collisions.isAboveGround(player)) {

            double posX = Math.max(0.05, DenemeMath.clamp(player.actualMovement.getX(), -16, 16) + 0.05);
            double posZ = Math.max(0.05, DenemeMath.clamp(player.actualMovement.getZ(), -16, 16) + 0.05);
            double negX = Math.min(-0.05, DenemeMath.clamp(player.actualMovement.getX(), -16, 16) - 0.05);
            double negZ = Math.min(-0.05, DenemeMath.clamp(player.actualMovement.getZ(), -16, 16) - 0.05);

            Vector3dm NE = Collisions.maybeBackOffFromEdge(new Vector3dm(posX, 0, negZ), player, true);
            Vector3dm NW = Collisions.maybeBackOffFromEdge(new Vector3dm(negX, 0, negZ), player, true);
            Vector3dm SE = Collisions.maybeBackOffFromEdge(new Vector3dm(posX, 0, posZ), player, true);
            Vector3dm SW = Collisions.maybeBackOffFromEdge(new Vector3dm(negX, 0, posZ), player, true);

            boolean isEast = NE.getX() != posX || SE.getX() != posX;
            boolean isWest = NW.getX() != negX || SW.getX() != negX;
            boolean isNorth = NE.getZ() != negZ || NW.getZ() != negZ;
            boolean isSouth = SE.getZ() != posZ || SW.getZ() != posZ;

            if (isEast) player.uncertaintyHandler.lastStuckEast.reset();
            if (isWest) player.uncertaintyHandler.lastStuckWest.reset();
            if (isNorth) player.uncertaintyHandler.lastStuckNorth.reset();
            if (isSouth) player.uncertaintyHandler.lastStuckSouth.reset();

            if (isEast || isWest || isSouth || isNorth) {
                player.uncertaintyHandler.stuckOnEdge.reset();
            }
        }

        player.compensatedWorld.tickPlayerInPistonPushingArea();
        player.compensatedEntities.tick();

        if (player.vehicleData.wasVehicleSwitch || player.vehicleData.lastDummy) {
            player.uncertaintyHandler.lastVehicleSwitch.reset();
        }

        if (player.vehicleData.lastDummy) {
            player.clientVelocity.multiply(0.98);
        }

        final PacketEntity riding = player.compensatedEntities.self.getRiding();
        if (player.vehicleData.wasVehicleSwitch || player.vehicleData.lastDummy) {
            update.setTeleport(true);

            player.vehicleData.lastDummy = false;
            player.vehicleData.wasVehicleSwitch = false;

            if (riding != null) {
                SimpleCollisionBox interTruePositions = riding.getPossibleCollisionBoxes();

                final float scale = (float) riding.getAttributeValue(Attributes.SCALE);
                float width = BoundingBoxSize.getWidth(player, riding) * scale;
                float height = BoundingBoxSize.getHeight(player, riding) * scale;
                interTruePositions.expand(-width, 0, -width);
                interTruePositions.expandMax(0, -height, 0);

                Vector3dm cutTo = VectorUtils.cutBoxToVector(player.x, player.y, player.z, interTruePositions);

                player.lastX = cutTo.getX();
                player.lastY = cutTo.getY();
                player.lastZ = cutTo.getZ();

                player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
            } else {

                if (new Vector3dm(player.lastX, player.lastY, player.lastZ).distance(new Vector3dm(player.x, player.y, player.z)) > 3) {
                    player.getSetbackTeleportUtil().executeForceResync();
                }

                handleTeleport(update);

                if (player.isClimbing) {
                    Vector3dm ladder = player.clientVelocity.clone().setY(0.2);
                    PredictionEngineNormal.staticVectorEndOfTick(player, ladder, player.clientVelocity.getY());
                    player.lastWasClimbing = ladder.getY();
                }
                return;
            }
        }

        if (player.isInBed != player.lastInBed) {
            update.setTeleport(true);
        }
        player.lastInBed = player.isInBed;

        if (player.isInBed) return;

        if (!player.inVehicle()) {
            player.speed = player.compensatedEntities.self.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (player.hasGravity != player.playerEntityHasGravity) {
                player.pointThreeEstimator.updatePlayerGravity();
            }
            player.hasGravity = player.playerEntityHasGravity;
        }

        if (player.inVehicle()) {

            player.checkManager.getExplosionHandler().forceExempt();

            riding.setPositionRaw(player, new SimpleCollisionBox(player.x, player.y, player.z, player.x, player.y, player.z));

            if (riding instanceof PacketEntityTrackXRot boat) {
                boat.packetYaw = player.yaw;
                boat.interpYaw = player.yaw;
                boat.steps = 0;
            }

            if (player.hasGravity != riding.hasGravity) {
                player.pointThreeEstimator.updatePlayerGravity();
            }
            player.hasGravity = riding.hasGravity;

            if (riding instanceof PacketEntityRideable) {
                VehicleC vehicleC = player.checkManager.get(VehicleC.class);

                ItemType requiredItem = riding.getType() == EntityTypes.PIG ? ItemTypes.CARROT_ON_A_STICK : ItemTypes.WARPED_FUNGUS_ON_A_STICK;
                ItemStack mainHand = player.inventory.getHeldItem();
                ItemStack offHand = player.inventory.getOffHand();

                boolean correctMainHand = mainHand.getType() == requiredItem;
                boolean correctOffhand = offHand.getType() == requiredItem;

                if (!correctMainHand && !correctOffhand) {

                    vehicleC.flag();
                } else {
                    vehicleC.reward();
                }
            }
        }

        if (player.isFlying) {
            player.fallDistance = 0;
            player.uncertaintyHandler.lastFlyingTicks.reset();
        }

        player.isClimbing = Collisions.onClimbable(player, player.lastX, player.lastY, player.lastZ);

        player.clientControlledVerticalCollision = Math.abs(player.y % (1 / 64D)) < 0.00001;

        player.actualMovement = new Vector3dm(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

        if (player.isSprinting != player.lastSprinting) {
            player.compensatedEntities.hasSprintingAttributeEnabled = player.isSprinting;
        }

        player.lastJumping = player.isJumping;
        player.isJumping = player.packetStateData.knownInput.jump();

        boolean oldFlying = player.isFlying;
        boolean oldGliding = player.isGliding;
        boolean oldSprinting = player.isSprinting;
        boolean oldSneaking = player.isSneaking;

        if (player.inVehicle()) {

            player.isFlying = false;
            player.isGliding = false;
            player.isSprinting &= riding instanceof PacketEntityCamel;
            player.isSneaking = false;

            if (riding.getType() != EntityTypes.PIG && riding.getType() != EntityTypes.STRIDER) {
                player.isClimbing = false;
            }
        }

        if (!player.inVehicle()) {
            double movementSpeed = player.speed;
            if (player.compensatedEntities.hasSprintingAttributeEnabled) {
                movementSpeed += movementSpeed * 0.3F;
            }
            player.speed = (float) movementSpeed;
        }

        boolean clientClaimsRiptide = player.packetStateData.tryingToRiptide;
        if (player.packetStateData.tryingToRiptide) {
            long currentTime = System.currentTimeMillis();
            boolean isInWater = player.isInWaterOrRain();

            if (currentTime - player.packetStateData.lastRiptide < 450 || !isInWater) {
                player.packetStateData.tryingToRiptide = false;
            }

            player.packetStateData.lastRiptide = currentTime;
        }

        SimpleCollisionBox steppingOnBB = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ)
                .expand(player.getMovementThreshold())
                .offset(0.0, player.getClientVersion().isOlderThan(ClientVersion.V_1_15) ? -1.0 : -0.2, 0.0);
        Collisions.forEachCollisionBox(player, steppingOnBB, (block, x, y, z) -> {
            if (block.getType() == StateTypes.SLIME_BLOCK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                double blockHeight = 1D;
                double blockAboveHeight = BlockProperties.getBlockCollisionHeight(player, player.compensatedWorld.getBlock(x, y + 1, z));
                if (blockAboveHeight > 0D && blockAboveHeight < 1D) {
                    blockHeight += blockAboveHeight;
                }
                if (Math.abs((y + blockHeight) - player.lastY) <= player.getMovementThreshold()) {
                    player.uncertaintyHandler.isSteppingOnSlime = true;
                    player.uncertaintyHandler.isSteppingOnBouncyBlock = true;
                }
            }

            if (block.getType() == StateTypes.HONEY_BLOCK) {
                if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_14)
                        && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {

                    player.uncertaintyHandler.isSteppingOnBouncyBlock = true;
                }
                player.uncertaintyHandler.isSteppingOnHoney = true;
            }

            if (BlockTags.BEDS.contains(block.getType()) && Math.abs((y + 0.5625D) - player.lastY) <= player.getMovementThreshold() && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12)) {
                player.uncertaintyHandler.isSteppingOnBouncyBlock = true;
            }

            if (block.getType() == StateTypes.SHELF_MUSHROOM && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_3)
                    && (Math.abs((y + 0.6875) - player.lastY) <= player.getMovementThreshold()
                    || Math.abs((y + (block.getAge() == 0 ? 0.5625 : 0.5)) - player.lastY) <= player.getMovementThreshold())) {
                player.uncertaintyHandler.isSteppingOnBouncyBlock = true;
            }

            if (BlockTags.ICE.contains(block.getType())) {
                player.uncertaintyHandler.isSteppingOnIce = true;
            }

            if (block.getType() == StateTypes.BUBBLE_COLUMN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
                player.uncertaintyHandler.isSteppingNearBubbleColumn = true;
            }

            if (block.getType() == StateTypes.SCAFFOLDING) {
                player.uncertaintyHandler.isSteppingNearScaffolding = true;
            }
        });

        player.uncertaintyHandler.thisTickSlimeBlockUncertainty = player.uncertaintyHandler.nextTickSlimeBlockUncertainty;
        player.uncertaintyHandler.nextTickSlimeBlockUncertainty = 0;

        SimpleCollisionBox expandedBB = GetBoundingBox.getBoundingBoxFromPosAndSize(player, player.lastX, player.lastY, player.lastZ, 0.001f, 0.001f);

        if (player.actualMovement.lengthSquared() < 2500)
            expandedBB.expandToAbsoluteCoordinates(player.x, player.y, player.z);

        expandedBB.expand(Pose.STANDING.width / 2, 0, Pose.STANDING.width / 2);
        expandedBB.expandMax(0, Pose.STANDING.height, 0);

        boolean isGlitchy = player.uncertaintyHandler.isNearGlitchyBlock;

        player.uncertaintyHandler.isNearGlitchyBlock = player.getClientVersion().isOlderThan(ClientVersion.V_1_9)
                && Collisions.hasMaterial(player, expandedBB.copy().expand(0.2),
                (block, x, y, z) -> BlockTags.ANVIL.contains(block.getType())
                        || block.getType() == StateTypes.CHEST || block.getType() == StateTypes.TRAPPED_CHEST);

        player.uncertaintyHandler.isOrWasNearGlitchyBlock = isGlitchy || player.uncertaintyHandler.isNearGlitchyBlock;
        player.uncertaintyHandler.checkForHardCollision();

        if (player.isFlying != player.wasFlying)
            player.uncertaintyHandler.lastFlyingStatusChange.reset();

        if (!player.inVehicle() && (Math.abs(player.x) == 2.9999999E7D || Math.abs(player.z) == 2.9999999E7D)) {
            player.uncertaintyHandler.lastThirtyMillionHardBorder.reset();
        }

        if (player.isFlying && player.getClientVersion().isOlderThan(ClientVersion.V_1_13) && player.compensatedWorld.containsLiquid(player.boundingBox)) {
            player.uncertaintyHandler.lastUnderwaterFlyingHack.reset();
        }

        IndexedVector3d stuckSpeed = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) ? player.lastStuckSpeedMultiplier : player.stuckSpeedMultiplier;

        int stuckSpeedIndex = stuckSpeed.getIndex();
        int maxStuckSpeed = StuckSpeed.checkStuckSpeed(player, player.getMovementThreshold());
        int minStuckSpeed = StuckSpeed.checkStuckSpeed(player, -player.getMovementThreshold());

        boolean couldBeStuckSpeed = maxStuckSpeed != StuckSpeed.NONE.getIndex();
        boolean hasDifferentStuckSpeeds = maxStuckSpeed != minStuckSpeed;
        boolean hasMultipleStuckSpeeds = Integer.bitCount(maxStuckSpeed) > 1;
        boolean lastStuckSpeedNotIncluded = (minStuckSpeed & stuckSpeedIndex) == 0 && (stuckSpeed != StuckSpeed.NONE);

        player.uncertaintyHandler.shouldSimulateStuckSpeed = hasDifferentStuckSpeeds || hasMultipleStuckSpeeds || lastStuckSpeedNotIncluded;
        player.uncertaintyHandler.stuckSpeedMultiplierMask = maxStuckSpeed;

        if (couldBeStuckSpeed) {
            player.uncertaintyHandler.lastStuckSpeedMultiplier.reset();
        }

        player.startTickClientVel = player.clientVelocity;

        boolean wasChecked = false;

        if (player.compensatedEntities.self.isDead || (riding != null && riding.isDead)) {

            player.predictedVelocity = new VectorData(new Vector3dm(), VectorData.VectorType.Dead);
            player.clientVelocity = new Vector3dm();
        } else if (player.disableDeneme || (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8) && player.gamemode == GameMode.SPECTATOR) || player.isFlying || (player.isExemptElytra() && player.isGliding)) {

            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Spectator);
            player.clientVelocity = player.actualMovement.clone();
            player.gravity = 0;
            player.friction = BlockProperties.getModifiedAirDrag(0.91f, player);
            PredictionEngineNormal.staticVectorEndOfTick(player, player.clientVelocity);
        } else if (riding == null) {
            wasChecked = true;

            player.depthStriderLevel = (float) player.compensatedEntities.self.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
            player.sneakingSpeedMultiplier = (float) player.compensatedEntities.self.getAttributeValue(Attributes.SNEAKING_SPEED);

            player.verticalCollision = false;

            if (player.lastOnGround && player.packetStateData.tryingToRiptide && !player.inVehicle()) {
                Vector3dm pushingMovement = Collisions.collide(player, 0, 1.1999999F, 0);
                player.verticalCollision = pushingMovement.getY() != 1.1999999F;
                double currentY = player.clientVelocity.getY();

                if (likelyGroundRiptide(pushingMovement)) {
                    player.uncertaintyHandler.thisTickSlimeBlockUncertainty = Math.abs(Riptide.getRiptideVelocity(player).getY()) + (currentY > 0 ? currentY : 0);
                    player.uncertaintyHandler.nextTickSlimeBlockUncertainty = Math.abs(Riptide.getRiptideVelocity(player).getY()) + (currentY > 0 ? currentY : 0);

                    player.lastOnGround = false;
                    player.lastY += pushingMovement.getY();
                    PlayerBaseTick.updatePlayerPose(player);
                    player.boundingBox = GetBoundingBox.getPlayerBoundingBox(player, player.lastX, player.lastY, player.lastZ);
                    player.actualMovement = new Vector3dm(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

                    player.couldSkipTick = true;

                    Collisions.handleInsideBlocks(player);
                }
            }

            PlayerBaseTick.doBaseTick(player);
            new MovementTickerPlayer(player).livingEntityAIStep();
            PlayerBaseTick.updatePowderSnow(player);
            PlayerBaseTick.updatePlayerPose(player);
        } else if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            wasChecked = true;
            player.depthStriderLevel = 0f;

            if (riding.isBoat) {
                PlayerBaseTick.doBaseTick(player);

                new PredictionEngineBoat(player).guessBestMovement(0.1f, player);
            } else if (riding instanceof PacketEntityNautilus) {
                PlayerBaseTick.doBaseTick(player);
                new MovementTickerNautilus(player).livingEntityAIStep();
            } else if (riding instanceof PacketEntityCamel) {
                PlayerBaseTick.doBaseTick(player);
                new MovementTickerCamel(player).livingEntityAIStep();
            } else if (riding instanceof PacketEntityHappyGhast) {
                PlayerBaseTick.doBaseTick(player);
                new MovementTickerHappyGhast(player).livingEntityAIStep();
            } else if (riding instanceof PacketEntityHorse) {
                PlayerBaseTick.doBaseTick(player);
                new MovementTickerHorse(player).livingEntityAIStep();
            } else if (riding.getType() == EntityTypes.PIG) {
                PlayerBaseTick.doBaseTick(player);
                new MovementTickerPig(player).livingEntityAIStep();
            } else if (riding.getType() == EntityTypes.STRIDER) {
                PlayerBaseTick.doBaseTick(player);
                new MovementTickerStrider(player).livingEntityAIStep();
                MovementTickerStrider.floatStrider(player);
                Collisions.handleInsideBlocks(player);
            } else {
                wasChecked = false;
            }
        }

        double offset = player.predictedVelocity.vector.distance(player.actualMovement);
        offset = player.uncertaintyHandler.reduceOffset(offset);

        if (player.packetStateData.tryingToRiptide != clientClaimsRiptide) {
            player.getSetbackTeleportUtil().executeForceResync();
        }

        if (player.getSetbackTeleportUtil().getRequiredSetBack() != null && player.getSetbackTeleportUtil().getRequiredSetBack().getTicksComplete() == 1) {
            Vector3dm setbackVel = player.getSetbackTeleportUtil().getRequiredSetBack().getVelocity();

            if (player.predictedVelocity.isJump()
                    && !player.wasTouchingLava && !player.wasTouchingWater
                    && ((setbackVel != null && setbackVel.getY() >= 0) || !Collisions.slowCouldPointThreeHitGround(player, player.lastX, player.lastY, player.lastZ))) {
                player.getSetbackTeleportUtil().executeForceResync();
            }
            boolean lavaBugFix = player.wasTouchingLava && player.predictedVelocity.isJump() &&
                    player.predictedVelocity.vector.getY() < 0.06 && player.predictedVelocity.vector.getY() > -0.02;

            if (!player.predictedVelocity.isKnockback() && !lavaBugFix && player.getSetbackTeleportUtil().getRequiredSetBack().getVelocity() != null) {

                player.getSetbackTeleportUtil().executeForceResync();
            }
        }

        if (player.getSetbackTeleportUtil().blockOffsets) offset = 0;

        if (player.skippedTickInActualMovement || !wasChecked)
            player.uncertaintyHandler.lastPointThree.reset();

        player.checkManager.onPredictionFinish(new PredictionComplete(offset, update, wasChecked));

        player.wasLastPredictionCompleteChecked = wasChecked;

        if (player.platformPlayer != null && player.isGliding && player.predictedVelocity.isJump() && player.isSprinting && !allowSprintJumpingWithElytra) {
            SetbackTeleportUtil.SetbackPosWithVector lastKnownGoodPosition = player.getSetbackTeleportUtil().lastKnownGoodPosition;
            lastKnownGoodPosition.setVector(lastKnownGoodPosition.getVector().multiply(0.6 * 0.91, 1, 0.6 * 0.91));
            player.getSetbackTeleportUtil().executeNonSimulatingSetback();
        }

        if (!wasChecked) {

            player.checkManager.getExplosionHandler().forceExempt();
            player.checkManager.getKnockbackHandler().forceExempt();
        }

        player.lastOnGround = player.onGround;
        player.lastSprinting = player.isSprinting;
        player.lastSprintingForSpeed = player.isSprinting;
        player.wasFlying = player.isFlying;
        player.wasGliding = player.isGliding;
        player.wasSwimming = player.isSwimming;
        player.wasSneaking = player.isSneaking;
        player.packetStateData.tryingToRiptide = false;

        if (player.inVehicle()) {
            player.isFlying = oldFlying;
            player.isGliding = oldGliding;
            player.isSprinting = oldSprinting;
            player.isSneaking = oldSneaking;
        }

        player.riptideSpinAttackTicks--;
        if (player.predictedVelocity.isTrident())
            player.riptideSpinAttackTicks = 20;

        player.uncertaintyHandler.lastMovementWasZeroPointZeroThree = !player.inVehicle() && player.skippedTickInActualMovement;
        player.uncertaintyHandler.lastMovementWasUnknown003VectorReset = !player.inVehicle() && player.couldSkipTick
                && player.predictedVelocity.isKnockback()

                && !player.predictedVelocity.isSetbackKb(player);
        player.couldSkipTick = false;

        player.uncertaintyHandler.wasZeroPointThreeVertically = !player.inVehicle() &&
                ((player.uncertaintyHandler.lastMovementWasZeroPointZeroThree && player.pointThreeEstimator.controlsVerticalMovement())
                        || !player.pointThreeEstimator.canPredictNextVerticalMovement() || !player.pointThreeEstimator.isWasAlwaysCertain());

        player.uncertaintyHandler.lastPacketWasGroundPacket = player.uncertaintyHandler.onGroundUncertain;
        player.uncertaintyHandler.onGroundUncertain = false;

        PredictionEngineRideableUtils.applyPendingJumps(player);

        player.vehicleData.vehicleForward = (float) Math.min(0.98, Math.max(-0.98, player.vehicleData.nextVehicleForward));
        player.vehicleData.vehicleHorizontal = (float) Math.min(0.98, Math.max(-0.98, player.vehicleData.nextVehicleHorizontal));

        player.dashableEntities.tick();

        player.minAttackSlow = 0;
        player.maxAttackSlow = 0;

        player.likelyKB = null;
        player.firstBreadKB = null;
        player.firstBreadExplosion = null;
        player.likelyExplosions = null;

        player.trigHandler.setOffset(offset);
        player.pointThreeEstimator.endOfTickTick();
    }

    /**
     * Computes the movement from the riptide, and then uses it to determine whether the player
     * was more likely to be on or off of the ground when they started to riptide
     * <p>
     * A player on ground when riptiding will move upwards by 1.2f
     * We don't know whether the player was on the ground, however, which is why
     * we must attempt to guess here
     * <p>
     * Very reliable.
     *
     * @param pushingMovement The collision result when trying to move the player upwards by 1.2f
     * @return Whether it is more likely that this player was on the ground the tick they riptided
     */
    private boolean likelyGroundRiptide(Vector3dm pushingMovement) {

        double riptideYResult = Riptide.getRiptideVelocity(player).getY();

        double riptideDiffToBase = Math.abs(player.actualMovement.getY() - riptideYResult);
        double riptideDiffToGround = Math.abs(player.actualMovement.getY() - riptideYResult - pushingMovement.getY());

        return riptideDiffToGround < riptideDiffToBase;
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        allowSprintJumpingWithElytra = config.getBooleanElse("exploit.allow-sprint-jumping-when-using-elytra", true);
    }
}
