package ac.deneme.anticheat.checks.impl.badpackets;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "BadPacketsR", stableKey = "deneme.badpackets.position_starvation", description = "Stopped sending position updates while still responding to transactions", decay = 0.25, experimental = true)
public class BadPacketsR extends Check implements PacketReceiveListener {
    private static final Verbose V = Verbose.of("time={ulong}ms, lst={ulong}ms, positions={uint}");

    private int positions = 0;
    private long clock = 0;
    private long lastTransTime;
    private int oldTransId = 0;

    public BadPacketsR(final DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (isTransaction(event.getPacketType()) && player.packetStateData.lastTransactionPacketWasValid) {
            long ms = (player.getPlayerClockAtLeast() - clock) / 1000000L;
            long diff = (System.currentTimeMillis() - lastTransTime);
            if (diff > 2000 && ms > 2000) {
                if (positions == 0 && clock != 0 && player.cameraEntity.isSelf() && !player.compensatedEntities.self.isDead) {
                    flag(V.write(verbose()).ulong(ms).ulong(diff).uint(positions));
                } else {
                    reward();
                }
                player.compensatedEntities.entitiesRemovedThisTick.clear();
                player.compensatedWorld.removeInvalidPistonLikeStuff(oldTransId);
                positions = 0;
                clock = player.getPlayerClockAtLeast();
                lastTransTime = System.currentTimeMillis();
                oldTransId = player.lastTransactionSent.get();
            }
        }
        //
        if ((event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) && !player.inVehicle()) {
            positions++;
        } else if ((event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE || event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE)
                && player.inVehicle()) {
            positions++;
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION && player.inVehicle()
                && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2)) {
            // 1.21.2 replaced steer vehicle with player input, which is only sent when the input changes
            // The rotation is the only packet a passenger still sends every tick
            positions++;
        }
    }

}
