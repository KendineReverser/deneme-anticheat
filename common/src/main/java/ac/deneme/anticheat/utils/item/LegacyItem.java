package ac.deneme.anticheat.utils.item;

import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.FoodProperties;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemConsumable;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class LegacyItem extends ItemBehaviour {

    public static final LegacyItem INSTANCE = new LegacyItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, DenemePlayer player, InteractionHand hand) {
        final ItemType material = item.getType();

        final ItemConsumable consumable = item.getComponentOr(ComponentTypes.CONSUMABLE, null);
        final FoodProperties foodComponent = item.getComponentOr(ComponentTypes.FOOD, null);

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) && consumable != null && foodComponent == null) {
            return true;
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5) && foodComponent != null) {
            if (foodComponent.isCanAlwaysEat() || player.food < 20 || player.gamemode == GameMode.CREATIVE) {
                return true;
            }
        }

        if (material.hasAttribute(ItemTypes.ItemAttribute.EDIBLE) &&
                (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15) || player.gamemode != GameMode.CREATIVE)
                || material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET) {

            if (material == ItemTypes.SPLASH_POTION)
                return false;

            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9) && item.getLegacyData() > 16384) {
                return false;
            }

            if (material == ItemTypes.POTION || material == ItemTypes.MILK_BUCKET
                    || material == ItemTypes.GOLDEN_APPLE || material == ItemTypes.ENCHANTED_GOLDEN_APPLE
                    || material == ItemTypes.HONEY_BOTTLE || material == ItemTypes.SUSPICIOUS_STEW ||
                    material == ItemTypes.CHORUS_FRUIT) {
                return true;
            }

            if (material.hasAttribute(ItemTypes.ItemAttribute.EDIBLE) && ((player.platformPlayer != null && player.food < 20) || player.gamemode == GameMode.CREATIVE)) {
                return true;
            }
        }

        if (material == ItemTypes.SHIELD && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            return true;
        }

        final NBTCompound nbt = item.getNBT();
        if (material == ItemTypes.CROSSBOW && nbt != null && nbt.getBoolean("Charged")) {
            return false;
        }

        if (material == ItemTypes.TRIDENT
                && item.getDamageValue() < item.getMaxDamage() - 1
                && (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13_2)
                || player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8))) {
            return item.getEnchantmentLevel(EnchantmentTypes.RIPTIDE) <= 0;
        }

        if (material == ItemTypes.BOW || material == ItemTypes.CROSSBOW) {

            return false;
        }

        if (material == ItemTypes.SPYGLASS && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            return true;
        }

        if (material == ItemTypes.GOAT_HORN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19)) {
            return true;
        }

        if (material.hasAttribute(ItemTypes.ItemAttribute.SWORD)) {
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                return true;
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) {
                return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_4);
            }
        }

        return false;
    }

}
