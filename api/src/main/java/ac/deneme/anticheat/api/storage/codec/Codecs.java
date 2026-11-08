package ac.deneme.anticheat.api.storage.codec;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Codec registry / factory. Looks up or generates a {@link Codec} for a
 * {@link Persistent} record class. Thread-safe; results cached forever
 * inside the installed {@link Provider}.
 * <p>
 * On first call to {@link #of}, if no provider has been installed yet,
 * attempts to lazy-bootstrap {@code deneme-internal}'s
 * {@code MethodHandleCodecFactory} via reflection — keeps {@code deneme-api}
 * free of a build-time dep on {@code deneme-internal} while ensuring any
 * production deployment (which always ships deneme-internal) gets a working
 * provider without manual bootstrap wiring.
 * <p>
 * Test-only deployments without {@code deneme-internal} on the classpath
 * still get the original "no provider installed" error.
 */
@ApiStatus.Experimental
public final class Codecs {

    private static final Provider THROWING_DEFAULT = new Provider() {
        @Override
        public <R> @NotNull Codec<R> create(@NotNull Class<R> recordType) {
            throw new UnsupportedOperationException(
                "no codec provider installed yet; deneme-internal must be on the classpath, "
                    + "or call Codecs.install() at startup");
        }
    };

    private static volatile Provider provider = THROWING_DEFAULT;
    private static volatile boolean bootstrapTried = false;

    private Codecs() {}

    /** Look up or generate the codec for a persistent record class. */
    public static <R> @NotNull Codec<R> of(@NotNull Class<R> recordType) {
        Provider p = provider;
        if (p == THROWING_DEFAULT && !bootstrapTried) {
            tryBootstrapFromInternal();
            p = provider;
        }
        return p.create(recordType);
    }

    /** Install the provider. Called by {@code deneme-internal} at startup; idempotent. */
    public static void install(@NotNull Provider p) {
        provider = p;
        bootstrapTried = true;
    }

    /** Whether a non-default provider has been installed (introspection only). */
    public static boolean isInstalled() {
        return provider != THROWING_DEFAULT;
    }

    private static synchronized void tryBootstrapFromInternal() {
        if (bootstrapTried) return;
        bootstrapTried = true;
        try {
            Class<?> factory = Class.forName(
                "ac.deneme.anticheat.internal.storage.codec.MethodHandleCodecFactory");
            factory.getMethod("installAsDefaultProvider").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "failed to lazy-bootstrap MethodHandleCodecFactory; install() it explicitly", e);
        }
    }

    /** SPI for the actual generator implementation (reflective MethodHandle or, later, ASM). */
    public interface Provider {
        <R> @NotNull Codec<R> create(@NotNull Class<R> recordType);
    }
}
