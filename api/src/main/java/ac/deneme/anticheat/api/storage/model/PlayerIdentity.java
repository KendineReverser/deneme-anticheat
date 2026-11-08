package ac.deneme.anticheat.api.storage.model;

import ac.deneme.anticheat.api.storage.codec.Id;
import ac.deneme.anticheat.api.storage.codec.Indexed;
import ac.deneme.anticheat.api.storage.codec.MergeMax;
import ac.deneme.anticheat.api.storage.codec.MergeMin;
import ac.deneme.anticheat.api.storage.codec.Name;
import ac.deneme.anticheat.api.storage.codec.Nullable;
import ac.deneme.anticheat.api.storage.codec.Persistent;
import ac.deneme.anticheat.api.storage.codec.Value;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

/**
 * Immutable read-side record for one player's identity. One row per UUID.
 * {@code currentName} is the last known display name; carries an
 * {@link Indexed} role so name-prefix lookups work — name-lookup
 * convention is to write the lowercased form to a parallel store at the
 * adapter level (Mongo: companion {@code current_name_lower} index on
 * the same field via {@link ac.deneme.anticheat.api.storage.kind.IndexSpec}'s
 * {@code caseInsensitivePrefix}).
 */
@ApiStatus.Experimental
@Persistent
public record PlayerIdentity(
        @Id                                          UUID uuid,
        @Indexed @Nullable                           String currentName,

        @Value @MergeMin @Name("first_seen")         long firstSeenEpochMs,
        @Value @MergeMax @Name("last_seen")          long lastSeenEpochMs) {

    public PlayerIdentity {
        if (uuid == null) throw new IllegalArgumentException("uuid");
    }
}
