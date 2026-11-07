package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.DenemeEvent;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when Deneme sends a transaction packet to the client.
 *
 * <p>Plugins can use this event to track transaction ids issued by Deneme and
 * correlate them with the matching {@link DenemeTransactionReceivedEvent} once a
 * response is received.
 *
 * <p>Fires on the Netty thread associated with the user. Observational, not
 * cancellable.
 */
public final class DenemeTransactionSendEvent extends DenemeEvent<DenemeTransactionSendEvent.Channel> {
    private DenemeTransactionSendEvent() {

    }

    @FunctionalInterface
    public interface Handler {
        void onTransactionSend(@NotNull DenemeUser user, int transactionId, long timestamp);
    }

    public static final class Channel extends EventChannel<DenemeTransactionSendEvent, Handler> {
        public Channel() {
            super(DenemeTransactionSendEvent.class, Handler.class);
        }

        public void onTransactionSend(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribe(handler, ListenerPriority.NORMAL, false, plugin, null);
        }

        public void onTransactionSend(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribe(handler, priority, false, plugin, null);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onTransactionSend(@NotNull Object pluginContext, @NotNull Handler handler) {
            onTransactionSend(resolvePlugin(pluginContext), handler);
        }

        /** @deprecated see {@link #onTransactionSend(Object, Handler)}. */
        @Deprecated
        public void onTransactionSend(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            onTransactionSend(resolvePlugin(pluginContext), handler, priority);
        }

        public void fire(@NotNull DenemeUser user, int transactionId, long timestamp) {
            Entry<Handler>[] entries = entries();
            for (Entry<Handler> e : entries) {
                try {
                    e.handler.onTransactionSend(user, transactionId, timestamp);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        protected boolean dispatchTypedFromLegacy(@NotNull DenemeTransactionSendEvent event, @NotNull Handler handler, boolean cancelled) {
            throw new UnsupportedOperationException("DenemeTransactionSendEvent has no legacy representation");
        }

        @org.jetbrains.annotations.ApiStatus.Internal
        public static @NotNull Handler bridgeFromAny(@NotNull ac.deneme.anticheat.api.event.DenemeEvent.Handler abstractHandler) {
            return (user, id, ts) -> abstractHandler.onAnyEvent(DenemeTransactionSendEvent.class, false);
        }
    }
}
