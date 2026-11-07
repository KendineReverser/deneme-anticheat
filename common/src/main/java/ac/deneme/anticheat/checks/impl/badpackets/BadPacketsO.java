package ac.deneme.anticheat.checks.impl.badpackets;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.checks.type.PacketSendListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;

import java.util.LinkedList;

@CheckData(name = "BadPacketsO", stableKey = "deneme.badpackets.invalid_keepalive", description = "Responded with a keepalive ID that was not sent by the server")
public class BadPacketsO extends Check implements PacketReceiveListener, PacketSendListener {
    private static final Verbose V = Verbose.of("id={slong}");

    private final LinkedList<Long> keepalives = new LinkedList<>();

    public BadPacketsO(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {
            keepalives.add(new WrapperPlayServerKeepAlive(event).getId());
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            final long id = new WrapperPlayClientKeepAlive(event).getId();

            for (long keepalive : keepalives) {
                if (keepalive == id) {

                    Long data;
                    do {
                        data = keepalives.poll();
                    } while (data != null && data != id);

                    return;
                }
            }

            if (flag(V.write(verbose()).slong(id)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
