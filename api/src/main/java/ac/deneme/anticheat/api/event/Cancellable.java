package ac.deneme.anticheat.api.event;

public interface Cancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
