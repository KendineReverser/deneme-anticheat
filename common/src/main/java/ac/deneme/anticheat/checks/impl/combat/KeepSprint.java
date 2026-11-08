package ac.deneme.anticheat.checks.impl.combat;

import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "KeepSprint", stableKey = "deneme.combat.keep_sprint", description = "Kept sprint after a hit without resetting it", decay = 0.02)
public class KeepSprint extends Check implements PacketReceiveListener {

    private static final long EVALUATE_WINDOW_MS = 500L;
    private static final long WINDOW_MS = 10000L;
    private static final int FLAG_HITS = 4;

    private boolean pendingEvaluation;
    private boolean sprintToggleSeen;
    private long attackAt;
    private int hits;
    private long firstHitAt;

    public KeepSprint(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (player.disableDeneme || player.gamemode == null
                || player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR
                || player.inVehicle()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction action = new WrapperPlayClientEntityAction(event);
            if (action.getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING
                    || action.getAction() == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                sprintToggleSeen = true;
            }
            return;
        }

        boolean attack = event.getPacketType() == PacketType.Play.Client.ATTACK
                || (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                && new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK);

        if (attack) {
            if (new WrapperPlayClientAttack(event).getEntityId() == -1) {
                return;
            }
            pendingEvaluation = player.isSprinting;
            sprintToggleSeen = false;
            attackAt = System.currentTimeMillis();
            return;
        }

        if (!pendingEvaluation || !isUpdate(event.getPacketType())) {
            return;
        }

        pendingEvaluation = false;

        if (System.currentTimeMillis() - attackAt > EVALUATE_WINDOW_MS) {
            return;
        }

        if (!player.isSprinting || sprintToggleSeen) {
            return;
        }

        long now = System.currentTimeMillis();
        if (hits == 0 || now - firstHitAt > WINDOW_MS) {
            hits = 0;
            firstHitAt = now;
        }
        hits++;
        CombatEvidence.report(player, "keep_sprint", 1.0);

        if (hits >= FLAG_HITS) {
            flag();
            hits = 0;
            firstHitAt = now;
        }
    }
}
