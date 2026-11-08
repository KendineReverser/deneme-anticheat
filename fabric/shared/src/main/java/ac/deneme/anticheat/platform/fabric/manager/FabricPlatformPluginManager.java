package ac.deneme.anticheat.platform.fabric.manager;

import ac.deneme.anticheat.platform.api.PlatformPlugin;
import ac.deneme.anticheat.platform.api.manager.PlatformPluginManager;
import ac.deneme.anticheat.platform.fabric.FabricPlatformPlugin;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Collection;
import java.util.Optional;

public class FabricPlatformPluginManager implements PlatformPluginManager {

    @Override
    public PlatformPlugin[] getPlugins() {

        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        PlatformPlugin[] plugins = new PlatformPlugin[mods.size()];
        int i = 0;
        for (ModContainer mod : mods) {
            plugins[i++] = new FabricPlatformPlugin(mod);
        }
        return plugins;
    }

    @Override
    public PlatformPlugin getPlugin(String pluginName) {

        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer(pluginName);
        return mod.map(FabricPlatformPlugin::new).orElse(null);
    }
}
