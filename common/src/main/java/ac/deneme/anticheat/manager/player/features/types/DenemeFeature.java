package ac.deneme.anticheat.manager.player.features.types;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.feature.FeatureState;
import ac.deneme.anticheat.player.DenemePlayer;

public interface DenemeFeature {
    String getName();

    void setState(DenemePlayer player, ConfigManager config, FeatureState state);

    boolean isEnabled(DenemePlayer player);

    boolean isEnabledInConfig(DenemePlayer player, ConfigManager config);
}
