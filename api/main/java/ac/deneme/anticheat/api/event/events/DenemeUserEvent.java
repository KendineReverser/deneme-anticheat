package ac.deneme.anticheat.api.event.events;

import ac.deneme.anticheat.api.DenemeUser;

public interface DenemeUserEvent {
    DenemeUser getUser();
    default DenemeUser getPlayer() {
        return getUser();
    }
}

