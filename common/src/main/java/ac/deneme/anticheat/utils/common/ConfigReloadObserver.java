package ac.deneme.anticheat.utils.common;


import ac.deneme.anticheat.api.config.ConfigManager;

public interface ConfigReloadObserver {

    void onReload(ConfigManager config);

}
