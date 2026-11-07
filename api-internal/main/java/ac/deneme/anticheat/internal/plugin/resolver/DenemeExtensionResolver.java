package ac.deneme.anticheat.internal.plugin.resolver;

import ac.deneme.anticheat.api.plugin.DenemePlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A functional interface responsible for attempting to resolve a generic context object
 * into a {@link DenemePlugin}.
 * <p>
 * Implementations of this are provided by the core DenemeAC platform module (e.g., for Bukkit, Fabric)
 * and registered with the central DenemeExtensionManager.
 */
@FunctionalInterface
public interface DenemeExtensionResolver {

    /**
     * Attempts to resolve the given context object into a DenemePlugin.
     *
     * @param context The context object to resolve (e.g., a Bukkit Plugin, a Plugin Class, a Fabric Mod).
     * @return A DenemePlugin if this resolver supports the context type, otherwise null.
     */
    @Nullable DenemePlugin resolve(@NotNull Object context);

}
