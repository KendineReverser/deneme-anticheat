package ac.deneme.anticheat.checks.impl.timer;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PrePredictionPacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "Timer", stableKey = "deneme.timer.timer", configName = "TimerA", description = "The players game is running faster than normal", setback = 10)
public class Timer extends Check implements PrePredictionPacketReceiveListener {
    protected long timerBalanceRealTime = 0;

    protected long knownPlayerClockTime = (long) (System.nanoTime() - 6e10);
    protected long lastMovementPlayerClock = (long) (System.nanoTime() - 6e10);

    protected long clockDrift;

    protected boolean hasGottenMovementAfterTransaction = false;

    public Timer(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPrePredictionPacketReceive(final PacketReceiveEvent event) {
        if (hasGottenMovementAfterTransaction && checkForTransaction(event.getPacketType())) {
            knownPlayerClockTime = lastMovementPlayerClock;
            lastMovementPlayerClock = player.getPlayerClockAtLeast();
            hasGottenMovementAfterTransaction = false;
        }

        if (!shouldCountPacketForTimer(event.getPacketType())) return;

        hasGottenMovementAfterTransaction = true;
        timerBalanceRealTime += 50e6;

        doCheck(event);
    }

    public void doCheck(final PacketReceiveEvent event) {
        if (timerBalanceRealTime > System.nanoTime()) {
            if (flag()) {

                if (shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }

                if (shouldSetback()) {
                    player.getSetbackTeleportUtil().executeNonSimulatingSetback();
                }
            }

            timerBalanceRealTime -= 50e6;
        }

        limitFallBehind();
    }

    protected void limitFallBehind() {
        timerBalanceRealTime = Math.max(timerBalanceRealTime, lastMovementPlayerClock - clockDrift);
    }

    public boolean checkForTransaction(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PONG ||
                packetType == PacketType.Play.Client.WINDOW_CONFIRMATION;
    }

    public boolean shouldCountPacketForTimer(PacketTypeCommon packetType) {

        return isTickPacket(packetType);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        clockDrift = (long) (config.getDoubleElse(getConfigName() + ".drift", 120.0) * 1e6);
    }
}
