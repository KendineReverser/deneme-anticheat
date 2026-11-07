package ac.deneme.anticheat.manager.player.features;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.config.ConfigManager;
import ac.deneme.anticheat.api.feature.FeatureManager;
import ac.deneme.anticheat.api.feature.FeatureState;
import ac.deneme.anticheat.manager.player.features.types.*;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.common.ConfigReloadObserver;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FeatureManagerImpl implements FeatureManager, ConfigReloadObserver {

    private static final Map<String, DenemeFeature> FEATURES;

    @Contract(pure = true)
    @Deprecated
    public static Map<String, DenemeFeature> getFEATURES() {
        return getFeatures();
    }

    @Contract(pure = true)
    public static Map<String, DenemeFeature> getFeatures() {
        return FEATURES;
    }

    static {
        FeatureBuilder builder = new FeatureBuilder();
        builder.register(new ExperimentalChecksFeature());
        builder.register(new ExemptElytraFeature());
        builder.register(new ForceStuckSpeedFeature());
        builder.register(new ForceSlowMovementFeature());
        FEATURES = builder.buildMap();
    }

    private final Map<String, FeatureState> states = new HashMap<>();

    private final DenemePlayer player;

    public FeatureManagerImpl(DenemePlayer player) {
        this.player = player;
        for (DenemeFeature value : FEATURES.values()) states.put(value.getName(), FeatureState.UNSET);
    }

    @Override
    public Collection<String> getFeatureKeys() {
        return ImmutableSet.copyOf(FEATURES.keySet());
    }

    @Override
    public @Nullable FeatureState getFeatureState(String key) {
        return states.get(key);
    }

    @Override
    public boolean isFeatureEnabled(String key) {
        DenemeFeature feature = FEATURES.get(key);
        if (feature == null) return false;
        return feature.isEnabled(player);
    }

    @Override
    public boolean setFeatureState(String key, FeatureState tristate) {
        DenemeFeature feature = FEATURES.get(key);
        if (feature == null) return false;
        states.put(key, tristate);
        return true;
    }

    @Override
    public void reload() {
        onReload(DenemeAPI.INSTANCE.getExternalAPI().getConfigManager());
    }

    @Override
    public void onReload(ConfigManager config) {
        for (Map.Entry<String, FeatureState> entry : states.entrySet()) {
            String key = entry.getKey();
            FeatureState state = entry.getValue();
            DenemeFeature feature = FEATURES.get(key);
            if (feature == null) continue;
            feature.setState(player, config, state);
        }
    }

}
