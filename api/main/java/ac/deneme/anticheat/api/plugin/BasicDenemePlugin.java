package ac.deneme.anticheat.api.plugin;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;

public class BasicDenemePlugin implements DenemePlugin {
    private final Logger logger;
    private final File dataFolder;
    private final BasicDenemePluginDescription description;

    public BasicDenemePlugin(Logger logger, File dataFolder, String version, String description, Collection<String> authors) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.description = new BasicDenemePluginDescription(version, description, authors);
    }

    @Override
    public DenemePluginDescription getDescription() {
        return description;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    private static class BasicDenemePluginDescription implements DenemePluginDescription {

        private final String version;
        private final String description;
        private final Collection<String> authors;

        public BasicDenemePluginDescription(String version, String description, Collection<String> authors) {
            this.version = version;
            this.description = description;
            this.authors = authors;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public @NotNull Collection<String> getAuthors() {
            return authors;
        }
    }
}
