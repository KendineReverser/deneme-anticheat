package ac.deneme.anticheat.checks.impl.prediction;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "GroundSpoof", stableKey = "deneme.groundspoof.fake", description = "Claimed to be on ground when predicted otherwise", setback = 10, decay = 0.01)
public class GroundSpoof extends Check implements PostPredictionListener {
    private static final Verbose V = Verbose.of("claimed {bool}");

    public GroundSpoof(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8) && player.gamemode == GameMode.SPECTATOR)
            return;

        if (player.exemptOnGround() || !predictionComplete.isChecked()) return;

        if (player.getSetbackTeleportUtil().blockOffsets) return;

        if (player.packetStateData.lastPacketWasTeleport) return;

        boolean claimed = player.clientClaimsLastOnGround;
        if (claimed != player.onGround) {
            flagWithSetback(V.write(verbose()).bool(claimed));
            player.checkManager.getNoFall().flipPlayerGroundStatus = true;
        }
    }
}
