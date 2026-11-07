package ac.deneme.anticheat.manager.player.features.types;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.feature.FeatureState;
import ac.deneme.anticheat.player.DenemePlayer;

public class ExperimentalChecksFeature implements DenemeFeature {

    @Override
    public String getName() {
        return "ExperimentalChecks";
    }

    @Override
    public void setState(DenemePlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setExperimentalChecks(true);
            case DISABLED -> player.setExperimentalChecks(false);
            default -> player.setExperimentalChecks(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(DenemePlayer player) {
        return player.isExperimentalChecks();
    }

    @Override
    public boolean isEnabledInConfig(DenemePlayer player, ConfigManager config) {
        return config.getBooleanElse("experimental-checks", false);
    }

}
