package ac.deneme.anticheat.checks.impl.breaking;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.verbose.VerboseCodecs;
import ac.deneme.anticheat.checks.type.BlockBreakListener;
import ac.deneme.anticheat.checks.type.PreViaPacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockBreak;
import ac.deneme.anticheat.utils.math.DenemeMath;
import ac.deneme.anticheat.utils.nmsutil.BlockBreakSpeed;
import ac.deneme.anticheat.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@CheckData(name = "FastBreak", stableKey = "deneme.breaking.fast_break", description = "Breaking blocks too quickly")
public class FastBreak extends Check implements BlockBreakListener, PreViaPacketReceiveListener {
    private static final Verbose V =
            Verbose.of("[delay={ulong}ms|diff={f64:%.1f}ms, balance={f64:%.1f}ms], type={block}");

    private static final Set<StateType> EXEMPT_STATES = Set.of();
    private final boolean clientOlderThanServer = PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion() > player.getClientVersion().getProtocolVersion();

    public FastBreak(DenemePlayer player) {
        super(player);
    }

    Vector3i targetBlockPosition = null;

    double maximumBlockDamage = 0;

    long lastFinishBreak = 0;

    long startBreak = 0;

    double blockBreakBalance = 0;
    double blockDelayBalance = 0;

    private double breakThreshold;
    private double delayThreshold;

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            if (!ViaVersionUtil.isAvailable) {

                final WrappedBlockState defaultState = WrappedBlockState.getDefaultState(player.getClientVersion(), blockBreak.block.getType());
                if (defaultState.getType() == StateTypes.AIR || EXEMPT_STATES.contains(defaultState.getType())) {
                    return;
                }
            }

            WrappedBlockState block = clientOlderThanServer ? WrappedBlockState.getByGlobalId(player.getClientVersion(), player.getViaTranslatedClientBlockID(blockBreak.block.getGlobalId())) : blockBreak.block;

            startBreak = System.currentTimeMillis() - (targetBlockPosition == null ? 50 : 0);
            targetBlockPosition = blockBreak.position;

            maximumBlockDamage = BlockBreakSpeed.getBlockDamage(player, block);

            double breakDelay = System.currentTimeMillis() - lastFinishBreak;

            if (breakDelay >= 275) {
                blockDelayBalance *= 0.9;
            } else {
                blockDelayBalance += 300 - breakDelay;
            }

            if (blockDelayBalance > delayThreshold) {
                int type = VerboseCodecs.block(blockBreak.block.getType(), player.getClientVersion());
                if (flag(V.write(verbose()).bool(true).ulong((long) breakDelay).f64(0).f64(0).sint(type)) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            }

            clampBalance();
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING && targetBlockPosition != null) {
            double predictedTime = Math.ceil(1 / maximumBlockDamage) * 50;
            double realTime = System.currentTimeMillis() - startBreak;
            double diff = predictedTime - realTime;

            clampBalance();

            if (diff < 25) {
                blockBreakBalance *= 0.9;
            } else {
                blockBreakBalance += diff;
            }

            if (blockBreakBalance > breakThreshold) {
                int type = VerboseCodecs.block(blockBreak.block.getType(), player.getClientVersion());
                if (flag(V.write(verbose()).bool(false).ulong(0).f64(diff).f64(blockBreakBalance).sint(type)) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            }

            lastFinishBreak = startBreak = System.currentTimeMillis();
        }
    }

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {

        boolean flying = WrapperPlayClientPlayerFlying.isFlying(event.getPacketType());
        if ((flying || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && isAnimation(event.getPacketType())) && targetBlockPosition != null) {
            maximumBlockDamage = Math.max(maximumBlockDamage, BlockBreakSpeed.getBlockDamage(player, player.compensatedWorld.getBlock(targetBlockPosition)));
        }
    }

    private void clampBalance() {
        double breakBalance = Math.max(breakThreshold, player.getTransactionPing());
        double delayBalance = Math.max(delayThreshold, player.getTransactionPing());
        blockBreakBalance = DenemeMath.clamp(blockBreakBalance, -breakBalance, breakBalance);
        blockDelayBalance = DenemeMath.clamp(blockDelayBalance, -delayBalance, delayBalance);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        breakThreshold = config.getDoubleElse(getConfigName() + ".break-threshold", 1000);
        delayThreshold = config.getDoubleElse(getConfigName() + ".delay-threshold", 1000);
    }
}
