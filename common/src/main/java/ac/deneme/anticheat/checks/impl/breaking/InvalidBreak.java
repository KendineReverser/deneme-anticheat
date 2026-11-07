package ac.deneme.anticheat.checks.impl.breaking;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.impl.verbose.VerboseCodecs;
import ac.deneme.anticheat.checks.type.BlockBreakListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

@CheckData(name = "InvalidBreak", stableKey = "deneme.breaking.invalid_break", description = "Sent impossible block face id")
public class InvalidBreak extends Check implements BlockBreakListener {
    private static final Verbose V = Verbose.of("face={sint}, action={digging}");

    public InvalidBreak(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.faceId == 255 && blockBreak.action == DiggingAction.CANCELLED_DIGGING && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
            return;
        }

        if (blockBreak.faceId < 0 || blockBreak.faceId > 5) {
            // ban
            if (flag(V.write(verbose())
                    .sint(blockBreak.faceId)
                    .uint(VerboseCodecs.enumId(blockBreak.action))) && shouldModifyPackets()) {
                blockBreak.cancel();
            }
        }
    }
}
