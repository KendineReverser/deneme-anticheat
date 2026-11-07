package ac.deneme.anticheat.checks.impl.badpackets;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PreViaPacketReceiveListener;
import ac.deneme.anticheat.checks.type.PreViaPacketSendListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.Combat;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCombatEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDeathCombatEvent;

@CheckData(name = "BadPacketsM", stableKey = "deneme.badpackets.respawn_alive", description = "Tried to respawn while alive", experimental = true)
public class BadPacketsM extends Check implements PreViaPacketReceiveListener, PreViaPacketSendListener {
    public BadPacketsM(final DenemePlayer player) {
        super(player);
    }

    private int exempt;
    private boolean menu;

    @Override
    public void onPreViaPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLIENT_STATUS
                || new WrapperPlayClientClientStatus(event).getAction() != WrapperPlayClientClientStatus.Action.PERFORM_RESPAWN) {
            return;
        }

        if (exempt > 0) {
            exempt--;
            return;
        }

        if (!player.compensatedEntities.self.isDead && !menu) {
            flag();
        }

        menu = player.compensatedEntities.self.isDead && player.packetStateData.showsDeathScreen;
    }

    @Override
    public void onPreViaPacketSend(PacketSendEvent event) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.CHANGE_GAME_STATE) {
            WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);
            if (packet.getReason() != WrapperPlayServerChangeGameState.Reason.WIN_GAME) return;

            if (packet.getValue() != 0 && packet.getValue() != 1) {
                return;
            }

            player.sendTransaction();
            player.addRealTimeTaskNow(() -> {

                exempt++;
                menu = false;
            });
        }

        if (event.getPacketType() == PacketType.Play.Server.DEATH_COMBAT_EVENT
                && new WrapperPlayServerDeathCombatEvent(event).getPlayerId() == player.entityID) {
            player.sendTransaction();
            player.addRealTimeTaskNow(this::onDeathCombatEvent);
        }

        if (event.getPacketType() == PacketType.Play.Server.COMBAT_EVENT) {
            WrapperPlayServerCombatEvent packet = new WrapperPlayServerCombatEvent(event);
            if (packet.getCombat() == Combat.ENTITY_DEAD && packet.getPlayerId() == player.entityID) {
                player.sendTransaction();
                player.addRealTimeTaskNow(this::onDeathCombatEvent);
            }
        }
    }

    private void onDeathCombatEvent() {
        if (player.packetStateData.showsDeathScreen) {
            menu = true;
        } else {
            exempt++;
        }
    }

    public void onRespawn() {
        menu = false;
    }

    public void onDeath() {
        if (player.packetStateData.showsDeathScreen) {
            menu = true;
        }
    }
}
