package ac.deneme.anticheat.manager.tick.impl;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.manager.tick.Tickable;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;

public class ClientVersionSetter implements Tickable {
    @Override
    public void tick() {
        for (DenemePlayer player : DenemeAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            // channel was somehow closed without us getting a disconnect event
            if (!ChannelHelper.isOpen(player.user.getChannel())) {
                DenemeAPI.INSTANCE.getPlayerDataManager().onDisconnect(player.user);
                continue;
            }

            player.pollData();
        }
    }
}
