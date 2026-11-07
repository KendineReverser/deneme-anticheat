package ac.deneme.anticheat.api.plugin;

import java.io.File;
import java.util.logging.Logger;

public interface DenemePlugin {

    DenemePluginDescription getDescription();

    Logger getLogger();

    File getDataFolder();
}
