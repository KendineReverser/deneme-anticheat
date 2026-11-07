package ac.deneme.anticheat.events.packets;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.checks.impl.combat.InvalidInteractTarget;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntity;
import ac.deneme.anticheat.utils.data.packetentity.PacketEntityHorse;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSpectateEntity;
import org.jetbrains.annotations.NotNull;

public class PacketPlayerAttack extends PacketListenerAbstract {

    public PacketPlayerAttack() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            DenemePlayer player = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            int entityId = interact.getEntityId();

            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                onAttack(event, player, entityId);
            } else {
                if (isInvalidEntity(event, player, entityId)) return;

                if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT) {

                    if (player.compensatedEntities.getEntity(entityId) instanceof PacketEntityHorse
                            && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_13)) {
                        player.packetStateData.horseInteractCausedForcedRotation = true;
                    }
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() != DiggingAction.STAB) return;

            DenemePlayer player = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            if (player.isResetItemUsageOnAttack()) {
                DenemeAPI.INSTANCE.getItemResetHandler().resetItemUsage(player.platformPlayer);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            DenemePlayer player = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            onAttack(event, player, packet.getEntityId());
        } else if (event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY) {
            DenemePlayer player = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            WrapperPlayClientSpectateEntity packet = new WrapperPlayClientSpectateEntity(event);
            onAttack(event, player, packet.getEntityId());
        }
    }

    private static void onAttack(@NotNull PacketReceiveEvent event, @NotNull DenemePlayer player, int entityId) {
        if (isInvalidEntity(event, player, entityId)) return;

        if (player.isResetItemUsageOnAttack()) {
            DenemeAPI.INSTANCE.getItemResetHandler().resetItemUsage(player.platformPlayer);
        }

        if (player.compensatedEntities.self.getAttributeValue(Attributes.ATTACK_DAMAGE) <= 0) return;

        ItemStack heldItem = player.inventory.getHeldItem();
        PacketEntity entity = player.compensatedEntities.getEntity(entityId);

        if (entity != null && (!entity.isLivingEntity || entity.getType() == EntityTypes.PLAYER || entity.getType() == EntityTypes.PAINTING
                || entity.getType() == EntityTypes.ENDER_DRAGON && player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2))) {
            int knockbackLevel = player.getClientVersion().isOlderThan(ClientVersion.V_1_21) && heldItem != null
                    ? heldItem.getEnchantmentLevel(EnchantmentTypes.KNOCKBACK)
                    : 0;
            final boolean hasNegativeKB = knockbackLevel < 0;

            final boolean isLegacyPlayer = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8);

            final boolean sufficientCooldownProgress = isLegacyPlayer || player.attackCooldown.getMinimumProgress() > 0.9F;

            if (!isLegacyPlayer) {
                knockbackLevel = Math.max(knockbackLevel, 0);
            }

            if (player.lastSprinting && !hasNegativeKB && sufficientCooldownProgress || knockbackLevel > 0) {
                player.minAttackSlow++;
                player.maxAttackSlow++;

                if (knockbackLevel == 0) {
                    player.maxAttackSlow = player.minAttackSlow = 1;
                }
            } else if (!isLegacyPlayer && player.lastSprinting) {

                if (player.maxAttackSlow > 0
                        && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)
                        && player.compensatedEntities.self.getAttributeValue(Attributes.ATTACK_SPEED) < 16) {
                    return;
                }

                player.maxAttackSlow++;
            }
        }

        if (player.gamemode != GameMode.SPECTATOR) {
            player.attackCooldown.reset();
        }
    }

    private static boolean isInvalidEntity(@NotNull PacketReceiveEvent event, @NotNull DenemePlayer player, int entityId) {

        if (!player.compensatedEntities.entityMap.containsKey(entityId) && !player.compensatedEntities.serverPositionsMap.containsKey(entityId)

                && (!player.compensatedEntities.entitiesRemovedThisTick.contains(entityId) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14))) {
            final InvalidInteractTarget check = player.checkManager.get(InvalidInteractTarget.class);
            if (check.flag("entityId=" + entityId) && check.shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            return true;
        }
        return false;
    }
}
