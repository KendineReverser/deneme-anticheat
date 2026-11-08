package ac.deneme.anticheat.manager.player.features.types;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.feature.FeatureState;
import ac.deneme.anticheat.player.DenemePlayer;

public class ForceSlowMovementFeature implements DenemeFeature {

    @Override
    public String getName() {
        return "ForceSlowMovement";
    }

    @Override
    public void setState(DenemePlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setForceSlowMovement(true);
            case DISABLED -> player.setForceSlowMovement(false);
            default -> player.setForceSlowMovement(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(DenemePlayer player) {
        return player.isForceSlowMovement();
    }

    @Override
    public boolean isEnabledInConfig(DenemePlayer player, ConfigManager config) {
        return config.getBooleanElse("force-slow-movement", true);
    }

}
