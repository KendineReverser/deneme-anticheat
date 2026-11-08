package ac.deneme.anticheat.checks.impl.breaking;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.verbose.VerboseCodecs;
import ac.deneme.anticheat.checks.type.BlockBreakListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockBreak;
import ac.deneme.anticheat.utils.nmsutil.BlockBreakSpeed;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;

@CheckData(name = "WrongBreak", stableKey = "deneme.breaking.wrong_break", description = "Sent block break progress for a different block than the one being mined")
public class WrongBreak extends Check implements BlockBreakListener {
    private static final Verbose V =
            Verbose.of("action={digging}, last=[{mcpos}|null], pos={mcpos}");

    private final int exemptedY = player.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 255 : (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14) ? -1 : 4095);
    private boolean lastBlockWasInstantBreak = false;
    private Vector3i lastBlock, lastCancelledBlock, lastLastBlock = null;

    public WrongBreak(final DenemePlayer player) {
        super(player);
    }

    private boolean shouldExempt(final WrappedBlockState block, int yPos) {

        if (lastLastBlock != null || lastBlock == null)
            return false;

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) && yPos != exemptedY)
            return false;

        return player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)

                || !BlockBreakSpeed.couldInstantlyBreakBlock(player, block.getType());
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            final Vector3i pos = blockBreak.position;

            lastBlockWasInstantBreak = BlockBreakSpeed.couldInstantlyBreakBlock(player, blockBreak.block.getType());
            lastCancelledBlock = null;
            lastLastBlock = lastBlock;
            lastBlock = pos;
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            final Vector3i pos = blockBreak.position;

            if (!shouldExempt(blockBreak.block, pos.y) && !pos.equals(lastBlock)) {

                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || (!lastBlockWasInstantBreak && pos.equals(lastCancelledBlock))) {
                    var buf = V.write(verbose()).uint(VerboseCodecs.enumId(DiggingAction.CANCELLED_DIGGING))
                            .bool(lastBlock != null)
                            .mcPos(lastBlock == null ? 0 : lastBlock.x, lastBlock == null ? 0 : lastBlock.y, lastBlock == null ? 0 : lastBlock.z)
                            .mcPos(pos.x, pos.y, pos.z);
                    if (flag(buf)) {
                        if (shouldModifyPackets()) {
                            blockBreak.cancel();
                        }
                    }
                }
            }

            lastCancelledBlock = pos;
            lastLastBlock = null;
            lastBlock = null;
            return;
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING) {
            final Vector3i pos = blockBreak.position;

            if (!pos.equals(lastCancelledBlock) && (!lastBlockWasInstantBreak || player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) && !pos.equals(lastBlock)) {
                var buf = V.write(verbose()).uint(VerboseCodecs.enumId(DiggingAction.FINISHED_DIGGING))
                        .bool(lastBlock != null)
                        .mcPos(lastBlock == null ? 0 : lastBlock.x, lastBlock == null ? 0 : lastBlock.y, lastBlock == null ? 0 : lastBlock.z)
                        .mcPos(pos.x, pos.y, pos.z);
                if (flag(buf)) {
                    if (shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                }
            }

            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) {
                lastCancelledBlock = null;
                lastLastBlock = null;
                lastBlock = null;
            }
        }
    }
}
