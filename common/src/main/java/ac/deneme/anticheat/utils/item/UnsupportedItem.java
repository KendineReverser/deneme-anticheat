package ac.deneme.anticheat.utils.item;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class UnsupportedItem extends ItemBehaviour {

    public static final UnsupportedItem INSTANCE = new UnsupportedItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, DenemePlayer player, InteractionHand hand) {
        return false;
    }

}
