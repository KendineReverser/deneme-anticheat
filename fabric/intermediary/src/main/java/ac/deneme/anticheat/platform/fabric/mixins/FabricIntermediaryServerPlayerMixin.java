package ac.deneme.anticheat.platform.fabric.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(ServerPlayer.class)
@Implements(@Interface(iface = ac.deneme.anticheat.platform.fabric.inject.FabricServerPlayerHandle.class, prefix = "deneme$"))
abstract class FabricIntermediaryServerPlayerMixin extends Player {

    public FabricIntermediaryServerPlayerMixin(Level level, BlockPos blockPos, GameProfile gameProfile) {
        super(level, blockPos, gameProfile);
    }

    public void deneme$resyncSharedFlags() {
        this.getEntityData().getItem(DATA_SHARED_FLAGS_ID).setDirty(true);
        this.getEntityData().isDirty = true;
    }

    public boolean deneme$isDead() {
        return this.isDeadOrDying();
    }

    public void deneme$sendSystemText(Object nativeComponent) {
        this.displayClientMessage((Component) nativeComponent, false);
    }

    public boolean deneme$isDisconnected() {
        return ((ServerPlayer) (Object) this).hasDisconnected();
    }

    public String deneme$usernameString() {
        return this.getName().getString();
    }

    public void deneme$broadcastInventoryChanges() {
        this.containerMenu.broadcastChanges();
    }

    public void deneme$stopUsingItem() {
        this.stopUsingItem();
    }

    public boolean deneme$isUsingItem() {
        return this.isUsingItem();
    }

    public double deneme$posX() {
        return this.getX();
    }

    public double deneme$posY() {
        return this.getY();
    }

    public double deneme$posZ() {
        return this.getZ();
    }

    public UUID deneme$uuid() {
        return this.getUUID();
    }

    public Object deneme$vehicleEntity() {
        return this.getVehicle();
    }

    public Object deneme$gameMode() {
        return ((ServerPlayer) (Object) this).gameMode.getGameModeForPlayer();
    }

    public Object deneme$heldItemStack() {
        return this.inventory.getSelected();
    }

    public Object deneme$inventoryItemAt(int slot) {
        return this.inventory.getItem(slot);
    }

    public Object deneme$usedItemHand() {
        return this.getUsedItemHand();
    }

    public int deneme$inventorySlotCount() {
        return this.inventory.getContainerSize();
    }
}
