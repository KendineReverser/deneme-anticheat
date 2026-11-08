package ac.deneme.anticheat.platform.fabric.mixins;

import ac.deneme.anticheat.platform.fabric.inject.FabricEntityHandle;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(Entity.class)
@Implements(@Interface(iface = FabricEntityHandle.class, prefix = "deneme$"))
abstract class FabricIntermediaryEntityMixin {

    public UUID deneme$fabricEntityUuid() {
        return ((Entity) (Object) this).getUUID();
    }

    public boolean deneme$fabricEjectPassengers() {
        Entity entity = (Entity) (Object) this;
        if (entity.isVehicle()) {
            entity.ejectPassengers();
            return true;
        }
        return false;
    }

    public Object deneme$fabricWorld() {
        return ((Entity) (Object) this).level;
    }

    public double deneme$fabricPosX() {
        return ((Entity) (Object) this).getX();
    }

    public double deneme$fabricPosY() {
        return ((Entity) (Object) this).getY();
    }

    public double deneme$fabricPosZ() {
        return ((Entity) (Object) this).getZ();
    }

    public float deneme$fabricYaw(float partialTick) {
        return ((Entity) (Object) this).getViewYRot(partialTick);
    }

    public float deneme$fabricPitch(float partialTick) {
        return ((Entity) (Object) this).getViewXRot(partialTick);
    }
}
