package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.AbstractCheck;
import ac.deneme.anticheat.api.DenemeUser;
import ac.deneme.anticheat.api.event.AbstractEventChannel;
import ac.deneme.anticheat.api.event.Cancellable;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.DenemeEvent;
import ac.deneme.anticheat.api.event.ListenerPriority;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public abstract class DenemeCheckEvent<CHANNEL extends EventChannel<?, ?>>
        extends DenemeEvent<CHANNEL> implements DenemeUserEvent, Cancellable {
    private DenemeUser user;
    @Getter
    protected AbstractCheck check;
    private boolean cancelled;

    /** Pool constructor — fields populated via {@link #init(DenemeUser, AbstractCheck)}. */
    protected DenemeCheckEvent() {
        super(true); // Async
    }

    public DenemeCheckEvent(DenemeUser user, AbstractCheck check) {
        super(true); // Async
        this.user = user;
        this.check = check;
    }

    @ApiStatus.Internal
    protected void init(DenemeUser user, AbstractCheck check) {
        resetForReuse();
        this.user = user;
        this.check = check;
        this.cancelled = false;
    }

    @Override
    public DenemeUser getUser() {
        return user;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public double getViolations() {
        return check.getViolations();
    }

    public boolean isSetback() {
        return check.getViolations() > check.getSetbackVL();
    }

    /**
     * Abstract-level check handler. Fires for every concrete
     * {@code DenemeCheckEvent} subtype (FlagEvent, CompletePredictionEvent,
     * CommandExecuteEvent, and any addon subtypes that opt into bridging).
     *
     * <p>Returns the new cancelled state — the value is threaded back into
     * the priority-ordered dispatch loop of whichever concrete subtype
     * fired, so a low-priority abstract subscriber can cancel and
     * higher-priority direct subscribers to the concrete event see the
     * cancellation just like any other priority-ordered handler.
     */
    @FunctionalInterface
    public interface Handler {
        boolean onCheck(@NotNull DenemeUser user, @NotNull AbstractCheck check, boolean currentlyCancelled);
    }

    public static final class Channel extends AbstractEventChannel<DenemeCheckEvent<?>, Handler> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Channel() {
            super((Class<DenemeCheckEvent<?>>) (Class) DenemeCheckEvent.class, Handler.class);
        }

        public void onCheck(@NotNull DenemePlugin plugin, @NotNull Handler handler) {
            subscribeAbstract(handler, ListenerPriority.NORMAL, false, plugin);
        }

        public void onCheck(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority) {
            subscribeAbstract(handler, priority, false, plugin);
        }

        public void onCheck(@NotNull DenemePlugin plugin, @NotNull Handler handler, int priority, boolean ignoreCancelled) {
            subscribeAbstract(handler, priority, ignoreCancelled, plugin);
        }

        /** @deprecated resolve your context once at plugin enable — {@code api.getDenemePlugin(this)} — and call the {@link DenemePlugin}-taking overload. */
        @Deprecated
        public void onCheck(@NotNull Object pluginContext, @NotNull Handler handler) {
            subscribeAbstractResolving(pluginContext, handler, ListenerPriority.NORMAL, false);
        }

        /** @deprecated see {@link #onCheck(Object, Handler)}. */
        @Deprecated
        public void onCheck(@NotNull Object pluginContext, @NotNull Handler handler, int priority) {
            subscribeAbstractResolving(pluginContext, handler, priority, false);
        }

        /** @deprecated see {@link #onCheck(Object, Handler)}. */
        @Deprecated
        public void onCheck(@NotNull Object pluginContext, @NotNull Handler handler, int priority, boolean ignoreCancelled) {
            subscribeAbstractResolving(pluginContext, handler, priority, ignoreCancelled);
        }
    }
}
