package ac.deneme.anticheat.predictionengine.movementtick;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.predictionengine.PlayerBaseTick;
import ac.deneme.anticheat.predictionengine.blockeffects.PotentSulfurGeyser;
import ac.deneme.anticheat.predictionengine.predictions.PredictionEngine;
import ac.deneme.anticheat.predictionengine.predictions.PredictionEngineElytra;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.VectorData;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityStrider;
import ac.deneme.anticheat.utils.data.tags.SyncedTags;
import ac.deneme.anticheat.utils.enums.FluidTag;
import ac.deneme.anticheat.utils.math.DenemeMath;
import ac.deneme.anticheat.utils.math.Vector3dm;
import ac.deneme.anticheat.utils.nmsutil.BlockProperties;
import ac.deneme.anticheat.utils.nmsutil.Collisions;
import ac.deneme.anticheat.utils.nmsutil.EntityTypeTags;
import ac.deneme.anticheat.utils.nmsutil.FluidFallingAdjustedMovement;
import ac.deneme.anticheat.utils.nmsutil.GetBoundingBox;
import ac.deneme.anticheat.utils.nmsutil.MainSupportingBlockPosFinder;
import ac.deneme.anticheat.utils.nmsutil.StuckSpeed;
import ac.deneme.anticheat.utils.team.EntityPredicates;
import ac.deneme.anticheat.utils.team.EntityTeam;
import ac.deneme.anticheat.utils.team.TeamHandler;
import ac.deneme.anticheat.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.viaversion.viaversion.api.Via;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Set;

@RequiredArgsConstructor
public class MovementTicker {
    public final DenemePlayer player;

    public static void handleEntityCollisions(DenemePlayer player) {

        final boolean serverSupported = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9);
        boolean hasEntityPushing = !(player.getClientVersion().isOlderThan(ClientVersion.V_1_9)

                || (!serverSupported
                && (!ViaVersionUtil.isAvailable || Via.getConfig().isPreventCollision())));
        if (!hasEntityPushing) return;

        int possibleCollidingEntities = 0;
        int possibleRiptideEntities = 0;

        if (!player.inVehicle() && player.gamemode != GameMode.SPECTATOR) {

            SimpleCollisionBox playerBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player, player.lastX, player.lastY, player.lastZ, 0.6f, 1.8f);
            playerBox.encompass(GetBoundingBox.getBoundingBoxFromPosAndSize(player, player.x, player.y, player.z, 0.6f, 1.8f).expand(player.getMovementThreshold()));
            playerBox.expand(0.2);

