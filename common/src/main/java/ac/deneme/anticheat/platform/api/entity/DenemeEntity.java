package ac.deneme.anticheat.platform.api.entity;

import ac.deneme.anticheat.api.DenemeIdentity;
import ac.deneme.anticheat.platform.api.world.PlatformWorld;
import ac.deneme.anticheat.utils.math.Location;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface DenemeEntity extends DenemeIdentity {
    /**
     * Eject any passenger.
     *
     * @return True if there was a passenger.
     */
    boolean eject();

    CompletableFuture<Boolean> teleportAsync(Location location);

    @NotNull
    Object getNative();

    boolean isDead();

    PlatformWorld getWorld();

    Location getLocation();

    double distanceSquared(double x, double y, double z);
}
