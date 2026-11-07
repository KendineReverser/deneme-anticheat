package ac.deneme.anticheat.events.packets;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.checks.DenemeProcessor;
import ac.deneme.anticheat.checks.type.PacketSendListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import org.jetbrains.annotations.NotNull;

public class PacketChangeGameState extends DenemeProcessor implements PacketSendListener {

    public PacketChangeGameState(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(final @NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.CHANGE_GAME_STATE) return;
        WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);

        switch (packet.getReason()) {
            case CHANGE_GAME_MODE -> {
                player.sendTransaction();

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    GameMode previous = player.gamemode;
                    int gamemode = (int) packet.getValue();

                    if (gamemode < 0 || gamemode >= GameMode.values().length) {
                        player.gamemode = GameMode.SURVIVAL;
                    } else {
                        player.gamemode = GameMode.values()[gamemode];
                    }

                    if (previous == GameMode.SPECTATOR && player.gamemode != GameMode.SPECTATOR) {
                        DenemeAPI.INSTANCE.getSpectateManager().handlePlayerStopSpectating(player.uuid);
                    }
                });
            }

            case ENABLE_RESPAWN_SCREEN -> {
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_15)
                        || event.getServerVersion().isOlderThan(ServerVersion.V_1_15)) return;
                player.sendTransaction();
                final boolean enabled = packet.getValue() == 0f;
                player.addRealTimeTaskNow(() -> player.packetStateData.showsDeathScreen = enabled);
            }
        }
    }
}
