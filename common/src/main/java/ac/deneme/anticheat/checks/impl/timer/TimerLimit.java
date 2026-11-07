package ac.deneme.anticheat.checks.impl.timer;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "TimerLimit", stableKey = "deneme.timer.limit", description = "The player has sent too many packets after high latency", setback = 10)
public class TimerLimit extends Timer {

    private long limitAbuseOverPing;

    public TimerLimit(DenemePlayer player) {
        super(player);
    }

    @Override
    public void doCheck(final PacketReceiveEvent event) {

        if (timerBalanceRealTime > System.nanoTime()) {

            if (!event.isCancelled()) {
                if (flag() && shouldSetback()) {
                    player.getSetbackTeleportUtil().executeNonSimulatingSetback();
                }
            }

            timerBalanceRealTime -= 50e6;
        }

        limitFallBehind();
    }

    @Override
    protected void limitFallBehind() {

        long playerClock = lastMovementPlayerClock;
        if (limitAbuseOverPing != -1 && System.nanoTime() - playerClock > limitAbuseOverPing) {
            playerClock = System.nanoTime() - limitAbuseOverPing;
        }
        timerBalanceRealTime = Math.max(timerBalanceRealTime, playerClock - clockDrift);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        super.onReload(config);
        limitAbuseOverPing = config.getLongElse(getConfigName() + ".ping-abuse-limit-threshold", 1000L);
        if (limitAbuseOverPing != -1) {
            limitAbuseOverPing *= (long) 1e6;
        }
    }
}
