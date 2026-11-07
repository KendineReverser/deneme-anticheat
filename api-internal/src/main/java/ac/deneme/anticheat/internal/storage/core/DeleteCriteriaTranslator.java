package ac.deneme.anticheat.internal.storage.core;

import ac.deneme.anticheat.api.storage.category.Categories;
import ac.deneme.anticheat.api.storage.category.Category;
import ac.deneme.anticheat.api.storage.kind.Operation;
import ac.deneme.anticheat.api.storage.kind.ops.EntityOps;
import ac.deneme.anticheat.api.storage.kind.ops.EventStreamOps;
import ac.deneme.anticheat.api.storage.kind.ops.KeyValueScopedOps;
import ac.deneme.anticheat.api.storage.model.SettingScope;
import ac.deneme.anticheat.api.storage.query.DeleteCriteria;
import ac.deneme.anticheat.api.storage.query.Deletes;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Translates legacy {@link DeleteCriteria} into the equivalent v2
 * {@link Operation} so {@code DataStoreImpl.delete()} can dispatch
 * through the v2 adapter when a v2 route exists. Returns {@code null}
 * for untranslatable criteria (e.g. unknown category or criteria type)
 * — the caller falls back to legacy {@code Backend.delete()}.
 */
@ApiStatus.Internal
final class DeleteCriteriaTranslator {

    private DeleteCriteriaTranslator() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    static @Nullable Operation<Void> translate(Category<?> cat, DeleteCriteria criteria) {
        if (criteria instanceof Deletes.ByPlayer bp) {
            if (cat == Categories.VIOLATION) {
                return (Operation) new EventStreamOps.DeleteByPartitionOp(cat,
                    "player_uuid", bp.uuid());
            }
            if (cat == Categories.PLAYER_IDENTITY) {
                return (Operation) new EntityOps.DeleteByIdOp(cat, bp.uuid());
            }
            if (cat == Categories.SESSION) {

                return (Operation) new EntityOps.DeleteByIndexOp(cat,
                    "by_player_started", bp.uuid());
            }
            if (cat == Categories.SETTING) {

                return (Operation) new KeyValueScopedOps.RemoveAllOp<>(cat,
                    SettingScope.PLAYER, bp.uuid().toString());
            }
        }
        if (criteria instanceof Deletes.OlderThan ot) {
            if (cat == Categories.VIOLATION) {
                long cutoff = System.currentTimeMillis() - ot.maxAgeMs();
                return (Operation) new EventStreamOps.DeleteOlderThanOp(cat, cutoff);
            }

        }
        return null;
    }
}
