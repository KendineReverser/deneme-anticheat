package ac.deneme.anticheat.checks.impl.breaking;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.verbose.VerboseCodecs;
import ac.deneme.anticheat.checks.type.BlockBreakListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockBreak;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

@CheckData(name = "PositionBreakA", stableKey = "deneme.breaking.position_break_a", description = "Tried to break a block face from an impossible eye position")
public class PositionBreakA extends Check implements BlockBreakListener {
    private static final Verbose V = Verbose.of("action={digging}, face={face}");

    public PositionBreakA(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.inVehicle()
                || blockBreak.action == DiggingAction.CANCELLED_DIGGING
                || blockBreak.block.getType() == StateTypes.REDSTONE_WIRE
        ) return;

        SimpleCollisionBox combined = blockBreak.getCombinedBox();

        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        double minEyeHeight = Double.MAX_VALUE;
        double maxEyeHeight = Double.MIN_VALUE;
        for (double height : possibleEyeHeights) {
            minEyeHeight = Math.min(minEyeHeight, height);
            maxEyeHeight = Math.max(maxEyeHeight, height);
        }

        SimpleCollisionBox eyePositions = new SimpleCollisionBox(player.x, player.y + minEyeHeight, player.z, player.x, player.y + maxEyeHeight, player.z);
        if (!player.packetStateData.didLastMovementIncludePosition || player.canSkipTicks()) {
            eyePositions.expand(player.getMovementThreshold());
        }

        if (eyePositions.isIntersected(combined)) {
            return;
        }

        boolean flag = switch (blockBreak.face) {
            case NORTH -> eyePositions.minZ > combined.minZ;
            case SOUTH -> eyePositions.maxZ < combined.maxZ;
            case EAST -> eyePositions.maxX < combined.maxX;
            case WEST -> eyePositions.minX > combined.minX;
            case UP -> eyePositions.maxY < combined.maxY;
            case DOWN -> eyePositions.minY > combined.minY;
            default -> false;
        };

        if (flag && flag(V.write(verbose())
                .uint(VerboseCodecs.enumId(blockBreak.action))
                .uint(VerboseCodecs.enumId(blockBreak.face)))
                && shouldModifyPackets()) {
            blockBreak.cancel();
        }
    }
}
