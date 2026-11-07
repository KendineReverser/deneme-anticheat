package ac.deneme.anticheat.api.storage.backend;

import ac.deneme.anticheat.api.storage.admin.CollectionStats;
import ac.deneme.anticheat.api.storage.admin.ExplainPlan;
import ac.deneme.anticheat.api.storage.admin.IndexStats;
import ac.deneme.anticheat.api.storage.kind.Operation;
import ac.deneme.anticheat.api.storage.registry.StoreId;
import ac.deneme.anticheat.api.storage.search.SearchSpec;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Per-backend admin SPI. Synchronous; called from the reader pool by the
 * {@code AdminAccess} facade.
 */
@ApiStatus.Experimental
public interface AdminAdapter {

    @NotNull ExplainPlan explain(@NotNull StoreId id, @NotNull Operation<?> op) throws BackendException;

    @NotNull ExplainPlan explainSearch(@NotNull StoreId id, @NotNull SearchSpec spec) throws BackendException;

    void reindex(@NotNull StoreId id) throws BackendException;

    void rebuildSearchIndex(@NotNull StoreId id) throws BackendException;

    void setRetention(@NotNull StoreId id, @NotNull Duration retention) throws BackendException;

    @NotNull IndexStats indexStats(@NotNull StoreId id) throws BackendException;

    @NotNull CollectionStats collectionStats(@NotNull StoreId id) throws BackendException;
}
