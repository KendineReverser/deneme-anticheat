package ac.deneme.anticheat.events.packets;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCooldown;

public class PacketPlayerCooldown extends PacketListenerAbstract {

    public PacketPlayerCooldown() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SET_COOLDOWN) return;
        WrapperPlayServerSetCooldown cooldown = new WrapperPlayServerSetCooldown(event);

        DenemePlayer player = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        int lastTransactionSent = player.lastTransactionSent.get();

        if (cooldown.getCooldownTicks() == 0) {
            player.latencyUtils.addRealTimeTask(lastTransactionSent + 1,
                    () -> player.checkManager.getCompensatedCooldown().removeCooldown(cooldown.getCooldownGroup()));
        } else {
            player.latencyUtils.addRealTimeTask(lastTransactionSent,
                    () -> player.checkManager.getCompensatedCooldown().addCooldown(
                            cooldown.getCooldownGroup(),
                            cooldown.getCooldownTicks(),
                            lastTransactionSent
                    )
            );
        }
    }
}
