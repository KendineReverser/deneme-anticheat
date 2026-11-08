package ac.deneme.anticheat.platform.fabric.utils.thread;

import ac.deneme.anticheat.DenemeAPI;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FabricFutureUtil {
    public static <U> CompletableFuture<U> supplySync(Supplier<U> entityTeleportSupplier) {
        CompletableFuture<U> ret = new CompletableFuture<>();
        DenemeAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(DenemeAPI.INSTANCE.getDenemePlugin(),
                () -> ret.complete(entityTeleportSupplier.get()));
        return ret;
    }
}
