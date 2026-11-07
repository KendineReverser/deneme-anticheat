package ac.deneme.anticheat.player;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.AbstractCheck;
import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.PacketWorld;
import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.handler.ResyncHandler;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.impl.aim.processor.AimProcessor;
import ac.deneme.anticheat.checks.impl.misc.ClientBrand;
import ac.deneme.anticheat.checks.impl.misc.TransactionOrder;
import ac.deneme.anticheat.checks.impl.packetorder.PacketOrderProcessor;
import ac.deneme.anticheat.events.packets.CheckManagerListener;
import ac.deneme.anticheat.events.packets.PacketEntityReplication;
import ac.deneme.anticheat.manager.*;
import ac.deneme.anticheat.manager.player.features.FeatureManagerImpl;
import ac.deneme.anticheat.manager.player.handlers.DefaultResyncHandler;
import ac.deneme.anticheat.manager.player.handlers.NoOpResyncHandler;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.predictionengine.EntityFluidInteraction;
import ac.deneme.anticheat.predictionengine.MovementCheckRunner;
import ac.deneme.anticheat.predictionengine.PointThreeEstimator;
import ac.deneme.anticheat.predictionengine.UncertaintyHandler;
import ac.deneme.anticheat.manager.AttackCooldownHandler;
import ac.deneme.anticheat.predictionengine.blockeffects.CompensatedGeysers;
import ac.deneme.anticheat.predictionengine.blockeffects.PotentSulfurGeyser;
import ac.deneme.anticheat.utils.anticheat.LogUtil;
import ac.deneme.anticheat.utils.anticheat.MessageUtil;
import ac.deneme.anticheat.utils.anticheat.update.BlockBreak;
import ac.deneme.anticheat.utils.change.PlayerBlockHistory;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.*;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityHappyGhast;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityNautilus;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntitySelf;
import ac.deneme.anticheat.utils.data.tags.SyncedTags;
import ac.deneme.anticheat.utils.enums.FluidTag;
import ac.deneme.anticheat.utils.enums.Pose;
import ac.deneme.anticheat.utils.latency.*;
import ac.deneme.anticheat.utils.math.DenemeMath;
import ac.deneme.anticheat.utils.math.Location;
import ac.deneme.anticheat.utils.math.TrigHandler;
import ac.deneme.anticheat.utils.math.Vector3dm;
import ac.deneme.anticheat.utils.nmsutil.BlockProperties;
import ac.deneme.anticheat.utils.nmsutil.Collisions;
import ac.deneme.anticheat.utils.nmsutil.GetBoundingBox;
import ac.deneme.anticheat.utils.nmsutil.Materials;
import ac.deneme.anticheat.utils.nmsutil.StuckSpeed;
import ac.deneme.anticheat.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemEquippable;
import com.github.retrooper.packetevents.protocol.entity.EntityPositionData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.packet.PacketTracker;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.github.retrooper.packetevents.adventure.serializer.legacy.LegacyComponentSerializer;
import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DenemePlayer implements DenemeUser {
    public final @NotNull User user;
    public final @NotNull UUID uuid;
    public int entityID;
    public @MonotonicNonNull PlatformPlayer platformPlayer;

    public final @NotNull Queue<@NotNull ShortToLongPair> transactionsSent = new ConcurrentLinkedQueue<>();
    public final @NotNull Set<@NotNull Short> didWeSendThatTrans = ConcurrentHashMap.newKeySet();
    private final @NotNull AtomicInteger transactionIDCounter = new AtomicInteger(0);
    public final @NotNull AtomicInteger lastTransactionSent = new AtomicInteger(0);
    public final @NotNull AtomicInteger lastTransactionReceived = new AtomicInteger(0);

    public final @NotNull CheckManager checkManager;
    public final @NotNull AttackCooldownHandler attackCooldown;
    public final @NotNull PunishmentManager punishmentManager;
    public final @NotNull MovementCheckRunner movementCheckRunner;
    public final @NotNull SyncedTags tagManager;

    public @NotNull Vector3dm clientVelocity = new Vector3dm();
    private @MonotonicNonNull PacketTracker viaPacketTracker;
    public final @NotNull PacketOrderProcessor packetOrderProcessor = new PacketOrderProcessor(this);
    private long transactionPing;
    public long lastTransSent;
    public long lastTransReceived;
    private long playerClockAtLeast = System.nanoTime();
    public double lastWasClimbing;
    public boolean canSwimHop;
    public int riptideSpinAttackTicks;
    public int powderSnowFrozenTicks;
    public boolean hasGravity = true;
    public final long joinTime = System.currentTimeMillis();
    public boolean playerEntityHasGravity = true;
    public @NotNull VectorData predictedVelocity = new VectorData(new Vector3dm(), VectorData.VectorType.Normal);
    public @NotNull Vector3dm actualMovement = new Vector3dm();
    public @NotNull IndexedVector3d stuckSpeedMultiplier = StuckSpeed.NONE;
    public @NotNull IndexedVector3d lastStuckSpeedMultiplier = StuckSpeed.NONE;
    public final @NotNull UncertaintyHandler uncertaintyHandler;
    public double gravity;
    public float friction;
    public double speed;
    public final @NotNull Vector3dm filterMojangStupidityOnMojangStupidity = new Vector3dm();
    public double x;
    public double y;
    public double z;
    public double lastX;
    public double lastY;
    public double lastZ;

    public float yaw;
    public float pitch;
    public float lastYaw;
    public float lastPitch;
    public boolean onGround;
    public boolean lastOnGround;
    public boolean isSneaking;
    public boolean wasSneaking;
    public boolean isSprinting;
    public boolean lastSprinting;

    public boolean lastSprintingForSpeed;
    public boolean isFlying;
    public boolean canFly;
    public boolean wasFlying;
    public boolean isSwimming;
    public boolean wasSwimming;
    public boolean isClimbing;
    public boolean isGliding;
    public boolean wasGliding;
    public boolean isRiptidePose;
    public double fallDistance;
    public @NotNull SimpleCollisionBox boundingBox = GetBoundingBox.getBoundingBoxFromPosAndSizeRaw(x, y, z, 0.6f, 1.8f);
    public @NotNull Pose pose = Pose.STANDING;
    public @NotNull Pose lastPose = Pose.STANDING;

    public boolean isSlowMovement;
    public boolean isInBed;
    public boolean lastInBed;
    public int food = 20;
    public float depthStriderLevel;
    public float sneakingSpeedMultiplier = 0.3f;
    public float flySpeed;
    public final @NotNull VehicleData vehicleData = new VehicleData();

    public boolean clientClaimsLastOnGround;

    public boolean wasTouchingWater;
    public boolean wasWasTouchingWater;
    public boolean wasTouchingLava;
    public boolean wasEyeInWater;
    public @Nullable FluidTag fluidOnEyes;
    public boolean softHorizontalCollision;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean clientControlledVerticalCollision;

    public boolean couldSkipTick;

    public boolean skippedTickInActualMovement;

    public final @NotNull LastInstanceManager lastInstanceManager;
    public final @NotNull CompensatedFireworks fireworks;
    public final @NotNull CompensatedWorld compensatedWorld;
    public final @NotNull CompensatedGeysers compensatedGeysers;
    public final @NotNull CompensatedEntities compensatedEntities;
    public final @NotNull CompensatedDashableEntities dashableEntities;
    public final @NotNull CompensatedInventory inventory;
    public final @NotNull PacketEntityReplication packetEntityReplication = new PacketEntityReplication(this);
    public final @NotNull LatencyUtils latencyUtils = new LatencyUtils(this);
    public final @NotNull PointThreeEstimator pointThreeEstimator;
    public final @NotNull TrigHandler trigHandler = new TrigHandler(this);
    public final @NotNull PacketStateData packetStateData = new PacketStateData();

    public final @NotNull Vector3dm baseTickAddition = new Vector3dm();
    public final @NotNull Vector3dm baseTickWaterPushing = new Vector3dm();
    public @NotNull Vector3dm startTickClientVel = new Vector3dm();
    public @Nullable VelocityData firstBreadKB;
    public @Nullable VelocityData likelyKB;
    public @Nullable VelocityData firstBreadExplosion;
    public @Nullable VelocityData likelyExplosions;
    public int minAttackSlow;
    public int maxAttackSlow;
    public @MonotonicNonNull GameMode gamemode;
    public @MonotonicNonNull DimensionType dimensionType;
    public @Nullable String worldName;
    public @MonotonicNonNull Vector3d bedPosition;
    public long lastBlockPlaceUseItem;
    public long lastBlockBreak;
    public final @NotNull AtomicInteger cancelledPackets = new AtomicInteger(0);
    public @NotNull MainSupportingBlockData mainSupportingBlockData = MainSupportingBlockData.AIR_OFF_GROUND;
    public final @NotNull Object2DoubleMap<FluidTag> fluidHeight = new Object2DoubleArrayMap<>(2);

    public final double[][] possibleEyeHeights = new double[3][];
    public final @NotNull Queue<@NotNull BlockPlaceSnapshot> placeUseItemPackets = new LinkedBlockingQueue<>();
    public final @NotNull Queue<@NotNull BlockBreak> queuedBreaks = new LinkedBlockingQueue<>();
    public final @NotNull PlayerBlockHistory blockHistory = new PlayerBlockHistory();
    public final @NotNull ArrayDeque<@NotNull RotationData> pendingRotations = new ArrayDeque<>();
    public final @NotNull CompensatedCameraEntity cameraEntity;
    @Getter @Setter private ResyncHandler resyncHandler = DenemeAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("disable-default-resync-handler", false) ? NoOpResyncHandler.INSTANCE : new DefaultResyncHandler(this);
    @Getter private final @NotNull FeatureManagerImpl featureManager = new FeatureManagerImpl(this);
    public boolean serverOpenedInventoryThisTick;

    public boolean intersectedWithNetherPortal;

    private boolean debugPacketCancel;
    private int spamThreshold = 100;
    private int maxTransactionTime = 60;
    @Getter private boolean ignoreDuplicatePacketRotation;
    @Getter @Setter private boolean experimentalChecks;
    @Getter private boolean cancelDuplicatePacket = true;
    @Getter @Setter private boolean exemptElytra;
    @Getter @Setter private boolean forceStuckSpeed = true;
    @Getter @Setter private boolean forceSlowMovement = true;
    @Getter private boolean resetItemUsageOnAttack;
    @Getter private boolean resetItemUsageOnItemUpdate;
    @Getter private boolean resetItemUsageOnSlotChange;
    @Getter private boolean resetItemUsageOnItemUse;

    public boolean noModifyPacketPermission;
    public boolean noSetbackPermission;

    public boolean disableDeneme;
    public final @NotNull ArrayDeque<@NotNull Movement> movementThisTick = new ArrayDeque<>(8);
    public final @NotNull List<@NotNull Movement> finalMovementsThisTick = new ObjectArrayList<>();
    public final @NotNull LongSet visitedBlocks = new LongOpenHashSet();
    private @MonotonicNonNull UserConnection viaUserConnection;
    public boolean wasLastPredictionCompleteChecked;
    public boolean isJumping;
    public boolean lastJumping;
    public final @NotNull EntityFluidInteraction fluidInteraction = new EntityFluidInteraction(this, FluidTag.WATER, FluidTag.LAVA);
    public boolean canFloatWhileRidden;

    public DenemePlayer(@NotNull User user) {
        this.user = Objects.requireNonNull(user, "user");
        this.uuid = Objects.requireNonNull(user.getUUID(), "uuid");
        this.fireworks = new CompensatedFireworks(this);
        this.inventory = new CompensatedInventory(this);

        this.compensatedWorld = new CompensatedWorld(this);
        this.compensatedGeysers = new CompensatedGeysers();
        this.compensatedEntities = new CompensatedEntities(this);
        this.dashableEntities = new CompensatedDashableEntities();
        this.cameraEntity = new CompensatedCameraEntity(this);

        this.lastInstanceManager = new LastInstanceManager(this);
        this.attackCooldown = new AttackCooldownHandler(this);
        this.checkManager = new CheckManager(this);
        this.punishmentManager = new PunishmentManager(this);
        this.tagManager = new SyncedTags(this);
        this.movementCheckRunner = new MovementCheckRunner(this);

        this.uncertaintyHandler = new UncertaintyHandler(this);
        this.pointThreeEstimator = new PointThreeEstimator(this);

        if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            final float scale = (float) compensatedEntities.self.getAttributeValue(Attributes.SCALE);
            possibleEyeHeights[2] = new double[]{0.4 * scale, 1.62 * scale, 1.27 * scale};
            possibleEyeHeights[1] = new double[]{1.27 * scale, 1.62 * scale, 0.4 * scale};
            possibleEyeHeights[0] = new double[]{1.62 * scale, 1.27 * scale, 0.4 * scale};
        } else if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            possibleEyeHeights[2] = new double[]{0.4, 1.62, 1.54};
            possibleEyeHeights[1] = new double[]{1.54, 1.62, 0.4};
            possibleEyeHeights[0] = new double[]{1.62, 1.54, 0.4};
        } else {
            possibleEyeHeights[1] = new double[]{(double) (1.62f - 0.08f), (double) (1.62f)};
            possibleEyeHeights[0] = new double[]{(double) (1.62f), (double) (1.62f - 0.08f)};
        }

        reload();
    }

    public void onPacketCancel() {
        if (spamThreshold != -1 && cancelledPackets.incrementAndGet() > spamThreshold) {
            LogUtil.info("Disconnecting " + getName() + " for spamming invalid packets, packets cancelled within a second " + cancelledPackets);
            disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(this, DenemeAPI.INSTANCE.getConfigManager().getDisconnectClosed())));
            cancelledPackets.set(0);

            if (debugPacketCancel) {
                try {
                    throw new Exception();
                } catch (Exception e) {
                    LogUtil.error("Stacktrace for onPacketCancel (debug-packet-cancel=true)", e);
                }
            }
        }
    }

    public Set<VectorData> getPossibleVelocities() {
        Set<VectorData> set = new HashSet<>();

        if (firstBreadKB != null) {
            set.add(new VectorData(firstBreadKB.vector.clone(), VectorData.VectorType.Knockback).returnNewModified(VectorData.VectorType.FirstBreadKnockback));
        }

        if (likelyKB != null) {

            set.add(new VectorData(likelyKB.vector.clone(), VectorData.VectorType.Knockback));
        }

        set.addAll(getPossibleVelocitiesMinusKnockback());
        return set;
    }

    public Set<VectorData> getPossibleVelocitiesMinusKnockback() {
        Set<VectorData> possibleMovements = new HashSet<>();
        possibleMovements.add(new VectorData(clientVelocity, VectorData.VectorType.Normal));

        if (canSwimHop && !onGround) {
            Vector3dm vector = clientVelocity.clone().setY(0.30000001192092896 + (canFloatWhileRidden ? 0.03999999910593033 : 0.0));
            if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2)) {
                Collisions.resolveBlockEffects(this, vector, true, finalMovementsThisTick);
            }

            if (getClientVersion().isNewerThanOrEquals(ClientVersion.V_26_2)) {
                PotentSulfurGeyser.launchEntityTicker(this, vector, false);
            }

            possibleMovements.add(new VectorData(vector, VectorData.VectorType.Swimhop));
        }

        if (riptideSpinAttackTicks >= 0 && Collections.max(uncertaintyHandler.riptideEntities) > 0) {
            possibleMovements.add(new VectorData(clientVelocity.clone().multiply(-0.2), VectorData.VectorType.Trident));
        }

        if (lastWasClimbing != 0) {
            possibleMovements.add(new VectorData(clientVelocity.clone().setY(lastWasClimbing + baseTickAddition.getY()), VectorData.VectorType.Climbable));
        }

        for (VectorData data : new HashSet<>(possibleMovements)) {
            for (BlockFace direction : uncertaintyHandler.slimePistonBounces) {
                if (direction.getModX() != 0) {
                    possibleMovements.add(data.returnNewModified(data.vector.clone().setX(direction.getModX()), VectorData.VectorType.SlimePistonBounce));
                } else if (direction.getModY() != 0) {
                    possibleMovements.add(data.returnNewModified(data.vector.clone().setY(direction.getModY()), VectorData.VectorType.SlimePistonBounce));
                } else if (direction.getModZ() != 0) {
                    possibleMovements.add(data.returnNewModified(data.vector.clone().setZ(direction.getModZ()), VectorData.VectorType.SlimePistonBounce));
                }
            }
        }

        return possibleMovements;
    }

    public boolean addTransactionResponse(short id) {
        ShortToLongPair data = null;
        boolean hasID = false;
        int skipped = 0;
        for (ShortToLongPair iterator : transactionsSent) {
            if (iterator.first() == id) {
                hasID = true;
                break;
            }
            skipped++;
        }

        if (hasID) {

            if (viaPacketTracker != null) viaPacketTracker.setIntervalPackets(viaPacketTracker.getIntervalPackets() - 1);

            if (skipped > 0 && System.currentTimeMillis() - joinTime > 5000)
                checkManager.get(TransactionOrder.class).flag("skipped=" + skipped);

            do {
                data = transactionsSent.poll();
                if (data == null)
                    break;

                lastTransactionReceived.incrementAndGet();
                lastTransReceived = System.currentTimeMillis();
                transactionPing = (System.nanoTime() - data.second());
                playerClockAtLeast = data.second();
            } while (data.first() != id);

            CheckManagerListener.handleQueuedPlaces(this, false, 0, 0, System.currentTimeMillis());
            CheckManagerListener.handleQueuedBreaks(this, false, 0, 0, System.currentTimeMillis());
            latencyUtils.handleNettySyncTransaction(lastTransactionReceived.get());
        }

        return data != null;
    }

    public void baseTickAddWaterPushing(Vector3dm vector) {
        baseTickWaterPushing.add(vector);
    }

    public void baseTickAddVector(Vector3dm vector) {
        clientVelocity.add(vector);
    }

    public void trackBaseTickAddition(Vector3dm vector) {
        baseTickAddition.add(vector);
    }

    public float getMaxUpStep() {
        final PacketEntitySelf self = compensatedEntities.self;
        final PacketEntity riding = self.getRiding();
        if (riding == null) return (float) self.getAttributeValue(Attributes.STEP_HEIGHT);

        if (riding.isBoat) {
            return 0f;
        }

        float value = (float) riding.getAttributeValue(Attributes.STEP_HEIGHT);
        if (riding.isHappyGhast) {
            return ((PacketEntityHappyGhast) riding).isControllingPassenger() ? Math.max(value, 1.0F) : value;
        }

        return value;
    }

    public void sendTransaction() {
        sendTransaction(false);
    }

    public void sendTransaction(boolean async) {

        if (user.getEncoderState() != ConnectionState.PLAY) return;

        if (disableDeneme && (System.nanoTime() - getPlayerClockAtLeast()) > 15e9) {
            return;
        }

        lastTransSent = System.currentTimeMillis();
        short transactionID = (short) (-1 * (transactionIDCounter.getAndIncrement() & 0x7FFF));
        try {

            PacketWrapper<?> packet;
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                packet = new WrapperPlayServerPing(transactionID);
            } else {
                packet = new WrapperPlayServerWindowConfirmation((byte) 0, transactionID, false);
            }

            if (async) {
                runSafely(() -> {
                    addTransactionSend(transactionID);
                    user.writePacket(packet);
                });
            } else {
                addTransactionSend(transactionID);
                user.writePacket(packet);
            }
        } catch (Exception ignored) {

        }
    }

    public void addTransactionSend(short id) {
        didWeSendThatTrans.add(id);
    }

    public double getEyeHeight() {
        return getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ?
                compensatedEntities.self.getAttributeValue(Attributes.SCALE) * pose.eyeHeight
                : isSneaking ? 1.54f : 1.62f;
    }

    private final AtomicBoolean hasDisconnected = new AtomicBoolean(false);

    public void timedOut() {
        disconnect(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(this, DenemeAPI.INSTANCE.getConfigManager().getDisconnectTimeout())));
    }

    public void disconnect(Component reason) {
        if (!hasDisconnected.compareAndSet(false, true)) {
            return;
        }

        String textReason;
        if (reason instanceof TranslatableComponent translatableComponent) {
            textReason = translatableComponent.key();
        } else {
            textReason = LegacyComponentSerializer.legacySection().serialize(reason);
        }
        LogUtil.info("Disconnecting " + user.getProfile().getName() + " for " + MessageUtil.stripColor(textReason));
        try {
            user.sendPacket(new WrapperPlayServerDisconnect(reason));
        } catch (Exception ignored) {
            LogUtil.warn("Failed to send disconnect packet to disconnect " + user.getProfile().getName() + "! Disconnecting anyways.");
        }
        user.closeConnection();
        if (platformPlayer != null) {
            DenemeAPI.INSTANCE.getScheduler().getEntityScheduler().execute(platformPlayer, DenemeAPI.INSTANCE.getDenemePlugin(),
                    () -> platformPlayer.kickPlayer(textReason), null, 1);
        }
    }

    public void pollData() {

        if (lastTransSent != 0 && lastTransSent + 80 < System.currentTimeMillis()) {
            sendTransaction(true);
        }
        if ((System.nanoTime() - getPlayerClockAtLeast()) > maxTransactionTime * 1e9) {
            timedOut();
        }

        if (!DenemeAPI.INSTANCE.getPlayerDataManager().shouldCheck(user)) {
            DenemeAPI.INSTANCE.getPlayerDataManager().remove(user);
        }

        if (viaPacketTracker == null && ViaVersionUtil.isAvailable) {
            final UserConnection connection = getViaUserConnection();
            viaPacketTracker = connection != null ? connection.getPacketTracker() : null;
        }

        if (this.platformPlayer == null) {
            this.platformPlayer = DenemeAPI.INSTANCE.getPlatformPlayerFactory().getFromUUID(uuid);
            updatePermissions();
        }

        DenemeAPI.INSTANCE.getDataStoreLifecycle().sessionTracker()
                .pollHeartbeat(uuid, System.currentTimeMillis());
    }

    public void updateVelocityMovementSkipping() {
        if (!couldSkipTick) {
            couldSkipTick = pointThreeEstimator.determineCanSkipTick(BlockProperties.getFrictionInfluencedSpeed((float) (speed * (isSprinting ? 1.3 : 1)), this), getPossibleVelocitiesMinusKnockback());
        }

        Set<VectorData> knockback = new HashSet<>();
        if (firstBreadKB != null) knockback.add(new VectorData(firstBreadKB.vector, VectorData.VectorType.Knockback));
        if (likelyKB != null) knockback.add(new VectorData(likelyKB.vector, VectorData.VectorType.Knockback));

        boolean kbPointThree = pointThreeEstimator.determineCanSkipTick(BlockProperties.getFrictionInfluencedSpeed((float) (speed * (isSprinting ? 1.3 : 1)), this), knockback);
        checkManager.getKnockbackHandler().setPointThree(kbPointThree);

        Set<VectorData> explosion = new HashSet<>();
        if (firstBreadExplosion != null)
            explosion.add(new VectorData(firstBreadExplosion.vector, VectorData.VectorType.Explosion));
        if (likelyExplosions != null)
            explosion.add(new VectorData(likelyExplosions.vector, VectorData.VectorType.Explosion));

        boolean explosionPointThree = pointThreeEstimator.determineCanSkipTick(BlockProperties.getFrictionInfluencedSpeed((float) (speed * (isSprinting ? 1.3 : 1)), this), explosion);
        checkManager.getExplosionHandler().setPointThree(explosionPointThree);

        if (kbPointThree || explosionPointThree) {
            uncertaintyHandler.lastPointThree.reset();
        }
    }

    @Override
    public void updatePermissions() {
        runSafely(() -> {
            try {
                boolean noModifyPacketPermission = hasPermission("deneme.nomodifypacket");
                boolean noSetbackPermission = hasPermission("deneme.nosetback");
                boolean disabledPermission = hasPermission("deneme.disabled");
                boolean exemptPermission = hasPermission("deneme.exempt");
                for (AbstractCheck check : getChecks()) {
                    if (check instanceof Check c) {
                        c.updatePermissions();
                    }
                }

                this.noModifyPacketPermission = noModifyPacketPermission;
                this.noSetbackPermission = noSetbackPermission;
                this.disableDeneme = disabledPermission;
                if (exemptPermission) {
                    DenemeAPI.INSTANCE.getPlayerDataManager().exemptUser(user);
                }
            } catch (Exception e) {
                LogUtil.error("Failed to update permissions for " + getName() + "!", e);
            }
        });
    }

    public boolean isPointThree() {
        return getClientVersion().isOlderThan(ClientVersion.V_1_18_2);
    }

    public double getMovementThreshold() {
        return isPointThree() ? 0.03 : 0.0002;
    }

    public ClientVersion getClientVersion() {

        return Objects.requireNonNullElseGet(user.getClientVersion(), () -> ClientVersion.getById(PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion()));
    }

    public boolean isTickingReliablyFor(int ticks) {
        if (!cameraEntity.isSelf()) return false;

        return !canSkipTicks() || (inVehicle()
                || !uncertaintyHandler.lastPointThree.hasOccurredSince(ticks))
                && !uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(1);
    }

    public boolean inVehicle() {
        return compensatedEntities.self.inVehicle();
    }

    public PacketEntity getVehicle() {
        return compensatedEntities.self.riding;
    }

    public EntityType getVehicleType() {
        return inVehicle() ? getVehicle().getType() : null;
    }

    public double[] getPossibleEyeHeights() {

        if (this.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
            return this.possibleEyeHeights[this.isSneaking ? 1 : 0];
        } else {

            return switch (pose) {
                case FALL_FLYING,
                     SPIN_ATTACK,
                     SWIMMING ->
                        this.possibleEyeHeights[2];
                case NINE_CROUCHING, CROUCHING -> this.possibleEyeHeights[1];
                default -> this.possibleEyeHeights[0];
            };
        }
    }

    @Override
    public PacketWorld getPacketWorld() {
        return compensatedWorld;
    }

    @Override
    public int getTransactionPing() {
        return DenemeMath.floor(transactionPing / 1e6);
    }

    @Override
    public int getKeepAlivePing() {
        if (platformPlayer == null) return -1;
        return PacketEvents.getAPI().getPlayerManager().getPing(platformPlayer.getNative());
    }

    public long getPlayerClockAtLeast() {
        if (lastTransactionSent.get() == 0) {
            playerClockAtLeast = System.nanoTime();
        }
        return playerClockAtLeast;
    }

    public SetbackTeleportUtil getSetbackTeleportUtil() {
        return checkManager.getSetbackUtil();
    }

    public boolean wouldCollisionResultFlagGroundSpoof(double inputY, double collisionY) {
        boolean verticalCollision = inputY != collisionY;
        boolean calculatedOnGround = verticalCollision && inputY < 0.0D;

        if (exemptOnGround()) return false;

        if (inputY == -SimpleCollisionBox.COLLISION_EPSILON && collisionY > -SimpleCollisionBox.COLLISION_EPSILON && collisionY <= 0)
            return false;

        return calculatedOnGround != onGround;
    }

    public boolean exemptOnGround() {
        return inVehicle()
                || Collections.max(uncertaintyHandler.pistonX) != 0 || Collections.max(uncertaintyHandler.pistonY) != 0
                || Collections.max(uncertaintyHandler.pistonZ) != 0 || uncertaintyHandler.isStepMovement
                || isFlying || compensatedEntities.self.isDead || isInBed || lastInBed || uncertaintyHandler.lastFlyingStatusChange.hasOccurredSince(30)
                || uncertaintyHandler.lastHardCollidingLerpingEntity.hasOccurredSince(3) || uncertaintyHandler.isOrWasNearGlitchyBlock;
    }

    public void handleMountVehicle(int vehicleID) {
        compensatedEntities.serverPlayerVehicle = vehicleID;
        TrackerData data = compensatedEntities.getTrackedEntity(vehicleID);

        if (data != null) {

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) && getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {

                if (EntityTypes.isTypeInstanceOf(data.getEntityType(), EntityTypes.BOAT) ||
                        EntityTypes.isTypeInstanceOf(data.getEntityType(), EntityTypes.ABSTRACT_HORSE) ||
                        data.getEntityType() == EntityTypes.PIG ||
                        data.getEntityType() == EntityTypes.STRIDER ||
                        EntityTypes.isTypeInstanceOf(data.getEntityType(), EntityTypes.CAMEL) ||
                        data.getEntityType() == EntityTypes.HAPPY_GHAST ||
                        EntityTypes.isTypeInstanceOf(data.getEntityType(), EntityTypes.ABSTRACT_NAUTILUS)) {

                    user.writePacket(new WrapperPlayServerEntityVelocity(vehicleID, new Vector3d()));
                }
            }
        }

        sendTransaction();

        latencyUtils.addRealTimeTask(lastTransactionSent.get(), () -> this.vehicleData.wasVehicleSwitch = true);
    }

    public int getRidingVehicleId() {
        return compensatedEntities.getPacketEntityID(compensatedEntities.self.getRiding());
    }

    public void handleDismountVehicle(PacketSendEvent event) {
        EntityType entityType = getVehicleType();

        sendTransaction();

        compensatedEntities.serverPlayerVehicle = null;
        event.getTasksAfterSend().add(() -> {
            if (inVehicle()) {
                int ridingId = getRidingVehicleId();
                TrackerData data = compensatedEntities.serverPositionsMap.get(ridingId);
                if (data != null) {
                    final Vector3d pos = new Vector3d(data.getX(), data.getY(), data.getZ());

                    if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2)) {

                        user.writePacket(new WrapperPlayServerEntityPositionSync(ridingId, new EntityPositionData(pos, new Vector3d(), data.getXRot(), data.getYRot()), false));
                    } else {
                        user.writePacket(new WrapperPlayServerEntityTeleport(ridingId, pos, data.getXRot(), data.getYRot(), false));
                    }
                }
            }
        });

        latencyUtils.addRealTimeTask(lastTransactionSent.get(), () -> {
            this.vehicleData.wasVehicleSwitch = true;

            if (getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_14)) {
                compensatedEntities.hasSprintingAttributeEnabled = false;
            }
        });
    }

    public boolean canGlide() {

        final ItemStack chestPlate = inventory.getChestplate();
        if (chestPlate.getType() == ItemTypes.ELYTRA && chestPlate.getDamageValue() < chestPlate.getMaxDamage() - 1)
            return true;

        if (getClientVersion().isOlderThan(ClientVersion.V_1_21_2)
                || PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_21_2)) return false;

        return isGlider(inventory.getHelmet(), EquipmentSlot.HELMET)
                || isGlider(inventory.getChestplate(), EquipmentSlot.CHEST_PLATE)
                || isGlider(inventory.getLeggings(), EquipmentSlot.LEGGINGS)
                || isGlider(inventory.getBoots(), EquipmentSlot.BOOTS)
                || isGlider(inventory.getOffHand(), EquipmentSlot.OFF_HAND);
    }

    private static boolean isGlider(ItemStack stack, EquipmentSlot slot) {
        if (!stack.hasComponent(ComponentTypes.GLIDER) || (stack.canBeDepleted() && stack.getDamageValue() >= (stack.getMaxDamage() - 1))) {
            return false;
        }

        Optional<ItemEquippable> equippable = stack.getComponent(ComponentTypes.EQUIPPABLE);
        return equippable.isPresent() && equippable.get().getSlot() == slot;
    }

    public void resyncGlidingState() {
        if (platformPlayer != null) platformPlayer.resyncSharedFlags();
    }

    public boolean canPlaceGameMasterBlocks() {

        return getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_10) || canUseGameMasterBlocks();
    }

    public boolean canUseGameMasterBlocks() {
        return (gamemode == GameMode.CREATIVE && compensatedEntities.self.opLevel >= 2);
    }

    public boolean isInWaterOrRain() {
        return compensatedWorld.isRaining || Collisions.hasMaterial(this, boundingBox.copy().expand(0.1f), (block, x, y, z) -> Materials.isWater(CompensatedWorld.blockVersion, block));
    }

    @Contract(pure = true)
    public boolean supportsEndTickPreVia() {
        return getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2);
    }

    @Contract(pure = true)
    public boolean supportsEndTick() {
        return supportsEndTickPreVia() && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2);
    }

    @Contract(pure = true)
    public boolean canSkipTicksPreVia() {
        return getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && !supportsEndTickPreVia();
    }

    @Contract(pure = true)
    public boolean canSkipTicks() {
        return getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && !supportsEndTick();
    }

    @Override
    public void runSafely(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        ChannelHelper.runInEventLoop(this.user.getChannel(), runnable);
    }

    @Override
    public int getLastTransactionReceived() {
        return lastTransactionReceived.get();
    }

    @Override
    public int getLastTransactionSent() {
        return lastTransactionSent.get();
    }

    @Override
    public void addRealTimeTask(int transaction, Runnable runnable) {
        latencyUtils.addRealTimeTask(transaction, runnable);
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public UUID getUniqueId() {
        return user.getProfile().getUUID();
    }

    @Override
    public String getBrand() {
        return checkManager.get(ClientBrand.class).getBrand();
    }

    @Override
    public @Nullable String getWorldName() {
        return platformPlayer != null ? platformPlayer.getWorld().getName() : null;
    }

    @Override
    public @Nullable UUID getWorldUID() {
        return platformPlayer != null ? platformPlayer.getWorld().getUID() : null;
    }

    @Override
    public String getVersionName() {
        return getClientVersion().getReleaseName();
    }

    @Override
    public double getHorizontalSensitivity() {
        return checkManager.get(AimProcessor.class).sensitivityYaw;
    }

    @Override
    public double getVerticalSensitivity() {
        return checkManager.get(AimProcessor.class).sensitivityPitch;
    }

    @Override
    public boolean isVanillaMath() {
        return trigHandler.isVanillaMath();
    }

    @Override
    public Collection<? extends AbstractCheck> getChecks() {
        return checkManager.checks;
    }

    public void runNettyTaskInMs(@NotNull Runnable runnable, int ms) {
        Objects.requireNonNull(runnable, "runnable");
        ((Channel) user.getChannel()).eventLoop().schedule(runnable, ms, TimeUnit.MILLISECONDS);
    }

    @Override
    public final void reload(ConfigManager config) {
        updatePermissions();
        featureManager.onReload(config);
        debugPacketCancel = config.getBooleanElse("debug-packet-cancel", false);
        spamThreshold = config.getIntElse("packet-spam-threshold", 100);
        maxTransactionTime = DenemeMath.clamp(config.getIntElse("max-transaction-time", 60), 1, 180);
        ignoreDuplicatePacketRotation = config.getBooleanElse("ignore-duplicate-packet-rotation", false);
        cancelDuplicatePacket = config.getBooleanElse("cancel-duplicate-packet", true);

        boolean shouldDisableResync = config.getBooleanElse("disable-default-resync-handler", false);
        Class<?> currentHandlerClass = this.resyncHandler.getClass();

        boolean isInternalHandler = currentHandlerClass == DefaultResyncHandler.class
                || currentHandlerClass == NoOpResyncHandler.class;

        if (isInternalHandler) {
            if (shouldDisableResync) {

                if (currentHandlerClass != NoOpResyncHandler.class) {
                    this.resyncHandler = NoOpResyncHandler.INSTANCE;
                }
            } else {

                if (currentHandlerClass != DefaultResyncHandler.class) {
                    this.resyncHandler = new DefaultResyncHandler(this);
                }
            }
        }

        resetItemUsageOnAttack = config.getBooleanElse("reset-item-usage-on-attack", true);
        resetItemUsageOnItemUpdate = config.getBooleanElse("reset-item-usage-on-item-update", true);
        resetItemUsageOnSlotChange = config.getBooleanElse("reset-item-usage-on-slot-change", true);
        resetItemUsageOnItemUse = config.getBooleanElse("reset-item-usage-on-item-use", true);

        checkManager.reload();

        punishmentManager.reload(config);
        this.movementCheckRunner.reload(config);
    }

    @Override
    public void reload() {
        reload(DenemeAPI.INSTANCE.getConfigManager().getConfig());
    }

    @Override
    public void sendMessage(String message) {
        if (platformPlayer != null) platformPlayer.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String s) {
        return platformPlayer != null && platformPlayer.hasPermission(s);
    }

    public boolean hasPermission(String s, boolean defaultIfUnset) {
        return platformPlayer != null && platformPlayer.hasPermission(s, defaultIfUnset);
    }

    public void sendMessage(Component message) {
        if (platformPlayer != null) platformPlayer.sendMessage(message);
    }

    public void resyncPosition(Vector3i pos) {
        this.resyncHandler.resync(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public void resyncPosition(Vector3i pos, int sequenceID) {
        this.resyncHandler.resyncPosition(pos.x, pos.y, pos.z, sequenceID);
    }

    public void resyncPositions(SimpleCollisionBox box) {
        this.resyncHandler.resync(DenemeMath.floor(box.minX), DenemeMath.floor(box.minY), DenemeMath.floor(box.minZ),
                DenemeMath.ceil(box.maxX), DenemeMath.ceil(box.maxY), DenemeMath.ceil(box.maxZ));
    }

    public void addMovementThisTick(@NotNull DenemePlayer.Movement movement) {
        Objects.requireNonNull(movement, "movement");

        if (this.movementThisTick.size() >= 100) {
            DenemePlayer.Movement movement1 = this.movementThisTick.removeFirst();
            DenemePlayer.Movement movement2 = this.movementThisTick.removeFirst();
            DenemePlayer.Movement movement3 = new DenemePlayer.Movement(movement1.from(), movement2.to());
            this.movementThisTick.addFirst(movement3);
        }

        this.movementThisTick.add(movement);
    }

    public void resetStuckSpeedMultiplier() {
        this.lastStuckSpeedMultiplier = this.stuckSpeedMultiplier;
        this.stuckSpeedMultiplier = StuckSpeed.NONE;
    }

    public record Movement(Vector3d from, Vector3d to, Vector3d axisDependentOriginalMovement) {

        public Movement(Vector3d from, Vector3d to) {
            this(from, to, null);
        }

        public boolean axisIndependant() {
            return axisDependentOriginalMovement != null;
        }

    }

    public Location getLocation() {
        return new Location(platformPlayer.getWorld(), this.x, this.y, this.z, this.yaw, this.pitch);
    }

    public @Nullable UserConnection getViaUserConnection() {
        if (this.viaUserConnection == null && ViaVersionUtil.isAvailable) {
            this.viaUserConnection = Via.getManager().getConnectionManager().getConnectedClient(uuid);
        }
        return this.viaUserConnection;
    }

    public int getViaTranslatedClientBlockID(int blockStateId) {
        final UserConnection connection = getViaUserConnection();
        if (connection == null) {
            return blockStateId;
        }

        final ProtocolVersion clientVersion = connection.getProtocolInfo().protocolVersion();
        final ProtocolVersion serverVersion = connection.getProtocolInfo().serverProtocolVersion();

        final List<ProtocolPathEntry> protocolPath = Via.getManager().getProtocolManager().getProtocolPath(clientVersion, serverVersion);
        if (protocolPath == null) {
            return blockStateId;
        }

        for (int i = protocolPath.size() - 1; i >= 0; i--) {
            final Protocol<?, ?, ?, ?> protocol = protocolPath.get(i).protocol();
            if (protocol.getMappingData() != null && protocol.getMappingData().getBlockStateMappings() != null) {
                blockStateId = protocol.getMappingData().getNewBlockStateId(blockStateId);
            }
        }

        return blockStateId;
    }

    public double getFluidHeight(FluidTag fluidTag) {
        if (getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_21_11)) return this.fluidHeight.getDouble(fluidTag);
        return this.fluidInteraction.getFluidHeight(fluidTag);
    }

    public boolean isEyeInFluid(FluidTag fluidTag) {
        if (getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_21_11)) return this.fluidOnEyes == fluidTag;
        return this.fluidInteraction.isEyeInFluid(fluidTag);
    }

    public boolean isPushedByFluid() {
        if (!this.inVehicle()) return !this.isFlying;
        PacketEntity vehicle = getVehicle();
        return !(vehicle instanceof PacketEntityNautilus);
    }

}
