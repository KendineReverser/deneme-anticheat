package ac.deneme.anticheat.checks.impl.packetorder;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.checks.type.PreViaPacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "PacketOrderH", stableKey = "deneme.packetorder.sneak_sprint_order", description = "Sent sprinting and sneaking state changes in an invalid packet order", experimental = true)
public class PacketOrderH extends Check implements PreViaPacketReceiveListener, PostPredictionListener {

    private int invalid;

    public PacketOrderH(final DenemePlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {

        return player.getClientVersion().isOlderThan(ClientVersion.V_1_21_6);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            switch (new WrapperPlayClientEntityAction(event).getAction()) {
                case START_SPRINTING, STOP_SPRINTING -> {
                    if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2) && player.packetOrderProcessor.isSneaking()) {
                        if (!player.canSkipTicks()) {
                            flag();
                        } else {
                            invalid++;
                        }
                    }
                }
                case START_SNEAKING, STOP_SNEAKING -> {
                    if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && player.packetOrderProcessor.isSprinting()) {
                        if (!player.canSkipTicks()) {
                            flag();
                        } else {
                            invalid++;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (; invalid >= 1; invalid--) {
                flag();
            }
        }

        invalid = 0;
    }
}
