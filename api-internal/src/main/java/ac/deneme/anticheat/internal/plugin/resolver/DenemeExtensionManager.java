package ac.deneme.anticheat.internal.plugin.resolver;

import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the resolution of platform-specific plugin contexts into the universal {@link DenemePlugin} interface.
 * <p>
 * This class is the bridge that allows the API to remain platform-agnostic while still supporting
 * platform-specific objects for lifecycle management.
 */
@ApiStatus.Internal
public final class DenemeExtensionManager {
    private final List<DenemeExtensionResolver> resolvers = new CopyOnWriteArrayList<>();
    private ResolutionFailureHandler failureHandler = failedContext ->
            new IllegalArgumentException("Unable to resolve plugin context for type: " + failedContext.getClass().getName() +
                    ". Ensure you are passing a valid platform plugin instance or a pre-existing DenemePlugin.");

    /**
     * Sets a custom handler for creating exceptions when plugin context resolution fails.
     * This allows platform-specific implementations to provide more descriptive error messages.
     *
     * @param failureHandler The handler to use.
     */
    public void setFailureHandler(@NotNull ResolutionFailureHandler failureHandler) {
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler cannot be null");
    }

    /**
     * Registers a new resolver. This is intended to be called by the core DenemeAC implementation
     * during its startup phase.
     *
     * @param resolver The resolver to add.
     */
    public void registerResolver(@NotNull DenemeExtensionResolver resolver) {
        Objects.requireNonNull(resolver, "resolver cannot be null");
        this.resolvers.add(resolver);
    }

    /**
     * Resolves a context object into a DenemePlugin by trying all registered resolvers in order.
     * <p>
     * It also has built-in logic to handle being passed a {@link DenemePlugin} instance directly.
     *
     * @param context The context object to resolve.
     * @return The resolved DenemePlugin.
     * @throws IllegalArgumentException if no registered resolver can handle the provided context type.
     */
    @NotNull
    public DenemePlugin getPlugin(@NotNull Object context) {
        Objects.requireNonNull(context, "context cannot be null");

        if (context instanceof DenemePlugin) {
            return (DenemePlugin) context;
        }

        for (DenemeExtensionResolver resolver : resolvers) {
            DenemePlugin resolved = resolver.resolve(context);
            if (resolved != null) {
                return resolved;
            }
        }

        throw this.failureHandler.createExceptionFor(context);
    }
}
