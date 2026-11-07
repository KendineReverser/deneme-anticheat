package ac.deneme.anticheat.api.storage.backend;

import ac.deneme.anticheat.api.storage.category.Capability;
import ac.deneme.anticheat.api.storage.codec.EncodeShape;
import ac.deneme.anticheat.api.storage.registry.StoreId;
import ac.deneme.anticheat.api.storage.search.SearchResult;
import ac.deneme.anticheat.api.storage.search.SearchSpec;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Per-backend search SPI. Composed into {@link KindAdapter#ensureStore} for
 * categories that declare {@code @Searchable} fields; consulted by
 * {@code DataStore.search} for query execution.
 */
@ApiStatus.Experimental
public interface SearchAdapter {

    @NotNull EnumSet<Capability> searchCapabilities();

    /** Create the search index for a store, given its codec shape. Idempotent. */
    void ensureSearchIndex(@NotNull StoreId id, @NotNull EncodeShape shape) throws BackendException;

    void rebuildIndex(@NotNull StoreId id) throws BackendException;

    <R> @NotNull SearchResult<R> execute(@NotNull StoreId id, @NotNull EncodeShape shape,
                                         @NotNull SearchSpec spec) throws BackendException;
}
