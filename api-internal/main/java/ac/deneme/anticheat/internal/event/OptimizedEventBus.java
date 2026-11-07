package ac.deneme.anticheat.internal.event;

import ac.deneme.anticheat.api.event.EventBus;
import ac.deneme.anticheat.api.event.EventChannel;
import ac.deneme.anticheat.api.event.DenemeEvent;
import ac.deneme.anticheat.api.event.DenemeEventHandler;
import ac.deneme.anticheat.api.event.DenemeEventListener;
import ac.deneme.anticheat.api.event.events.CommandExecuteEvent;
import ac.deneme.anticheat.api.event.events.CompletePredictionEvent;
import ac.deneme.anticheat.api.event.events.FlagEvent;
import ac.deneme.anticheat.api.event.events.DenemeCheckEvent;
import ac.deneme.anticheat.api.event.events.DenemeJoinEvent;
import ac.deneme.anticheat.api.event.events.DenemePlayerSetbackEvent;
import ac.deneme.anticheat.api.event.events.DenemeQuitEvent;
import ac.deneme.anticheat.api.event.events.DenemeReloadEvent;
import ac.deneme.anticheat.api.event.events.DenemeSetbackEvent;
import ac.deneme.anticheat.api.event.events.DenemeTeleportEvent;
import ac.deneme.anticheat.api.event.events.DenemeTransactionReceivedEvent;
import ac.deneme.anticheat.api.event.events.DenemeTransactionSendEvent;
import ac.deneme.anticheat.api.event.events.DenemeVehicleSetbackEvent;
import ac.deneme.anticheat.api.event.events.DenemeVerboseCheckEvent;
import ac.deneme.anticheat.api.plugin.DenemePlugin;
import ac.deneme.anticheat.internal.plugin.resolver.DenemeExtensionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Channel-backed event bus implementation.
 *
 * <p>All Deneme built-in channels are registered explicitly at construction
 * time — no reflection, obfuscation-safe. Addons with their own events can
 * register via {@link #register(Class, EventChannel)}.
 *
 * <p>Legacy APIs ({@link #post(DenemeEvent)}, class-keyed {@code subscribe},
 * reflective {@code registerAnnotatedListeners}) are retained for source
 * compatibility with 1.2.4.0 callers. All three route through the same
 * channel objects as {@link #get(Class)} + the channel's typed
 * {@code on…(…)} methods — a single post therefore reaches typed handlers
 * as well as legacy listeners via each channel's
 * {@link EventChannel#dispatchLegacy(DenemeEvent)}.
 */
public class OptimizedEventBus implements EventBus {
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final DenemeExtensionManager extensionManager;
    private final ConcurrentMap<Class<?>, EventChannel<?, ?>> channels = new ConcurrentHashMap<>();

    /**
     * Side map of reflective/class-keyed registrations keyed by identity of
     * (pluginContext, listenerInstanceOrClass). Used by
     * {@link #unregisterListeners(Object, Object)} and friends to find the
     * specific entries to remove — Entry itself doesn't carry the originating
     * listener instance. Modifications are guarded by {@code this}.
     */
    private final Map<Object, Map<Object, List<Registration>>> instanceRegistrations = new IdentityHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public OptimizedEventBus(DenemeExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
        // Built-in channels — direct compile-time references.
        // Renaming any event's nested Channel breaks these lines immediately.

        // Concrete channels.
        FlagEvent.Channel flagCh                           = new FlagEvent.Channel();
        CommandExecuteEvent.Channel commandCh              = new CommandExecuteEvent.Channel();
        CompletePredictionEvent.Channel completeCh         = new CompletePredictionEvent.Channel();
        DenemeJoinEvent.Channel joinCh                       = new DenemeJoinEvent.Channel();
        DenemeQuitEvent.Channel quitCh                       = new DenemeQuitEvent.Channel();
        DenemeReloadEvent.Channel reloadCh                   = new DenemeReloadEvent.Channel();
        DenemeTeleportEvent.Channel teleportCh               = new DenemeTeleportEvent.Channel();
        DenemeTransactionSendEvent.Channel txSendCh          = new DenemeTransactionSendEvent.Channel();
        DenemeTransactionReceivedEvent.Channel txRecvCh      = new DenemeTransactionReceivedEvent.Channel();
        DenemePlayerSetbackEvent.Channel playerSetbackCh     = new DenemePlayerSetbackEvent.Channel();
        DenemeVehicleSetbackEvent.Channel vehicleSetbackCh   = new DenemeVehicleSetbackEvent.Channel();
        installChannel(FlagEvent.class,                    flagCh);
        installChannel(CommandExecuteEvent.class,          commandCh);
        installChannel(CompletePredictionEvent.class,      completeCh);
        installChannel(DenemeJoinEvent.class,                joinCh);
        installChannel(DenemeQuitEvent.class,                quitCh);
        installChannel(DenemeReloadEvent.class,              reloadCh);
        installChannel(DenemeTeleportEvent.class,            teleportCh);
        installChannel(DenemeTransactionSendEvent.class,     txSendCh);
        installChannel(DenemeTransactionReceivedEvent.class, txRecvCh);
        installChannel(DenemePlayerSetbackEvent.class,       playerSetbackCh);
        installChannel(DenemeVehicleSetbackEvent.class,      vehicleSetbackCh);

        // Abstract channels. Their Class<DenemeCheckEvent<?>> keys are raw-cast
        // because DenemeCheckEvent's CHANNEL type parameter is erased at .class.
        DenemeEvent.Channel anyCh                            = new DenemeEvent.Channel();
        DenemeCheckEvent.Channel checkCh                     = new DenemeCheckEvent.Channel();
        DenemeVerboseCheckEvent.Channel verboseCheckCh       = new DenemeVerboseCheckEvent.Channel();
        DenemeSetbackEvent.Channel setbackCh                 = new DenemeSetbackEvent.Channel();
        installAbstractChannel(DenemeEvent.class,             anyCh);
        installAbstractChannel(DenemeCheckEvent.class,        checkCh);
        installAbstractChannel(DenemeVerboseCheckEvent.class, verboseCheckCh);
        installAbstractChannel(DenemeSetbackEvent.class,      setbackCh);

        // Bridge wiring: every concrete subtype registers with every abstract
        // parent it can bridge to. Order matters only when abstract subscribes
        // land before registerSubtype — in that case registerSubtype walks the
        // subscriber list. We wire after installing both channels, so at
        // construction time the subscriber list is empty and the install is
        // just bookkeeping.
        checkCh.registerSubtype(FlagEvent.class,                flagCh,     FlagEvent.Channel::bridgeFromCheck);
        checkCh.registerSubtype(CommandExecuteEvent.class,      commandCh,  CommandExecuteEvent.Channel::bridgeFromCheck);
        checkCh.registerSubtype(CompletePredictionEvent.class,  completeCh, CompletePredictionEvent.Channel::bridgeFromCheck);

        verboseCheckCh.registerSubtype(FlagEvent.class,            flagCh,    FlagEvent.Channel::bridgeFromVerboseCheck);
        verboseCheckCh.registerSubtype(CommandExecuteEvent.class,  commandCh, CommandExecuteEvent.Channel::bridgeFromVerboseCheck);

        setbackCh.registerSubtype(DenemePlayerSetbackEvent.class,   playerSetbackCh,  DenemePlayerSetbackEvent.Channel::bridgeFromSetback);
        setbackCh.registerSubtype(DenemeVehicleSetbackEvent.class,  vehicleSetbackCh, DenemeVehicleSetbackEvent.Channel::bridgeFromSetback);

        anyCh.registerSubtype(FlagEvent.class,                    flagCh,           FlagEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(CommandExecuteEvent.class,          commandCh,        CommandExecuteEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(CompletePredictionEvent.class,      completeCh,       CompletePredictionEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(DenemeJoinEvent.class,                joinCh,           DenemeJoinEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(DenemeQuitEvent.class,                quitCh,           DenemeQuitEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(DenemeReloadEvent.class,              reloadCh,         DenemeReloadEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(DenemeTeleportEvent.class,            teleportCh,       DenemeTeleportEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(DenemeTransactionSendEvent.class,     txSendCh,         DenemeTransactionSendEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(DenemeTransactionReceivedEvent.class, txRecvCh,         DenemeTransactionReceivedEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(DenemePlayerSetbackEvent.class,       playerSetbackCh,  DenemePlayerSetbackEvent.Channel::bridgeFromAny);
        anyCh.registerSubtype(DenemeVehicleSetbackEvent.class,      vehicleSetbackCh, DenemeVehicleSetbackEvent.Channel::bridgeFromAny);
    }

    private <E extends DenemeEvent<C>, C extends EventChannel<? extends E, ?>> void installChannel(Class<E> eventClass, C channel) {
        channel.setPluginResolver(extensionManager::getPlugin);
        channels.put(eventClass, channel);
    }

    /**
     * Abstract-channel variant of {@link #installChannel}. Abstract events
     * (DenemeEvent, DenemeCheckEvent, DenemeVerboseCheckEvent) are generic on
     * {@code CHANNEL}, which makes the tight
     * {@code E extends DenemeEvent<C>, C extends EventChannel<? extends E, ?>}
     * constraint unsolvable when you pass the raw parent class with a
     * concrete abstract-channel. Loosen the E binding here; the abstract
     * channels are exhaustively listed in the constructor, so the looser
     * type safety is contained.
     */
    private <E, C extends EventChannel<?, ?>> void installAbstractChannel(Class<E> eventClass, C channel) {
        channel.setPluginResolver(extensionManager::getPlugin);
        channels.put(eventClass, channel);
    }

    // ───── Typed channel API ───────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public <E extends DenemeEvent<C>, C extends EventChannel<? extends E, ?>> @NotNull C get(@NotNull Class<E> eventClass) {
        EventChannel<?, ?> ch = channels.get(eventClass);
        if (ch == null) {
            throw new IllegalArgumentException("No EventChannel registered for " + eventClass.getName()
                    + " — addons must call EventBus.register(Class, Channel) before first use.");
        }
        return (C) ch;
    }

    @Override
    public <E extends DenemeEvent<C>, C extends EventChannel<? extends E, ?>> void register(@NotNull Class<E> eventClass, @NotNull C channel) {
        channel.setPluginResolver(extensionManager::getPlugin);
        channels.put(eventClass, channel);
    }

    // ───── Reflective annotated-method registration ────────────────────────

    @Override
    public void registerAnnotatedListeners(@NotNull Object pluginContext, @NotNull Object listener) {
        DenemePlugin plugin = extensionManager.getPlugin(pluginContext);
        registerAnnotatedListeners(plugin, listener);
    }

    @Override
    public void registerAnnotatedListeners(@NotNull DenemePlugin plugin, @NotNull Object listener) {
        registerMethods(plugin, listener, listener.getClass(), listener);
    }

    @Override
    public void registerStaticAnnotatedListeners(@NotNull Object pluginContext, @NotNull Class<?> clazz) {
        DenemePlugin plugin = extensionManager.getPlugin(pluginContext);
        registerStaticAnnotatedListeners(plugin, clazz);
    }

    @Override
    public void registerStaticAnnotatedListeners(@NotNull DenemePlugin plugin, @NotNull Class<?> clazz) {
        registerMethods(plugin, null, clazz, clazz);
    }

    private void registerMethods(@NotNull DenemePlugin plugin, @Nullable Object instance, @NotNull Class<?> clazz, @NotNull Object registrationKey) {
        for (Method method : clazz.getDeclaredMethods()) {
            DenemeEventHandler annotation = method.getAnnotation(DenemeEventHandler.class);
            if (annotation == null || method.getParameterCount() != 1) continue;
            Class<?> paramType = method.getParameterTypes()[0];
            if (!DenemeEvent.class.isAssignableFrom(paramType)) continue;
            if (instance == null && !Modifier.isStatic(method.getModifiers())) continue;

            @SuppressWarnings("unchecked")
            Class<? extends DenemeEvent<?>> eventClass = (Class<? extends DenemeEvent<?>>) (Class<?>) paramType;
            EventChannel<?, ?> channel = channels.get(eventClass);
            if (channel == null) continue; // no channel for this event — ignore silently (matches old behavior)

            try {
                method.setAccessible(true);
                MethodHandle handle = lookup.unreflect(method);
                DenemeEventListener<DenemeEvent<?>> listener = buildListener(instance, handle);
                subscribeLegacyInternal(plugin, channel, eventClass, listener,
                        annotation.priority(), annotation.ignoreCancelled(), method.getDeclaringClass(), registrationKey);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /** Wrap a reflected method + instance as a DenemeEventListener. */
    private static DenemeEventListener<DenemeEvent<?>> buildListener(@Nullable Object instance, MethodHandle handle) {
        if (instance != null) {
            return event -> {
                try {
                    handle.invoke(instance, event);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to invoke listener for " + event.getClass().getName(), t);
                }
            };
        }
        return event -> {
            try {
                handle.invoke(event);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to invoke listener for " + event.getClass().getName(), t);
            }
        };
    }

    // ───── Class-keyed explicit subscribe ──────────────────────────────────

    @Override
    public <T extends DenemeEvent<?>> void subscribe(@NotNull Object pluginContext, @NotNull Class<T> eventType, @NotNull DenemeEventListener<T> listener, int priority, boolean ignoreCancelled, @NotNull Class<?> declaringClass) {
        DenemePlugin plugin = extensionManager.getPlugin(pluginContext);
        subscribe(plugin, eventType, listener, priority, ignoreCancelled, declaringClass);
    }

    @Override
    public <T extends DenemeEvent<?>> void subscribe(@NotNull DenemePlugin plugin, @NotNull Class<T> eventType, @NotNull DenemeEventListener<T> listener, int priority, boolean ignoreCancelled, @NotNull Class<?> declaringClass) {
        EventChannel<?, ?> channel = channels.get(eventType);
        if (channel == null) {
            throw new IllegalArgumentException("No EventChannel registered for " + eventType.getName());
        }
        subscribeLegacyInternal(plugin, channel, eventType, listener, priority, ignoreCancelled, declaringClass, listener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void subscribeLegacyInternal(
            @NotNull DenemePlugin plugin,
            @NotNull EventChannel<?, ?> channel,
            @NotNull Class<? extends DenemeEvent<?>> eventClass,
            @NotNull DenemeEventListener<?> listener,
            int priority, boolean ignoreCancelled,
            @NotNull Class<?> declaringClass,
            @NotNull Object registrationKey) {
        ((EventChannel) channel).subscribeLegacy(listener, eventClass, priority, ignoreCancelled, plugin, declaringClass);
        synchronized (this) {
            Map<Object, List<Registration>> byPlugin = instanceRegistrations.computeIfAbsent(plugin, p -> new IdentityHashMap<>());
            byPlugin.computeIfAbsent(registrationKey, k -> new ArrayList<>()).add(new Registration(channel, listener));
        }
    }

    // ───── Unregister ──────────────────────────────────────────────────────

    @Override
    public void unregisterListeners(@NotNull Object pluginContext, @NotNull Object listener) {
        unregisterListeners(extensionManager.getPlugin(pluginContext), listener);
    }

    @Override
    public void unregisterListeners(@NotNull DenemePlugin plugin, @NotNull Object listener) {
        removeInstanceRegistrations(plugin, listener);
    }

    @Override
    public void unregisterStaticListeners(@NotNull Object pluginContext, @NotNull Class<?> clazz) {
        unregisterStaticListeners(extensionManager.getPlugin(pluginContext), clazz);
    }

    @Override
    public void unregisterStaticListeners(@NotNull DenemePlugin plugin, @NotNull Class<?> clazz) {
        removeInstanceRegistrations(plugin, clazz);
    }

    @Override
    public void unregisterAllListeners(@NotNull Object pluginContext) {
        unregisterAllListeners(extensionManager.getPlugin(pluginContext));
    }

    @Override
    public void unregisterAllListeners(@NotNull DenemePlugin plugin) {
        List<Registration> all = new ArrayList<>();
        synchronized (this) {
            Map<Object, List<Registration>> byPlugin = instanceRegistrations.remove(plugin);
            if (byPlugin != null) {
                for (List<Registration> regs : byPlugin.values()) all.addAll(regs);
            }
        }
        for (Registration r : all) r.channel.unsubscribeLegacy(r.listener);
        for (EventChannel<?, ?> ch : channels.values()) ch.unsubscribeAllFromPlugin(plugin);
    }

    @Override
    public void unregisterListener(@NotNull Object pluginContext, @NotNull DenemeEventListener<?> listener) {
        unregisterListener(extensionManager.getPlugin(pluginContext), listener);
    }

    @Override
    public void unregisterListener(@NotNull DenemePlugin plugin, @NotNull DenemeEventListener<?> listener) {
        for (EventChannel<?, ?> ch : channels.values()) ch.unsubscribeLegacy(listener);
        synchronized (this) {
            Map<Object, List<Registration>> byPlugin = instanceRegistrations.get(plugin);
            if (byPlugin != null) {
                for (List<Registration> regs : byPlugin.values()) {
                    regs.removeIf(r -> r.listener == listener);
                }
            }
        }
    }

    private void removeInstanceRegistrations(@NotNull DenemePlugin plugin, @NotNull Object key) {
        List<Registration> toRemove;
        synchronized (this) {
            Map<Object, List<Registration>> byPlugin = instanceRegistrations.get(plugin);
            if (byPlugin == null) return;
            List<Registration> regs = byPlugin.remove(key);
            if (regs == null) return;
            toRemove = regs;
        }
        for (Registration r : toRemove) r.channel.unsubscribeLegacy(r.listener);
    }

    // ───── Legacy post ─────────────────────────────────────────────────────

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void post(@NotNull DenemeEvent<?> event) {
        // Route to the concrete-class channel. Walks the superclass chain so a
        // hypothetical abstract-event subscriber (via addon-registered channel)
        // still receives the event.
        Class<?> c = event.getClass();
        while (c != null && DenemeEvent.class.isAssignableFrom(c)) {
            EventChannel<?, ?> ch = channels.get(c);
            if (ch != null) {
                ((EventChannel) ch).dispatchLegacy(event);
            }
            c = c.getSuperclass();
        }
    }

    /** Reflective-registration bookkeeping entry. */
    private static final class Registration {
        final EventChannel<?, ?> channel;
        final DenemeEventListener<?> listener;

        Registration(EventChannel<?, ?> channel, DenemeEventListener<?> listener) {
            this.channel = channel;
            this.listener = listener;
        }
    }
}