            final TeamHandler teamHandler = player.checkManager.get(TeamHandler.class);
            final EntityTeam playerTeam = teamHandler != null ? teamHandler.getPlayerTeam() : null;
            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {

                SimpleCollisionBox entityBox = entity.getPossibleCollisionBoxes();
                if (!playerBox.isCollided(entityBox)) continue;

                possibleRiptideEntities++;

                if (!entity.isPushable()) continue;

                if (serverSupported) {
                    final EntityTeam entityTeam = teamHandler != null ? teamHandler.getEntityTeam(entity) : null;
                    if (!EntityPredicates.canBePushedBy(entityTeam, playerTeam)) continue;
                }

                possibleCollidingEntities++;
            }
        }

        if (player.isGliding && possibleCollidingEntities > 0) {

            player.uncertaintyHandler.yNegativeUncertainty -= 0.05;
            player.uncertaintyHandler.yPositiveUncertainty += 0.05;
        }

        player.uncertaintyHandler.riptideEntities.add(possibleRiptideEntities);
        player.uncertaintyHandler.collidingEntities.add(possibleCollidingEntities);
    }

    private boolean isHorizontalCollisionSoft(Vector3dm collide) {
        double horizontalLengthSquared = collide.getX() * collide.getX() + collide.getZ() * collide.getZ();
        if (horizontalLengthSquared < 1E-5F) return false;

        float yawInRadians = player.yaw * (float) (Math.PI / 180.0);
        double sin = player.trigHandler.sin(yawInRadians);
        double cos = player.trigHandler.cos(yawInRadians);

        Vector3dm input = player.predictedVelocity.input;
        if (input != null) {
            return isHorizontalCollisionSoft(collide, horizontalLengthSquared, sin, cos, (float) input.getX(), (float) input.getZ());
        } else {
            ArrayList<VectorData> results = new ArrayList<>();
            new PredictionEngine().loopVectors(
                    player,
                    Set.of(new VectorData(new Vector3dm(), VectorData.VectorType.Normal)),
                    0,
                    results,
                    false
            );

            for (VectorData data : results) {
                if (isHorizontalCollisionSoft(
                        collide, horizontalLengthSquared,
                        sin, cos,
                        (float) data.input.getX(), (float) data.input.getZ())) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean isHorizontalCollisionSoft(
            Vector3dm collide, double horizontalLengthSquared,
            double sin, double cos,
            float xxa, float zza) {
        double g = xxa * cos - zza * sin;
        double h = zza * cos + xxa * sin;
        double i = g * g + h * h;
        return i >= 1E-5F && Math.acos((g * collide.getX() + h * collide.getZ()) / Math.sqrt(i * horizontalLengthSquared)) < 0.13962634F;
    }

    public void move(Vector3dm inputVel, Vector3dm collide) {
        if (player.stuckSpeedMultiplier.getX() < 0.99) {
            player.clientVelocity = new Vector3dm();
        }

        double deltaX = player.clientVelocity.getX(), deltaY = player.clientVelocity.getY(), deltaZ = player.clientVelocity.getZ();
        final boolean xAxis;
        final boolean zAxis;
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2)) {
            xAxis = !DenemeMath.equal(inputVel.getX(), collide.getX());
            zAxis = !DenemeMath.equal(inputVel.getZ(), collide.getZ());

            player.horizontalCollision = xAxis || zAxis;
            player.softHorizontalCollision = player.horizontalCollision && isHorizontalCollisionSoft(collide);
        } else {
            xAxis = inputVel.getX() != collide.getX();
            zAxis = inputVel.getZ() != collide.getZ();
            player.horizontalCollision = xAxis || zAxis;
        }

        player.verticalCollision = inputVel.getY() != collide.getY();

        boolean calculatedOnGround = (player.verticalCollision && inputVel.getY() < 0.0D);

        if (inputVel.getY() == -SimpleCollisionBox.COLLISION_EPSILON && collide.getY() > -SimpleCollisionBox.COLLISION_EPSILON && collide.getY() <= 0 && !player.inVehicle())
            calculatedOnGround = player.onGround;
        player.clientClaimsLastOnGround = player.onGround;

        if (player.inVehicle() && player.clientControlledVerticalCollision && player.uncertaintyHandler.isStepMovement &&
                (inputVel.getY() <= 0 || player.predictedVelocity.isSwimHop())) {
            calculatedOnGround = true;
        }

        if (player.inVehicle() || !player.exemptOnGround()) {
            player.onGround = calculatedOnGround;
        }

        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z);

        final PacketEntity riding = player.compensatedEntities.self.getRiding();

        if (player.getClientVersion() != ClientVersion.V_1_21_4 && (!player.wasTouchingWater && (riding == null || (!riding.isBoat && !riding.isHappyGhast)))) {
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_21_11)) {
                PlayerBaseTick.updateInWaterStateAndDoWaterCurrentPushing(player);
            } else {
                PlayerBaseTick.updateFluidInteraction(player);
            }
        }

        if (player.onGround) {
            player.fallDistance = 0;
        } else if (collide.getY() < 0) {
            player.fallDistance -= collide.getY();
            player.vehicleData.lastYd = collide.getY();
        }

        if (xAxis) player.clientVelocity.setX(BlockProperties.getVelocityAfterHorizontalCollision(player, player.clientVelocity.getX()));
        if (zAxis) player.clientVelocity.setZ(BlockProperties.getVelocityAfterHorizontalCollision(player, player.clientVelocity.getZ()));

        if (riding instanceof PacketEntityStrider) {
            Collisions.handleInsideBlocks(player);
        }

        player.mainSupportingBlockData = MainSupportingBlockPosFinder.findMainSupportingBlockPos(player, player.mainSupportingBlockData, new Vector3d(collide.getX(), collide.getY(), collide.getZ()), player.boundingBox, player.onGround);
        StateType onBlock = BlockProperties.getOnPos(player, player.mainSupportingBlockData, new Vector3d(player.x, player.y, player.z));

        if (inputVel.getY() != collide.getY()) {
            float blockRestitution = BlockProperties.getBlockBounceRestitution(onBlock, player);
            boolean suppressesBounce = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_2) && BlockTags.SUPPRESSES_BOUNCE.contains(onBlock);
            if (blockRestitution > 0.0F || !suppressesBounce && BlockProperties.getEntityBounciness(player) > 0.0F) {
                applyBlockBounce(blockRestitution, collide, riding);
            } else {
                player.clientVelocity.setY(0);
            }
        }

        collide = PredictionEngine.clampMovementToHardBorder(player, collide);

        if (collide.lengthSquared() <= 1e-7

                && (player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2) || inputVel.lengthSquared() - collide.lengthSquared() >= 1e-7)) {
            collide = new Vector3dm();
        } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_5)) {
            Vector3d from = new Vector3d(player.lastX, player.lastY, player.lastZ);
            Vector3d to = new Vector3d(player.x, player.y, player.z);

            player.addMovementThisTick(new DenemePlayer.Movement(from, to, new Vector3d(deltaX, deltaY, deltaZ)));
        }

        player.predictedVelocity = new VectorData(collide.clone(), player.predictedVelocity.lastVector, player.predictedVelocity.vectorType);

        float f = BlockProperties.getBlockSpeedFactor(player, player.mainSupportingBlockData, new Vector3d(player.x, player.y, player.z));
        player.clientVelocity.multiply(f, 1, f);

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2)) {
            return;
        }

        if (player.stuckSpeedMultiplier.getX() < 0.99) {
            player.uncertaintyHandler.lastStuckSpeedMultiplier.reset();
        }

        player.resetStuckSpeedMultiplier();

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_16))
            player.wasTouchingLava = false;

        Collisions.handleInsideBlocks(player);

        if (player.stuckSpeedMultiplier.getX() < 0.9) {

            player.fallDistance = 0;
        }

        if (player.isFlying) {
            player.stuckSpeedMultiplier = StuckSpeed.NONE;
        }
    }

    public void livingEntityAIStep() {
        handleEntityCollisions(player);

        SimpleCollisionBox oldBB = player.boundingBox.copy();

        if (!player.inVehicle()) {
            playerEntityTravel();
        } else {
            livingEntityTravel();
        }

        player.uncertaintyHandler.xNegativeUncertainty = 0;
        player.uncertaintyHandler.xPositiveUncertainty = 0;
        player.uncertaintyHandler.yNegativeUncertainty = 0;
        player.uncertaintyHandler.yPositiveUncertainty = 0;
        player.uncertaintyHandler.zNegativeUncertainty = 0;
        player.uncertaintyHandler.zPositiveUncertainty = 0;

        if (player.uncertaintyHandler.lastTeleportTicks.hasOccurredSince(0)) {
            player.uncertaintyHandler.yNegativeUncertainty -= 0.02;
        }

        if (player.isFlying) {
            SimpleCollisionBox playerBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
            if (!Collisions.isEmpty(player, playerBox.copy().offset(0, 0.1, 0))) {
                player.uncertaintyHandler.yPositiveUncertainty = player.flySpeed * 5;
            }

            if (!Collisions.isEmpty(player, playerBox.copy().offset(0, -0.1, 0))) {
                player.uncertaintyHandler.yNegativeUncertainty = player.flySpeed * -5;
            }
        }

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2))
            return;

        oldBB.expand(-SimpleCollisionBox.COLLISION_EPSILON);

        double posX = Math.max(0, player.predictedVelocity.vector.getX()) + SimpleCollisionBox.COLLISION_EPSILON;
        double negX = Math.min(0, player.predictedVelocity.vector.getX()) - SimpleCollisionBox.COLLISION_EPSILON;
        double posZ = Math.max(0, player.predictedVelocity.vector.getZ()) + SimpleCollisionBox.COLLISION_EPSILON;
        double negZ = Math.min(0, player.predictedVelocity.vector.getZ()) - SimpleCollisionBox.COLLISION_EPSILON;

        boolean xAxisCollision = !Collisions.isEmpty(player, oldBB.expandMin(negX, 0, 0).expandMax(posX, 0, 0));
        boolean zAxisCollision = !Collisions.isEmpty(player, oldBB.expandMin(0, 0, negZ).expandMax(0, 0, posZ));

        zAxisCollision = zAxisCollision || player.actualMovement.getZ() == 0;

        if (zAxisCollision && xAxisCollision) {
            double playerSpeed = player.speed;

            if (player.wasTouchingWater) {
                float swimSpeed = 0.02F;
                if (player.depthStriderLevel > 0.0F) {
                    swimSpeed += (((float) player.speed) - swimSpeed) * player.depthStriderLevel / 3.0F;
                }
                playerSpeed = swimSpeed;
            } else if (player.wasTouchingLava) {
                playerSpeed = 0.02F;
            } else if (player.isGliding) {
                playerSpeed = 0.4;

                player.uncertaintyHandler.yNegativeUncertainty -= 0.05;
                player.uncertaintyHandler.yPositiveUncertainty += 0.05;
            }

            player.uncertaintyHandler.xNegativeUncertainty -= playerSpeed * 3;
            player.uncertaintyHandler.xPositiveUncertainty += playerSpeed * 3;
        }
    }

    public void playerEntityTravel() {
        if (player.isFlying && !player.inVehicle()) {
            double oldY = player.clientVelocity.getY();
            double oldYJumping = oldY + player.flySpeed * 3;
            livingEntityTravel();

            if (player.predictedVelocity.isKnockback() || player.predictedVelocity.isTrident()
                    || player.uncertaintyHandler.yPositiveUncertainty != 0 || player.uncertaintyHandler.yNegativeUncertainty != 0 || player.isGliding) {
                player.clientVelocity.setY(player.actualMovement.getY() * 0.6);
            } else if (Math.abs(oldY - player.actualMovement.getY()) < (oldYJumping - player.actualMovement.getY())) {
                player.clientVelocity.setY(oldY * 0.6);
            } else {
                player.clientVelocity.setY(oldYJumping * 0.6);
            }

        } else {
            livingEntityTravel();
        }
    }

    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
    }

    public void doLavaMove() {
    }

    public void doNormalMove(float blockFriction) {
    }

    private void applyBlockBounce(float blockRestitution, Vector3dm movement, PacketEntity riding) {
        if (player.isSneaking) {
            player.clientVelocity.setY(0);
            return;
        }

        double currentY = player.clientVelocity.getY();
        if (currentY >= 0.0) {
            return;
        }

        double restitution = blockRestitution;
        if (riding != null && !riding.isLivingEntity) {
            restitution *= 0.8F;
        }

        if (player.getClientVersion().isOlderThan(ClientVersion.V_26_2)) {
            player.clientVelocity.setY(-currentY * restitution);
            return;
        }

        restitution = Math.max(restitution, BlockProperties.getEntityBounciness(player));
        player.clientVelocity.setY(BlockProperties.getVelocityAfterVerticalCollision(
                player, currentY, movement.getY(), restitution));
    }

    public void livingEntityTravel() {
        double playerGravity = !player.inVehicle()
                ? player.compensatedEntities.self.getAttributeValue(Attributes.GRAVITY)
                : player.compensatedEntities.self.getRiding().getAttributeValue(Attributes.GRAVITY);

        boolean isFalling = player.actualMovement.getY() <= 0.0;
        if (isFalling && player.compensatedEntities.getSlowFallingAmplifier().isPresent()) {
            playerGravity = player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5) ? 0.01 : Math.min(playerGravity, 0.01);

            player.fallDistance = 0;
        }

        player.gravity = playerGravity;

        float swimFriction;

        double lavaLevel = 0;
        if (canStandOnLava())
            lavaLevel = player.compensatedWorld.getLavaFluidLevelAt(DenemeMath.floor(player.lastX), DenemeMath.floor(player.lastY), DenemeMath.floor(player.lastZ));

        if (player.wasTouchingWater && !player.isFlying) {

            boolean isSkeletonHorse = player.inVehicle() && player.compensatedEntities.self.getRiding().getType() == EntityTypes.SKELETON_HORSE && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13);
            swimFriction = player.isSprinting && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) ? 0.9F : (isSkeletonHorse ? 0.96F : 0.8F);
            float swimSpeed = 0.02F;

            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21) && player.depthStriderLevel > 3.0F) {
                player.depthStriderLevel = 3.0F;
            }

            if (!player.lastOnGround) {
                player.depthStriderLevel *= 0.5F;
            }

            if (player.depthStriderLevel > 0.0F) {
                final float divisor = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21) ? 1.0F : 3.0F;
                swimFriction += (0.54600006F - swimFriction) * player.depthStriderLevel / divisor;
                swimSpeed += (((float) player.speed) - swimSpeed) * player.depthStriderLevel / divisor;
            }

            if (player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.DOLPHINS_GRACE).isPresent()) {
                swimFriction = 0.96F;
            }

            player.friction = swimFriction;
            doWaterMove(swimSpeed, isFalling, swimFriction);

            player.isClimbing = Collisions.onClimbable(player, player.x, player.y, player.z);

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) && player.isClimbing) {
                player.lastWasClimbing = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity.clone().setY(0.2D * 0.8F)).getY();
            }

            floatInLiquidWhileRidden();
        } else {
            player.canFloatWhileRidden = false;
            if (player.wasTouchingLava && !player.isFlying && !(lavaLevel > 0 && canStandOnLava())) {
                player.friction = 0.5F;

                doLavaMove();

                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) && player.getFluidHeight(FluidTag.LAVA) <= 0.4D) {
                    player.clientVelocity = player.clientVelocity.multiply(0.5D, 0.800000011920929D, 0.5D);
                    player.clientVelocity = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity);
                } else {
                    player.clientVelocity.multiply(0.5D);
                }

                if (player.hasGravity)
                    player.clientVelocity.add(0.0D, -playerGravity / 4.0D, 0.0D);

                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_3)) {
                    floatInLiquidWhileRidden();
                }
            } else if (player.isGliding) {
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_5) && Collisions.onClimbable(player, player.lastX, player.lastY, player.lastZ)) {
                    float blockFriction = BlockProperties.getFriction(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ));
                    float airDrag = BlockProperties.getModifiedAirDrag(0.91F, player);
                    player.friction = player.lastOnGround ? BlockProperties.getModifiedFriction(blockFriction, player) * airDrag : airDrag;

                    doNormalMove(blockFriction);

                    player.isGliding = false;
                    player.pointThreeEstimator.updatePlayerGliding();
                } else {
                    player.friction = 0.99F;

                    if (player.clientVelocity.getY() > -0.5) {
                        player.fallDistance = 1;
                    }

                    new PredictionEngineElytra().guessBestMovement(0, player);
                }
            } else {
                float blockFriction = BlockProperties.getFriction(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ));
                float airDrag = BlockProperties.getModifiedAirDrag(0.91F, player);
                player.friction = player.lastOnGround ? BlockProperties.getModifiedFriction(blockFriction, player) * airDrag : airDrag;

                doNormalMove(blockFriction);
            }
        }

        Collisions.applyEffectsFromBlocks(player);
        PotentSulfurGeyser.launchEntityTicker(player, player.clientVelocity, true);
    }

    private boolean canFloatWhileRidden() {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21_11) || !player.inVehicle()) return false;

        PacketEntity vehicle = player.getVehicle();
        double fluidHeight = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_3)
                ? player.fluidInteraction.getFluidHeight(player.tagManager.fluid(SyncedTags.ENTITY_FLOATABLE))
                : player.getFluidHeight(FluidTag.WATER);
        return EntityTypeTags.CAN_FLOAT_WHILE_RIDDEN.anyOf(vehicle.getType()) && fluidHeight > 0.4;
    }

    private void floatInLiquidWhileRidden() {
        player.canFloatWhileRidden = canFloatWhileRidden();
        if (player.canFloatWhileRidden) {
            player.clientVelocity.add(0.0, 0.03999999910593033, 0.0);
        }
    }

    public boolean canStandOnLava() {
        return false;
    }
}
