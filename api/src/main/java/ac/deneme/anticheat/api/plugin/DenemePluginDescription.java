package ac.deneme.anticheat.api.plugin;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface DenemePluginDescription {
    String getVersion();

    String getDescription();

    public @NotNull Collection<String> getAuthors();
}
