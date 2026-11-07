package ac.deneme.anticheat.checks.impl.crash;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "CrashB", stableKey = "deneme.crash.creative_while_not_creative", description = "Sent creative mode inventory click packets while not in creative mode")
public class CrashB extends Check implements PacketReceiveListener {
    public CrashB(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            if (player.gamemode != GameMode.CREATIVE) {
                event.setCancelled(true);
                player.onPacketCancel();
                flag(); // Could be transaction split, no need to setback though
            }
        }
    }
}
