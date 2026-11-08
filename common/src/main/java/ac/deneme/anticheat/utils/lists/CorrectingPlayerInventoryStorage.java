package ac.deneme.anticheat.utils.lists;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.inventory.Inventory;
import ac.deneme.anticheat.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is responsible for lag compensation of the player's inventory
 * Since I don't want to spend months finding version differences in inventory
 * Or copy (and debug) over around 5k lines of code to accomplish inventories
 * Deneme uses a hybrid system for inventories - we lag compensate but rely on the server
 * for the ultimate source of truth, and resync if what we found is different from what the server sees
 * <p>
 * This also patches most desync's that happen with inventories on some versions like 1.8 or
 * other desync's introduced by mojang or viabackwards
 *
 * <p>
 * To accomplish this we:
 * - Track items changed when the player swaps or moves items in a basic inventory
 * - Track items when the player has placed a block, for example
 * - Track other item predictions by the client
 * <p>
 * There is somewhat of a race condition
 * The server's inventory state can only truly be read at the start of the tick
 * However, we read inventories async for performance reasons
 * This shouldn't cause any negative issues in practice, but it technically is wrong
 * <p>
 * Apply this only to the player's inventory for simplicity reasons
 * Horses and stuff, the metadata for saddles is server authoritative
 * No inventory directly affects us other than the player's inventory.
 */
public class CorrectingPlayerInventoryStorage extends InventoryStorage {

    private static final Set<String> SUPPORTED_INVENTORIES = new HashSet<>(
            Arrays.asList("CHEST", "DISPENSER", "DROPPER", "PLAYER", "ENDER_CHEST", "SHULKER_BOX", "BARREL", "CRAFTING", "CREATIVE")
    );
    private final DenemePlayer player;

    private final Map<Integer, Integer> serverIsCurrentlyProcessingThesePredictions = new ConcurrentHashMap<>();

    private final Map<Integer, Integer> pendingFinalizedSlot = new ConcurrentHashMap<>();

    public CorrectingPlayerInventoryStorage(DenemePlayer player, int size) {
        super(size);
        this.player = player;
    }

    public void handleClientClaimedSlotSet(int slotID) {
        if (slotID >= 0 && slotID <= Inventory.ITEMS_END) {
            pendingFinalizedSlot.put(slotID, DenemeAPI.INSTANCE.getTickManager().currentTick + 5);
        }
    }

    public void handleServerCorrectSlot(int slotID) {
        if (slotID >= 0 && slotID <= Inventory.ITEMS_END) {
            serverIsCurrentlyProcessingThesePredictions.put(slotID, player.lastTransactionSent.get());
        }
    }

    @Override
    public void setItem(int item, ItemStack stack) {

        int finalTransaction = serverIsCurrentlyProcessingThesePredictions.getOrDefault(item, -1);

        if (finalTransaction == -1 || player.lastTransactionReceived.get() >= finalTransaction) {

            pendingFinalizedSlot.put(item, DenemeAPI.INSTANCE.getTickManager().currentTick + 5);
            serverIsCurrentlyProcessingThesePredictions.remove(item);
        }

        super.setItem(item, stack);

        if (item == player.inventory.inventory.getSelected() + Inventory.HOTBAR_OFFSET) {
            player.attackCooldown.updateHeldItem(false);
        }
    }

    /**
     * Checks that the specified slot is in sync with the server's and resyncs if needed.
     * @param slot the slot to check
     */
    private void checkThatBukkitIsSynced(int slot) {

        if (player.platformPlayer == null) return;

        if (!player.inventory.isPacketInventoryActive) return;

        int bukkitSlot = getBukkitSlot(slot);

        if (bukkitSlot != -1) {
            ItemStack existing = getItem(slot);
            ItemStack serverside = player.platformPlayer.getInventory().getStack(bukkitSlot, slot);

            if (existing.getType() != serverside.getType() || existing.getAmount() != serverside.getAmount()) {
                DenemeAPI.INSTANCE.getScheduler().getEntityScheduler().execute(player.platformPlayer, DenemeAPI.INSTANCE.getDenemePlugin(),
                        () -> player.platformPlayer.updateInventory(), null, 0);
                setItem(slot, serverside);
            }
        }
    }

    public static int getBukkitSlot(int packetSlot) {

        if (packetSlot <= 4) {
            return -1;
        }

        if (packetSlot <= 8) {

            return (7 - packetSlot) + 36;
        }

        if (packetSlot <= 35) {
            return packetSlot;
        }

        if (packetSlot <= 44) {

            return packetSlot - 36;
        }

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) && packetSlot == 45) {
            return 40;
        }
        return -1;
    }

    public void tickWithBukkit() {
        if (player.platformPlayer == null) return;

        int tickID = DenemeAPI.INSTANCE.getTickManager().currentTick;
        for (Iterator<Map.Entry<Integer, Integer>> it = pendingFinalizedSlot.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Integer> entry = it.next();

            if (entry.getValue() <= tickID) {
                checkThatBukkitIsSynced(entry.getKey());
                it.remove();
            }
        }

        if (player.inventory.needResend) {
            DenemeAPI.INSTANCE.getScheduler().getEntityScheduler().execute(player.platformPlayer, DenemeAPI.INSTANCE.getDenemePlugin(), () -> {

                if (!player.inventory.needResend) return;

                if (SUPPORTED_INVENTORIES.contains(player.platformPlayer.getInventory().getOpenInventoryKey().toUpperCase(Locale.ROOT))) {
                    player.inventory.needResend = false;
                    player.platformPlayer.updateInventory();
                }
            }, null, 0);
        }

        if (tickID % 5 == 0) {
            int slotToCheck = (tickID / 5) % getSize();

            if (!pendingFinalizedSlot.containsKey(slotToCheck) && !serverIsCurrentlyProcessingThesePredictions.containsKey(slotToCheck)) {
                checkThatBukkitIsSynced(slotToCheck);
            }
        }
    }
}
