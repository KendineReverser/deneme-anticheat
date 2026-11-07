package ac.deneme.anticheat.checks.impl.packetorder;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderL", stableKey = "deneme.packetorder.drop_item_order", description = "Sent drop, inventory open, or offhand swap packets in an invalid order", experimental = true)
public class PacketOrderL extends Check implements PacketReceiveListener, PostPredictionListener {
    private static final Verbose V = Verbose.of("[inventory|swap]");

    static final int ACTION_INVENTORY = 0;
    static final int ACTION_SWAP = 1;

    public PacketOrderL(final DenemePlayer player) {
        super(player);
    }

    private final ArrayDeque<Integer> flags = new ArrayDeque<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            if (new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                if (player.packetOrderProcessor.isDropping()) {
                    if (!player.canSkipTicks()) {
                        if (flag(V.write(verbose()).bool(true)) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else {
                        flags.add(ACTION_INVENTORY);
                    }
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            if (new WrapperPlayClientPlayerDigging(event).getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
                if (player.packetOrderProcessor.isDropping()) {
                    if (!player.canSkipTicks()) {
                        if (flag(V.write(verbose()).bool(false)) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else {
                        flags.add(ACTION_SWAP);
                    }
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (int action : flags) {
                flag(V.write(verbose()).bool(action == ACTION_INVENTORY));
            }
        }

        flags.clear();
    }
}
