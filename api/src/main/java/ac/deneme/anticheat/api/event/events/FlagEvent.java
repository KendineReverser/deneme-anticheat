package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.AbstractCheck;
import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FlagEvent extends DenemeVerboseCheckEvent<FlagEvent.Channel> {

    /** Pool constructor — fields populated via {@link #init}. */
    public FlagEvent() {
        super();
    }

    public FlagEvent(DenemeUser user, AbstractCheck check, String verbose) {
        super(user, check, verbose);
    }

    public FlagEvent(DenemeUser user, AbstractCheck check, Supplier<String> verboseSupplier) {
        super(user, check, verboseSupplier);
    }

    @ApiStatus.Internal
    @Override
    public void init(DenemeUser user, AbstractCheck check, String verbose) {
        super.init(user, check, verbose);
    }

    @ApiStatus.Internal
    public void init(DenemeUser user, AbstractCheck check, Supplier<String> verboseSupplier) {
        super.init(user, check, verboseSupplier);
    }

    /**
     * Typed flag handler. Returns the new cancelled state — returning
     * {@code true} cancels, {@code false} leaves uncancelled. The last
     * parameter carries the cancelled state threaded through priority-ordered
     * dispatch; a handler registered with {@code ignoreCancelled = true}
     * still runs when a lower-priority handler already cancelled the event.
     *
     */
    @FunctionalInterface
    public interface Handler {
        boolean onFlag(@NotNull DenemeUser user, @NotNull AbstractCheck check,
                       @NotNull String verbose, boolean currentlyCancelled);
    }

    @FunctionalInterface
    public interface SupplierHandler {
        boolean onFlag(@NotNull DenemeUser user, @NotNull AbstractCheck check,
                       @NotNull Supplier<String> verbose, boolean currentlyCancelled);
    }

    public static final class Channel extends EventChannel<FlagEvent, SupplierHandler> {
        private static final AtomicBoolean STRING_HANDLER_WARNING = new AtomicBoolean();
        private final ThreadLocal<FlagEvent> legacyPool = ThreadLocal.withInitial(FlagEvent::new);

        public Channel() {
            super(FlagEvent.class, SupplierHandler.class);
        }

        public void onFlagSupplier(@NotNull DenemePlugin plugin, @NotNull SupplierHandler handler) {
            subscribe(handler, ListenerPriority.NORMAL, false, plugin, null);
        }

        public void onFlagSupplier(@NotNull DenemePlugin plugin, @NotNull SupplierHandler handler, int priority) {
            subscribe(handler, priority, false, plugin, null);
        }

        public void onFlagSupplier(@NotNull DenemePlugin plugin, @NotNull SupplierHandler handler, int priority, boolean ignoreCancelled) {
            subscribe(handler, priority, ignoreCancelled, plugin, null);
        }

        /**
         * @deprecated Prefer {@link #onFlagSupplier(DenemePlugin, SupplierHandler)}.
         */
        @Deprecated
        public void onFlag(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            warnStringHandler(plugin);
            onFlagSupplier(plugin, adapt(handler));
        }

        /**
         * @deprecated Prefer {@link #onFlagSupplier(DenemePlugin, SupplierHandler, int)}.
         */
        @Deprecated
        public void onFlag(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            warnStringHandler(plugin);
            onFlagSupplier(plugin, adapt(handler), priority);
        }

        /**
         * @deprecated Prefer {@link #onFlagSupplier(DenemePlugin, SupplierHandler, int, boolean)}.
         */
        @Deprecated
        public void onFlag(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority, boolean ignoreCancelled) {
            warnStringHandler(plugin);
            onFlagSupplier(plugin, adapt(handler), priority, ignoreCancelled);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onFlagSupplier(@NotNull Object pluginContext, @NotNull SupplierHandler handler) {
            onFlagSupplier(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onFlagSupplier(Object, SupplierHandler)}. */
        @Deprecated
        public void onFlagSupplier(@NotNull Object pluginContext, @NotNull SupplierHandler handler, int priority) {
            onFlagSupplier(resolvePlugin(pluginContext), handler, priority);
        }

        /** @deprecated see {@link #onFlagSupplier(Object, SupplierHandler)}. */
        @Deprecated
        public void onFlagSupplier(@NotNull Object pluginContext, @NotNull SupplierHandler handler, int priority, boolean ignoreCancelled) {
            onFlagSupplier(resolvePlugin(pluginContext), handler, priority, ignoreCancelled);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onFlag(@NotNull Object pluginContext, @NotNull Handler handler) {
            onFlag(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onFlag(Object, Handler)}. */
        @Deprecated
        public void onFlag(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            onFlag(resolvePlugin(pluginContext), handler, priority);
        }

        /** @deprecated see {@link #onFlag(Object, Handler)}. */
        @Deprecated
        public void onFlag(@NotNull Object pluginContext, @NotNull Handler handler, int priority, boolean ignoreCancelled) {
            onFlag(resolvePlugin(pluginContext), handler, priority, ignoreCancelled);
        }

        /** Dispatches the event. Returns the final cancelled state after all handlers run. */
        public boolean fire(@NotNull DenemeUser user, @NotNull AbstractCheck check, @NotNull String verbose) {
            return fire(user, check, VerboseSuppliers.constant(verbose));
        }

        /** Dispatches the event. Returns the final cancelled state after all handlers run. */
        public boolean fire(@NotNull DenemeUser user, @NotNull AbstractCheck check, @NotNull Supplier<String> verboseSupplier) {
            Entry<SupplierHandler>[] entries = entries();
            if (entries.length == 0) return false;

            Supplier<String> verbose = VerboseSuppliers.memoize(verboseSupplier);
            boolean cancelled = false;
            if (!hasLegacy()) {
                for (Entry<SupplierHandler> e : entries) {
                    if (cancelled && !e.ignoreCancelled) continue;
                    try {
                        cancelled = e.handler.onFlag(user, check, verbose, cancelled);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                return cancelled;
            }

            FlagEvent pooled = legacyPool.get();
            pooled.init(user, check, verbose);
            for (Entry<SupplierHandler> e : entries) {
                if (cancelled && !e.ignoreCancelled) continue;
                try {
                    if (e.legacyListener != null) {
                        pooled.setCancelled(cancelled);
                        e.<FlagEvent>legacyListenerAs().handle(pooled);
                        cancelled = pooled.isCancelled();
                    } else {
                        cancelled = e.handler.onFlag(user, check, verbose, cancelled);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            return cancelled;
        }

        @Override
        protected boolean dispatchTypedFromLegacy(@NotNull FlagEvent event, @NotNull SupplierHandler handler, boolean cancelled) {
            return handler.onFlag(event.getUser(), event.getCheck(), event.verboseSupplier(), cancelled);
        }

        /** Bridge from {@link DenemeCheckEvent.Handler} — used by the abstract channel when a check-level subscriber registers. */
        @ApiStatus.Internal
        public static @NotNull SupplierHandler bridgeFromCheck(@NotNull DenemeCheckEvent.Handler abstractHandler) {
            return (user, check, verbose, cancelled) -> abstractHandler.onCheck(user, check, cancelled);
        }

        /** Bridge from {@link DenemeVerboseCheckEvent.Handler}. */
        @ApiStatus.Internal
        public static @NotNull SupplierHandler bridgeFromVerboseCheck(@NotNull DenemeVerboseCheckEvent.SupplierHandler abstractHandler) {
            return (user, check, verbose, cancelled) -> abstractHandler.onVerboseCheck(user, check, verbose, cancelled);
        }

        /** Bridge from root-level {@link ac.deneme.anticheat.api.event.DenemeEvent.Handler} — observational only, cancelled is passed through unchanged. */
        @ApiStatus.Internal
        public static @NotNull SupplierHandler bridgeFromAny(@NotNull ac.deneme.anticheat.api.event.DenemeEvent.Handler abstractHandler) {
            return (user, check, verbose, cancelled) -> {
                abstractHandler.onAnyEvent(FlagEvent.class, cancelled);
                return cancelled;
            };
        }

        private static @NotNull SupplierHandler adapt(@NotNull Handler handler) {
            return (user, check, verbose, cancelled) -> handler.onFlag(user, check, verbose.get(), cancelled);
        }

        private static void warnStringHandler(@NotNull DenemePlugin plugin) {
            if (STRING_HANDLER_WARNING.compareAndSet(false, true)) {
                plugin.getLogger().warning("Deprecated Deneme flag string listener registered; use the Supplier<String> verbose handler and call verbose.get() only when text is needed.");
            }
        }
    }
}
