package ac.deneme.anticheat.platform.fabric.mc1171.entity;

import ac.deneme.anticheat.platform.fabric.mc1161.entity.Fabric1161DenemeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class Fabric1170DenemeEntity extends Fabric1161DenemeEntity {

    public Fabric1170DenemeEntity(Entity entity) {
        super(entity);
    }

    @Override
    public boolean isDead() {
        return this.entity instanceof LivingEntity living ? living.isDeadOrDying() : this.entity.isRemoved();
    }
}
