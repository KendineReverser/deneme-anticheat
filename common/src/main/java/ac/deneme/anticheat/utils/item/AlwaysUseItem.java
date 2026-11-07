package ac.deneme.anticheat.utils.item;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class AlwaysUseItem extends ItemBehaviour {

    public static final AlwaysUseItem INSTANCE = new AlwaysUseItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, DenemePlayer player, InteractionHand hand) {
        return true;
    }

}
