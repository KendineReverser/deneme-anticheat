package ac.deneme.anticheat.events.packets;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class PacketHidePlayerInfo extends PacketListenerAbstract {

    public PacketHidePlayerInfo() {
        super(PacketListenerPriority.HIGHEST);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {

            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_12_2))
                return;

            DenemePlayer receiver = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());

            if (receiver == null) {
                return;
            }

            WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo(event);

            if (info.getAction() == WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE || info.getAction() == WrapperPlayServerPlayerInfo.Action.ADD_PLAYER) {
                List<WrapperPlayServerPlayerInfo.PlayerData> nmsPlayerInfoDataList = info.getPlayerDataList();

                int hideCount = 0;
                for (WrapperPlayServerPlayerInfo.PlayerData playerData : nmsPlayerInfoDataList) {
                    if (DenemeAPI.INSTANCE.getSpectateManager().shouldHidePlayer(receiver, playerData)) {
                        hideCount++;
                        if (playerData.getGameMode() == GameMode.SPECTATOR)
                            playerData.setGameMode(GameMode.SURVIVAL);
                    }
                }

                if (hideCount == nmsPlayerInfoDataList.size() && info.getAction() == WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE) {
                    event.setCancelled(true);
                } else if (hideCount > 0) {
                    event.markForReEncode(true);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            DenemePlayer receiver = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (receiver == null) return;

            WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
            EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = wrapper.getActions();
            if (!actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE)) return;

            boolean onlyGameMode = actions.size() == 1;
            int hideCount = 0;
            List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> modified = new ArrayList<>(wrapper.getEntries().size());

            for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : wrapper.getEntries()) {

                WrapperPlayServerPlayerInfoUpdate.PlayerInfo modifiedPacket = null;
                if (DenemeAPI.INSTANCE.getSpectateManager().shouldHidePlayer(receiver, entry.getGameProfile().getUUID())) {
                    hideCount++;

                    if (entry.getGameMode() == GameMode.SPECTATOR) {
                        modifiedPacket = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(entry);
                        modifiedPacket.setGameMode(GameMode.SURVIVAL);
                        modified.add(modifiedPacket);
                    }
                }

                if (modifiedPacket == null) {
                    modified.add(entry);
                } else if (!onlyGameMode) {
                    modified.add(modifiedPacket);
                }
            }

            if (hideCount == modified.size()) {
                if (onlyGameMode) {
                    event.setCancelled(true);
                } else {
                    wrapper.getActions().remove(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE);
                    event.markForReEncode(true);
                }
            } else {
                wrapper.setEntries(modified);
                event.markForReEncode(true);
            }
        }
    }
}
