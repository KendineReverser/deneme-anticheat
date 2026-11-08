package ac.deneme.anticheat.internal.storage.core;

import ac.deneme.anticheat.api.storage.backend.BackendV2;
import ac.deneme.anticheat.api.storage.backend.KindAdapter;
import ac.deneme.anticheat.api.storage.category.Capability;
import ac.deneme.anticheat.api.storage.category.Category;
import ac.deneme.anticheat.api.storage.kind.DataKind;
import ac.deneme.anticheat.api.storage.kind.EventStream;
import ac.deneme.anticheat.api.storage.kind.KeyValueScoped;
import ac.deneme.anticheat.api.storage.registry.Migration;
import ac.deneme.anticheat.api.storage.registry.MigrationContext;
import ac.deneme.anticheat.api.storage.registry.StoreId;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One-shot startup helper that {@code ensureStore}s + runs migrations for
 * a set of v2 {@link DataKind} bindings against one {@link BackendV2}, then
 * appends the wired routes into a caller-supplied {@link V2Routes.Builder}.
 *
 * <p>Intended to be called from the plugin's storage-lifecycle layer
 * (Deneme's {@code DataStoreLifecycle}) once per backend that hosts v2
 * categories. For multi-backend cutovers, call {@link #install} once per
 * backend with the same builder; {@link V2Routes.Builder#build()} at the
 * end produces the merged routes ready for {@code DataStoreImpl.withV2Routes}.
 *
 * <p><strong>Failure semantics — fail-closed per binding.</strong>
 * <ul>
 *   <li>Adapter-lookup failures (backend doesn't host the kind) → record +
 *       skip binding (no migrations, no route).</li>
 *   <li>Type-compatibility check ({@code category.eventType() == kind.eventType()}
 *       and {@code category.queryResultType() == kind.recordType()}) → record +
 *       skip binding. Catches misrouted bindings at startup rather than at
 *       first dispatch.</li>
 *   <li>{@code ensureStore} failures → record + skip binding. ensureStore
 *       is a precondition for usable storage; without it no migrations
 *       should run and no reads will work.</li>
 *   <li>Migration failures → record, break out of the migration loop for
 *       this binding, AND do NOT register the route. Running 2→3 after
 *       1→2 failed is unsafe (later migrations assume earlier ones
 *       succeeded), and a route to un-migrated data silently dispatches
 *       reads against the wrong physical layout. Fail-closed is the only
 *       safe default.</li>
 * </ul>
 * The caller MAY still inspect {@link Result#failures} to decide whether
 * to abort startup entirely, but the bootstrap will not leave dangerous
 * routes installed regardless of the caller's policy.
 *
 * <p>Stateless; safe to call multiple times against the same backend (each
 * call appends new routes — ensureStore and migrations are idempotent by
 * contract).
 */
@ApiStatus.Internal
public final class V2BackendBootstrap {

    private V2BackendBootstrap() {}

    /**
     * One v2 wiring entry: which Category to register, which physical
     * store to target, and the Kind that defines the schema + migrations.
     * Held as a record so callers can build the input map declaratively.
     */
    public record Binding<K extends DataKind<?, ?>>(
        @NotNull StoreId storeId,
        @NotNull K kind) {
    }

    /**
     * Per-install summary. {@code failures} captures partial failures so
     * the caller can log + decide whether to abort. The caller-supplied
     * builder is mutated in-place with the successfully-wired routes;
     * {@link V2Routes.Builder#build()} is the caller's responsibility so
     * a multi-backend cutover can use one builder across many install
     * calls.
     */
    public record Result(@NotNull List<String> failures) {
        public boolean ok() { return failures.isEmpty(); }
    }

    /**
     * Run the bootstrap pass for one backend, appending routes into
     * {@code routesBuilder}.
     *
     * @param bindings       identity-keyed (Category → Binding) map. Caller is
     *                       responsible for using the SAME Category instances
     *                       downstream consumers will pass to
     *                       {@code DataStoreImpl.execute(Operation)} — v2
     *                       route lookup is identity-keyed.
     * @param backend        already-initialized backend (caller called
     *                       {@code backend.init(ctx)} before this).
     * @param migrationCtx   backend-specific MigrationContext (e.g.
     *                       {@code MongoMigrationContext} wrapping a
     *                       {@code MongoDatabase}).
     * @param routesBuilder  mutated in-place with the routes for this
     *                       backend's bindings.
     * @param logger         receives INFO per migration, WARNING on failure.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static @NotNull Result install(
        @NotNull Map<Category<?>, Binding<?>> bindings,
        @NotNull BackendV2 backend,
        @NotNull MigrationContext migrationCtx,
        @NotNull V2Routes.Builder routesBuilder,
        @NotNull Logger logger) {

        List<String> failures = new ArrayList<>();

        for (Map.Entry<Category<?>, Binding<?>> entry : bindings.entrySet()) {
            Category<?> category = entry.getKey();
            Binding<?> binding = entry.getValue();
            DataKind kind = binding.kind();
            StoreId storeId = binding.storeId();

            boolean eventOk = kind.eventType().isAssignableFrom(category.eventType());
            boolean recordOk = (kind instanceof KeyValueScoped<?, ?>)
                || category.queryResultType() == kind.recordType();
            if (!eventOk || !recordOk) {
                failures.add("type mismatch: category " + category.id() + " (event="
                    + category.eventType().getSimpleName() + ", queryResult="
                    + category.queryResultType().getSimpleName() + ") does not match kind "
                    + kind.name() + " (event=" + kind.eventType().getSimpleName()
                    + ", record=" + kind.recordType().getSimpleName() + ")");
                logger.warning(() -> "[v2-bootstrap] " + category.id() + " ↔ " + kind.name()
                    + " type mismatch — skipping binding");
                continue;
            }

            @SuppressWarnings({"deprecation", "removal"})
            boolean expectsTimeseriesLegacy = category.requiredCapabilities().contains(Capability.TIMESERIES_APPEND);
            boolean expectsTimeseriesV2 = category.requiredCapabilities().contains(Capability.KIND_EVENT_STREAM);
            boolean expectsTimeseries = expectsTimeseriesLegacy || expectsTimeseriesV2;
            boolean isTimeseries = kind instanceof EventStream<?, ?>;
            if (expectsTimeseries != isTimeseries) {
                failures.add("kind family mismatch: category " + category.id()
                    + (expectsTimeseries
                        ? " requires an EventStream kind (TIMESERIES_APPEND) but got "
                        : " requires a non-EventStream kind but got an EventStream: ")
                    + kind.name());
                logger.warning(() -> "[v2-bootstrap] " + category.id() + " ↔ " + kind.name()
                    + " kind-family mismatch (expectsTimeseries=" + expectsTimeseries
                    + ", isTimeseries=" + isTimeseries + ") — skipping binding");
                continue;
            }

            KindAdapter adapter;
            try {
                adapter = (KindAdapter) backend.adapterFor(kind).orElseThrow(
                    () -> new IllegalStateException("backend " + backend.id()
                        + " does not advertise an adapter for kind " + kind.name()));
            } catch (Exception e) {
                failures.add("adapterFor " + kind.name() + " on " + backend.id() + ": " + e.getMessage());
                logger.log(Level.WARNING,
                    "[v2-bootstrap] adapterFor " + kind.name() + " on " + backend.id() + " failed", e);
                continue;
            }

            try {
                adapter.ensureStore(storeId, kind);
            } catch (Exception e) {
                failures.add("ensureStore " + kind.name() + " on " + storeId + ": " + e.getMessage());
                logger.log(Level.WARNING,
                    "[v2-bootstrap] ensureStore " + kind.name() + " on " + storeId + " failed", e);

                continue;
            }

            List<Migration> migs = adapter.migrations(kind);
            logger.info(() -> "[v2-bootstrap] " + storeId + " (" + kind.name() + "): "
                + migs.size() + " migration(s)");
            boolean migrationFailed = false;
            for (Migration m : migs) {
                try {
                    logger.fine(() -> "[v2-bootstrap]   apply " + m.getClass().getSimpleName()
                        + " " + m.fromVersion() + " → " + m.toVersion());
                    m.apply(migrationCtx, storeId, kind);
                    logger.fine(() -> "[v2-bootstrap]   OK    " + m.getClass().getSimpleName());
                } catch (Exception e) {
                    failures.add("migration " + m.getClass().getSimpleName()
                        + " on " + storeId + ": " + e.getMessage());
                    logger.log(Level.WARNING,
                        "[v2-bootstrap] migration " + m.getClass().getSimpleName()
                            + " on " + storeId + " FAILED — stopping migration chain for binding,"
                            + " not registering route", e);

                    migrationFailed = true;
                    break;
                }
            }

            if (migrationFailed) {

                continue;
            }

            registerRoute(routesBuilder, category, storeId, kind, backend);
        }

        return new Result(failures);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerRoute(@NotNull V2Routes.Builder routes,
                                      @NotNull Category category,
                                      @NotNull StoreId storeId,
                                      @NotNull DataKind kind,
                                      @NotNull BackendV2 backend) {
        routes.register(category, storeId, kind, backend);
    }
}
