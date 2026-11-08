package ac.deneme.anticheat.utils.data;

import ac.deneme.anticheat.player.DenemePlayer;

public class LastInstance {
    private int lastInstance = 100;

    public LastInstance(DenemePlayer player) {
        player.lastInstanceManager.addInstance(this);
    }

    public boolean hasOccurredSince(int time) {
        return lastInstance <= time;
    }

    public void reset() {
        lastInstance = 0;
    }

    public void tick() {

        if (lastInstance == Integer.MAX_VALUE) lastInstance = 100;
        lastInstance++;
    }
}
