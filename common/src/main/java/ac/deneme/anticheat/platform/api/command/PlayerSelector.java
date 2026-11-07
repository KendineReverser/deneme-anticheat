package ac.deneme.anticheat.platform.api.command;

import ac.deneme.anticheat.platform.api.sender.Sender;

import java.util.Collection;

public interface PlayerSelector {
    boolean isSingle();

    Sender getSinglePlayer();

    Collection<Sender> getPlayers();

    String inputString();
}
