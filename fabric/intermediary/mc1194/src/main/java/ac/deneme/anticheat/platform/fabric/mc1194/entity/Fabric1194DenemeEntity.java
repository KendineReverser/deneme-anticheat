package ac.deneme.anticheat.platform.fabric.mc1194.entity;

import ac.deneme.anticheat.platform.fabric.mc1171.entity.Fabric1170DenemeEntity;
import ac.deneme.anticheat.platform.fabric.utils.thread.FabricFutureUtil;
import ac.deneme.anticheat.utils.math.Location;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;

public class Fabric1194DenemeEntity extends Fabric1170DenemeEntity {

    public Fabric1194DenemeEntity(Entity entity) {
        super(entity);
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        return FabricFutureUtil.supplySync(() -> {
            if (entity.getLevel() instanceof ServerLevel) {
                entity.teleportTo(
                        (ServerLevel) location.getWorld(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        EnumSet.noneOf(RelativeMovement.class), // todo change to match paper? Do they do this?
                        location.getYaw(),
                        location.getPitch()

                );
                return true;
            }
            return false;
        });
    }
}
