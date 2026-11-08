package ac.deneme.anticheat.api.event;

@FunctionalInterface
public interface DenemeEventListener<T extends DenemeEvent<?>> {
    void handle(T event) throws Exception;
}
