package ac.deneme.anticheat.manager;

import ac.deneme.anticheat.checks.DenemeProcessor;
import ac.deneme.anticheat.checks.type.PacketReceiveListener;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.math.DenemeMath;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

public class AttackCooldownHandler extends DenemeProcessor implements PacketReceiveListener {
    private int ticksSinceLastSwing;
    private ItemStack stack = ItemStack.EMPTY;

    private boolean stackChanged;

    public AttackCooldownHandler(DenemePlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (isAnimation(event.getPacketType())) {

            reset();
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == DiggingAction.CANCELLED_DIGGING && packet.getBlockFace() == BlockFace.DOWN) {

                reset();
            }
        }

        if (isTickPacket(event.getPacketType())) {
            if (!stackChanged) {
                ++ticksSinceLastSwing;
            }
            updateHeldItem(true);
            stackChanged = false;
        }
    }

    public void reset() {
        ticksSinceLastSwing = 0;
    }

    public void updateHeldItem(boolean tick) {
        if (!tick && player.supportsEndTick()) return;
        ItemStack held = player.inventory.getHeldItem().copy();

        if (!(stack.isEmpty() && held.isEmpty() || stack.getType() == held.getType() && (stack.isDamageableItem() || stack.getLegacyData() == held.getLegacyData()))) {
            reset();
            stackChanged = true;
        }

        stack = held;
    }

    public float getMinimumProgress() {
        return DenemeMath.clamp(((float) ticksSinceLastSwing + 0.5F) / (float) (1d / player.compensatedEntities.self.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0D), 0, 1);
    }
}
