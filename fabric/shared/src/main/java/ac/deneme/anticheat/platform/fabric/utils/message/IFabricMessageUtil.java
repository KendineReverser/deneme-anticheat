package ac.deneme.anticheat.platform.fabric.utils.message;

import ac.deneme.anticheat.platform.api.sender.Sender;

public interface IFabricMessageUtil {
    Object textLiteral(String message);

    void sendMessage(Sender target, Object message, boolean overlay);
}
