package ac.deneme.anticheat.predictionengine;

import ac.deneme.anticheat.checks.DenemeProcessor;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;
import ac.deneme.anticheat.utils.collisions.datatypes.SimpleCollisionBox;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public class GhostBlockDetector extends DenemeProcessor implements PostPredictionListener {

    public GhostBlockDetector(DenemePlayer player) {
        super(player);
    }

    public static boolean isGhostBlock(DenemePlayer player) {

        if (player.uncertaintyHandler.isOrWasNearGlitchyBlock) {
            return true;
        }

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
            SimpleCollisionBox largeExpandedBB = player.boundingBox.copy().expand(12, 0.5, 12);

            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                if (entity.isBoat) {
                    if (entity.getPossibleCollisionBoxes().isIntersected(largeExpandedBB)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {

        if (predictionComplete.getOffset() < 0.001 && (player.clientClaimsLastOnGround == player.onGround || player.inVehicle()))
            return;

        boolean shouldResync = isGhostBlock(player);

        if (shouldResync) {

            if (player.clientClaimsLastOnGround != player.onGround) {

                player.onGround = player.clientClaimsLastOnGround;
            }

            predictionComplete.setOffset(0);
            player.getSetbackTeleportUtil().executeForceResync();
        }
    }
}
