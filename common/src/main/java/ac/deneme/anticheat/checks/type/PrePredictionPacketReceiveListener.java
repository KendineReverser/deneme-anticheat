package ac.deneme.anticheat.checks.type;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;

public interface PrePredictionPacketReceiveListener {
    void onPrePredictionPacketReceive(PacketReceiveEvent event);
}
