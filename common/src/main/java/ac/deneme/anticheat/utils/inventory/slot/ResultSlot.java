package ac.deneme.anticheat.utils.inventory.slot;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class ResultSlot extends Slot {

    public ResultSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }

    @Override
    public void onTake(DenemePlayer player, ItemStack itemStack) {
        // Resync the player's inventory
    }
}
