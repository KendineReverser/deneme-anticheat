package ac.deneme.anticheat.manager.player.features.types;

import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.feature.FeatureState;
import ac.deneme.anticheat.player.DenemePlayer;

public class ExemptElytraFeature implements DenemeFeature {

    @Override
    public String getName() {
        return "ExemptElytra";
    }

    @Override
    public void setState(DenemePlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setExemptElytra(true);
            case DISABLED -> player.setExemptElytra(false);
            default -> player.setExemptElytra(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(DenemePlayer player) {
        return player.isExemptElytra();
    }

    @Override
    public boolean isEnabledInConfig(DenemePlayer player, ConfigManager config) {
        return config.getBooleanElse("exempt-elytra", false);
    }

}
