package ac.deneme.anticheat.manager.player.features.types;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.feature.FeatureState;
import ac.deneme.anticheat.player.DenemePlayer;

public class ForceStuckSpeedFeature implements DenemeFeature {

    @Override
    public String getName() {
        return "ForceStuckSpeed";
    }

    @Override
    public void setState(DenemePlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setForceStuckSpeed(true);
            case DISABLED -> player.setForceStuckSpeed(false);
            default -> player.setForceStuckSpeed(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(DenemePlayer player) {
        return player.isForceStuckSpeed();
    }

    @Override
    public boolean isEnabledInConfig(DenemePlayer player, ConfigManager config) {
        return config.getBooleanElse("force-stuck-speed", true);
    }

}
