package ac.deneme.anticheat.manager;

import ac.deneme.anticheat.manager.tick.Tickable;
import ac.deneme.anticheat.manager.tick.impl.ClearRecentlyUpdatedBlocks;
import ac.deneme.anticheat.manager.tick.impl.ClientVersionSetter;
import ac.deneme.anticheat.manager.tick.impl.ResetTick;
import ac.deneme.anticheat.manager.tick.impl.TickInventory;
import ac.deneme.anticheat.manager.tick.impl.TickPermissions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

public class TickManager {

    public int currentTick;
    private final ClassToInstanceMap<Tickable> syncTick;
    private final ClassToInstanceMap<Tickable> asyncTick;

    public TickManager() {
        syncTick = new ImmutableClassToInstanceMap.Builder<Tickable>()
                .put(ResetTick.class, new ResetTick())
                .build();

        asyncTick = new ImmutableClassToInstanceMap.Builder<Tickable>()
                .put(ClientVersionSetter.class, new ClientVersionSetter())
                .put(TickPermissions.class, new TickPermissions())
                .put(TickInventory.class, new TickInventory())
                .put(ClearRecentlyUpdatedBlocks.class, new ClearRecentlyUpdatedBlocks())
                .build();
    }

    public void tickSync() {
        currentTick++;
        for (Tickable tickable : syncTick.values()) {
            tickable.tick();
        }
    }

    public void tickAsync() {
        for (Tickable tickable : asyncTick.values()) {
            tickable.tick();
        }
    }
}
