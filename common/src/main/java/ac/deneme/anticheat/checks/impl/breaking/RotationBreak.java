package ac.deneme.anticheat.checks.impl.breaking;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.verbose.VerboseCodecs;
import ac.deneme.anticheat.checks.type.BlockBreakListener;
import ac.deneme.anticheat.checks.type.PostFlyingBlockBreakListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockBreak;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.Pair;
import ac.deneme.anticheat.utils.nmsutil.Ray;
import ac.deneme.anticheat.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CheckData(name = "RotationBreak", stableKey = "deneme.breaking.rotation_break", description = "Tried to break a block without looking at it", experimental = true)
public class RotationBreak extends Check implements BlockBreakListener, PostFlyingBlockBreakListener {
    private static final Verbose V = Verbose.of("[pre-flying|post-flying], action={digging}");

    private double flagBuffer = 0;
    private boolean ignorePost = false;

    public RotationBreak(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (!player.cameraEntity.isSelf())
            return;
        if (player.inVehicle()) return;
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) return;

        if (flagBuffer > 0 && !didRayTraceHit(blockBreak)) {
            ignorePost = true;

            if (flag(V.write(verbose()).bool(true)
                    .uint(VerboseCodecs.enumId(blockBreak.action))) && shouldModifyPackets()) {
                blockBreak.cancel();
            }
        }
    }

    @Override
    public void onPostFlyingBlockBreak(BlockBreak blockBreak) {
        if (!player.cameraEntity.isSelf())
            return;
        if (player.inVehicle()) return;
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) return;

        if (didRayTraceHit(blockBreak)) {
            flagBuffer = Math.max(0, flagBuffer - 0.1);
        } else {
            flagBuffer = 1;
            if (!ignorePost) {
                flag(V.write(verbose()).bool(false)
                        .uint(VerboseCodecs.enumId(blockBreak.action)));
            }
        }

        if (ignorePost) {
            ignorePost = false;
        }
    }

    private boolean didRayTraceHit(BlockBreak blockBreak) {
        SimpleCollisionBox box = new SimpleCollisionBox(blockBreak.position);

        final double[] possibleEyeHeights = player.getPossibleEyeHeights();

        double minEyeHeight = Double.MAX_VALUE;
        double maxEyeHeight = Double.MIN_VALUE;
        for (double height : possibleEyeHeights) {
            minEyeHeight = Math.min(minEyeHeight, height);
            maxEyeHeight = Math.max(maxEyeHeight, height);
        }

        SimpleCollisionBox eyePositions = new SimpleCollisionBox(player.x, player.y + minEyeHeight, player.z, player.x, player.y + maxEyeHeight, player.z);
        eyePositions.expand(player.getMovementThreshold());

        if (eyePositions.isIntersected(box)) {
            return true;
        }

        List<Vector3f> possibleLookDirs = new ArrayList<>(Arrays.asList(
                new Vector3f(player.lastYaw, player.pitch, 0),
                new Vector3f(player.yaw, player.pitch, 0)
        ));

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            possibleLookDirs.add(new Vector3f(player.lastYaw, player.lastPitch, 0));
        }

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
            possibleLookDirs = Collections.singletonList(new Vector3f(player.yaw, player.pitch, 0));
        }

        final double distance = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        for (double d : possibleEyeHeights) {
            for (Vector3f lookDir : possibleLookDirs) {
                Vector3d starting = new Vector3d(player.x, player.y + d, player.z);
                Ray trace = new Ray(player, starting.getX(), starting.getY(), starting.getZ(), lookDir.getX(), lookDir.getY());
                Pair<Vector3d, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.origin(), trace.getPointAtDistance(distance));

                if (intercept.first() != null) return true;
            }
        }

        return false;
    }
}
