package ac.deneme.anticheat.checks.impl.timer;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PrePredictionPacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "TickTimer", stableKey = "deneme.timer.tick", description = "Did not send client tick end packet", setback = 1)
public class TickTimer extends Check implements PrePredictionPacketReceiveListener {
    private static final Verbose V = Verbose.of("type=[end|flying], packets={uint}");

    private boolean receivedTickEnd = true;
    private int flyingPackets = 0;

    public TickTimer(DenemePlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.supportsEndTick();
    }

    @Override
    public void onPrePredictionPacketReceive(PacketReceiveEvent event) {
        if (isFlying(event.getPacketType()) && !player.packetStateData.lastPacketWasTeleport) {
            if (!receivedTickEnd && flagWithSetback(V.write(verbose()).bool(false).uint(flyingPackets))) {
                handleViolation();
            }
            receivedTickEnd = false;
            flyingPackets++;
        } else if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            receivedTickEnd = true;
            if (flyingPackets > 1 && flagWithSetback(V.write(verbose()).bool(true).uint(flyingPackets))) {
                handleViolation();
            }
            flyingPackets = 0;
        }
    }

    private void handleViolation() {

        player.onPacketCancel();
    }
}
