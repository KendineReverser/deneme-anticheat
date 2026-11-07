package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "SwingRatio", stableKey = "deneme.combat.swing_ratio", description = "Attacked more often than it swung its arm", decay = 0.02)
public class SwingRatio extends Check implements PacketReceiveListener {

    private static final int WINDOW_ATTACKS = 120;
    private static final int MAXIMUM_MISSING_SWINGS = 3;

    private int attacks;
    private int swings;

    public SwingRatio(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (player.disableDeneme || player.gamemode == null
                || player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            if (new WrapperPlayClientAnimation(event).getHand() == InteractionHand.MAIN_HAND) {
                swings++;
            }
            return;
        }

        boolean attack = event.getPacketType() == PacketType.Play.Client.ATTACK
                || (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                && new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK);

        if (!attack) {
            return;
        }

        attacks++;

        if (attacks < WINDOW_ATTACKS) {
            return;
        }

        int missing = attacks - swings;

        if (missing > MAXIMUM_MISSING_SWINGS) {
            CombatEvidence.report(player, "swing_ratio", 1.0);
            flag(String.format("attacks=%d swings=%d missing=%d", attacks, swings, missing));
        } else {
            reward();
        }

        attacks = 0;
        swings = 0;
    }
}
