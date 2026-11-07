package ac.deneme.anticheat.checks.impl.velocity;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.combat.CombatEvidence;
import ac.deneme.anticheat.checks.type.PacketSendListener;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;
import ac.deneme.anticheat.utils.reflection.GeyserUtil;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "ExplosionRatio", stableKey = "deneme.velocity.explosion_ratio", description = "Moved too little for the explosion it took", decay = 0.02)
public class ExplosionRatio extends Check implements PacketSendListener, PostPredictionListener {

    private static final double MINIMUM_EXPECTED = 0.12;
    private static final double MINIMUM_RATIO = 0.5;
    private static final int TRACKED_TICKS = 3;
    private static final int FLAG_STREAK = 2;

    private double expectedHorizontal;
    private int remainingTicks;
    private int streak;

    public ExplosionRatio(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.EXPLOSION) {
            return;
        }

        Vector3f motion = new WrapperPlayServerExplosion(event).getPlayerMotion();

        if (motion == null) {
            return;
        }

        double horizontal = Math.sqrt(motion.getX() * motion.getX() + motion.getZ() * motion.getZ());

        if (horizontal < MINIMUM_EXPECTED) {
            expectedHorizontal = 0.0;
            remainingTicks = 0;
            return;
        }

        expectedHorizontal = horizontal;
        remainingTicks = TRACKED_TICKS;
    }

    @Override
    public void onPredictionComplete(@NotNull PredictionComplete predictionComplete) {
        if (expectedHorizontal <= 0.0 || remainingTicks <= 0) {
            return;
        }

        if (!predictionComplete.isChecked() || predictionComplete.getData() == null || predictionComplete.getData().isTeleport()) {
            expectedHorizontal = 0.0;
            remainingTicks = 0;
            return;
        }

        double actual = Math.sqrt(
                player.actualMovement.getX() * player.actualMovement.getX()
                        + player.actualMovement.getZ() * player.actualMovement.getZ());

        remainingTicks--;

        if (player.compensatedEntities.self.getRiding() != null || player.disableDeneme
                || GeyserUtil.isBedrockPlayer(player.user.getUUID())) {
            expectedHorizontal = 0.0;
            remainingTicks = 0;
            return;
        }

        double ratio = actual / expectedHorizontal;

        if (ratio < MINIMUM_RATIO) {
            streak++;
            CombatEvidence.report(player, "explosion_ratio", 1.0);

            if (streak >= FLAG_STREAK) {
                flag(String.format("expected=%.3f actual=%.3f ratio=%.2f", expectedHorizontal, actual, ratio));
                streak = 0;
            }
        } else {
            streak = 0;
            reward();
        }

        if (remainingTicks <= 0) {
            expectedHorizontal = 0.0;
        }
    }
}
