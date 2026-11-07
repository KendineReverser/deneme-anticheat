package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.AbstractCheck;
import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.AbstractEventChannel;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class DenemeVerboseCheckEvent<CHANNEL extends EventChannel<?, ?>>
        extends DenemeCheckEvent<CHANNEL> {
    private Supplier<String> verboseSupplier = () -> "";

    /** Pool constructor — fields populated via {@link #init(DenemeUser, AbstractCheck, String)}. */
    protected DenemeVerboseCheckEvent() {
        super();
    }

    public DenemeVerboseCheckEvent(DenemeUser user, AbstractCheck check, String verbose) {
        super(user, check);
        setVerboseSupplier(VerboseSuppliers.constant(verbose));
    }

    public DenemeVerboseCheckEvent(DenemeUser user, AbstractCheck check, Supplier<String> verboseSupplier) {
        super(user, check);
        setVerboseSupplier(verboseSupplier);
    }

    @ApiStatus.Internal
    protected void init(DenemeUser user, AbstractCheck check, String verbose) {
        init(user, check, VerboseSuppliers.constant(verbose));
    }

    @ApiStatus.Internal
    protected void init(DenemeUser user, AbstractCheck check, Supplier<String> verboseSupplier) {
        super.init(user, check);
        setVerboseSupplier(verboseSupplier);
    }

    /** Returns the human verbose string, rendering it lazily on first use. */
    public @NotNull String getVerbose() {
        return verboseSupplier.get();
    }

    @ApiStatus.Internal
    final @NotNull Supplier<String> verboseSupplier() {
        return verboseSupplier;
    }

    private void setVerboseSupplier(Supplier<String> verboseSupplier) {
        this.verboseSupplier = VerboseSuppliers.memoize(verboseSupplier);
    }

    /**
     * Abstract-level verbose-check handler. Fires for every concrete
     * {@code DenemeVerboseCheckEvent} subtype — FlagEvent and
     * CommandExecuteEvent out of the box, plus any addon subtypes that
     * opt into bridging. Does not fire for
     * {@link CompletePredictionEvent}, which extends {@link DenemeCheckEvent}
     * directly and has no verbose field.
     */
    @FunctionalInterface
    public interface Handler {
        boolean onVerboseCheck(@NotNull DenemeUser user, @NotNull AbstractCheck check,
                               @NotNull String verbose, boolean currentlyCancelled);
    }

    @FunctionalInterface
    public interface SupplierHandler {
        boolean onVerboseCheck(@NotNull DenemeUser user, @NotNull AbstractCheck check,
                               @NotNull Supplier<String> verbose, boolean currentlyCancelled);
    }

    public static final class Channel extends AbstractEventChannel<DenemeVerboseCheckEvent<?>, SupplierHandler> {
        private static final AtomicBoolean STRING_HANDLER_WARNING = new AtomicBoolean();

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Channel() {
            super((Class<DenemeVerboseCheckEvent<?>>) (Class) DenemeVerboseCheckEvent.class, SupplierHandler.class);
        }

        public void onVerboseCheckSupplier(@NotNull DenemePlugin plugin, @NotNull SupplierHandler handler) {
            subscribeAbstract(handler, ListenerPriority.NORMAL, false, plugin);
        }

        public void onVerboseCheckSupplier(@NotNull DenemePlugin plugin, @NotNull SupplierHandler handler, int priority) {
            subscribeAbstract(handler, priority, false, plugin);
        }

        public void onVerboseCheckSupplier(@NotNull DenemePlugin plugin, @NotNull SupplierHandler handler, int priority, boolean ignoreCancelled) {
            subscribeAbstract(handler, priority, ignoreCancelled, plugin);
        }

        /**
         * @deprecated Prefer {@link #onVerboseCheckSupplier(DenemePlugin, SupplierHandler)}.
         */
        @Deprecated
        public void onVerboseCheck(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            warnStringHandler(plugin);
            onVerboseCheckSupplier(plugin, adapt(handler));
        }

        /**
         * @deprecated Prefer {@link #onVerboseCheckSupplier(DenemePlugin, SupplierHandler, int)}.
         */
        @Deprecated
        public void onVerboseCheck(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            warnStringHandler(plugin);
            onVerboseCheckSupplier(plugin, adapt(handler), priority);
        }

        /**
         * @deprecated Prefer {@link #onVerboseCheckSupplier(DenemePlugin, SupplierHandler, int, boolean)}.
         */
        @Deprecated
        public void onVerboseCheck(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority, boolean ignoreCancelled) {
            warnStringHandler(plugin);
            onVerboseCheckSupplier(plugin, adapt(handler), priority, ignoreCancelled);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onVerboseCheckSupplier(@NotNull Object pluginContext, @NotNull SupplierHandler handler) {
            subscribeAbstractResolving(pluginContext, handler, ListenerPriority.NORMAL, false);
        }

        /** @deprecated see {@link #onVerboseCheckSupplier(Object, SupplierHandler)}. */
        @Deprecated
        public void onVerboseCheckSupplier(@NotNull Object pluginContext, @NotNull SupplierHandler handler, int priority) {
            subscribeAbstractResolving(pluginContext, handler, priority, false);
        }

        /** @deprecated see {@link #onVerboseCheckSupplier(Object, SupplierHandler)}. */
        @Deprecated
        public void onVerboseCheckSupplier(@NotNull Object pluginContext, @NotNull SupplierHandler handler, int priority, boolean ignoreCancelled) {
            subscribeAbstractResolving(pluginContext, handler, priority, ignoreCancelled);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onVerboseCheck(@NotNull Object pluginContext, @NotNull Handler handler) {
            DenemePlugin plugin = resolvePlugin(pluginContext);
            onVerboseCheck(plugin, handler);
        }

        /** @deprecated see {@link #onVerboseCheck(Object, Handler)}. */
        @Deprecated
        public void onVerboseCheck(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            DenemePlugin plugin = resolvePlugin(pluginContext);
            onVerboseCheck(plugin, handler, priority);
        }

        /** @deprecated see {@link #onVerboseCheck(Object, Handler)}. */
        @Deprecated
        public void onVerboseCheck(@NotNull Object pluginContext, @NotNull Handler handler, int priority, boolean ignoreCancelled) {
            DenemePlugin plugin = resolvePlugin(pluginContext);
            onVerboseCheck(plugin, handler, priority, ignoreCancelled);
        }

        private static @NotNull SupplierHandler adapt(@NotNull Handler handler) {
            return (user, check, verbose, cancelled) -> handler.onVerboseCheck(user, check, verbose.get(), cancelled);
        }

        private static void warnStringHandler(@NotNull DenemePlugin plugin) {
            if (STRING_HANDLER_WARNING.compareAndSet(false, true)) {
                plugin.getLogger().warning("Deprecated Deneme verbose string listener registered; use the Supplier<String> verbose handler and call verbose.get() only when text is needed.");
            }
        }
    }
}
