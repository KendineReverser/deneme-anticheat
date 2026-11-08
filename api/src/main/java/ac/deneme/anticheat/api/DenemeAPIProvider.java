package ac.deneme.anticheat.api;

import java.util.concurrent.CompletableFuture;

public final class DenemeAPIProvider {
    private static DenemeAbstractAPI instance;
    private static final CompletableFuture<DenemeAbstractAPI> futureInstance = new CompletableFuture<>();

    private DenemeAPIProvider() {

    }

    /**
     * Initializes the DenemeAPI instance during mod loading.
     * This method should only be called once by the mod initializer.
     *
     * @param api The DenemeAbstractAPI instance to initialize.
     * @throws IllegalStateException If the API is already initialized.
     */
    public static void init(DenemeAbstractAPI api) {
        if (instance != null || futureInstance.isDone()) {
            throw new IllegalStateException("DenemeAPI is already initialized");
        }
        instance = api;
        futureInstance.complete(api);
    }

    /**
     * Gets the DenemeAPI instance synchronously.
     *
     * @return The DenemeAbstractAPI instance.
     * @throws IllegalStateException If the API is not loaded.
     */
    public static DenemeAbstractAPI get() {
        if (instance == null) {
            throw new IllegalStateException("DenemeAPI is not loaded. Ensure the Deneme mod is installed and initialized.");
        }
        return instance;
    }

    /**
     * Gets the DenemeAPI instance asynchronously.
     * The returned CompletableFuture will complete when the DenemeAPI instance is available.
     * If the API is already loaded, the future will complete immediately.
     * If the API fails to load (e.g., the mod is not installed), the future will complete exceptionally.
     *
     * @return A CompletableFuture that completes with the DenemeAbstractAPI instance.
     */
    public static CompletableFuture<DenemeAbstractAPI> getAsync() {
        if (instance != null) {

            return CompletableFuture.completedFuture(instance);
        }
        return futureInstance;
    }
}
