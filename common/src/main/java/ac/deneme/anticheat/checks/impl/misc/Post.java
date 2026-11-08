package ac.deneme.anticheat.checks.impl.misc;

import ac.deneme.anticheat.api.storage.verbose.Verbose;
import ac.deneme.anticheat.checks.Check;
import ac.deneme.anticheat.checks.CheckData;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.checks.type.PacketSendListener;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.checks.type.PostPredictionListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.update.PredictionComplete;
import ac.deneme.anticheat.utils.lists.EvictingQueue;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.*;

@CheckData(name = "Post", stableKey = "deneme.post.invalid_order", description = "Sent packets out of the expected movement tick order")
public class Post extends Check implements PacketReceiveListener, PacketSendListener, PostPredictionListener {
    private static final Verbose V = Verbose.of("{str}");

    private final ArrayDeque<PacketTypeCommon> post = new ArrayDeque<>();

    private final List<String> flags = new EvictingQueue<>(10);
    private boolean sentFlying = false;
    private int isExemptFromSwingingCheck = Integer.MIN_VALUE;

    public Post(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (flags.isEmpty()) {
            return;
        }

        if (player.isTickingReliablyFor(3)) {
            for (String flag : flags) {
                flag(V.write(verbose()).str(flag));
            }
        }

        flags.clear();
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_ANIMATION) {
            WrapperPlayServerEntityAnimation animation = new WrapperPlayServerEntityAnimation(event);
            if (animation.getEntityId() == player.entityID) {
                if (animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM ||
                        animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND) {
                    isExemptFromSwingingCheck = player.lastTransactionSent.get();
                }
            }
        }
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (isTickPacket(event.getPacketType())) {
            post.clear();
            sentFlying = true;
            return;
        }

        PacketTypeCommon packetType = event.getPacketType();
        if (isTransaction(packetType) && player.packetStateData.lastTransactionPacketWasValid) {
            if (sentFlying && !post.isEmpty()) {
                flags.add(post.getFirst().toString().toLowerCase(Locale.ROOT).replace("_", " ") + " v" + player.getClientVersion().getReleaseName());
            }
            post.clear();
            sentFlying = false;
            return;
        }

        if (!sentFlying) {
            return;
        }

        if (PLAYER_ABILITIES.equals(packetType)
                || (HELD_ITEM_CHANGE.equals(packetType) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8))
                || INTERACT_ENTITY.equals(packetType) || PLAYER_BLOCK_PLACEMENT.equals(packetType)
                || ATTACK.equals(packetType) || SPECTATE_ENTITY.equals(packetType) || PUNCH.equals(packetType)
                || USE_ITEM.equals(packetType) || PLAYER_DIGGING.equals(packetType)) {
            post.add(event.getPacketType());
        } else if (CLICK_WINDOW.equals(packetType) && player.getClientVersion().isOlderThan(ClientVersion.V_1_13)) {

            post.add(event.getPacketType());
        } else if (ANIMATION.equals(packetType)
                && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                || PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8))
                && player.getClientVersion().isOlderThan(ClientVersion.V_1_13)
                && isExemptFromSwingingCheck < player.lastTransactionReceived.get()) {
            post.add(event.getPacketType());
        } else if (ENTITY_ACTION.equals(packetType)
                && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) || new WrapperPlayClientEntityAction(event).getAction() != WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA)) {

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_3) && player.inVehicle()) {
                return;
            }
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) && new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.LEAVE_BED)
                return;
            post.add(event.getPacketType());
        }
    }
}
