package ac.deneme.anticheat.platform.fabric.mixins;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.platform.api.sender.Sender;
import ac.deneme.anticheat.platform.fabric.DenemeACFabricIntermediaryLoaderPlugin;
import ac.deneme.anticheat.platform.fabric.sender.FabricIntermediarySenderFactory;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(CommandSourceStack.class)
@Implements(@Interface(iface = Sender.class, prefix = "deneme$"))
abstract class FabricIntermediaryCommandSourceStackMixin {

    @Unique
    private CommandSourceStack denemeanticheat$self() {
        return (CommandSourceStack) (Object) this;
    }

    @Unique
    private FabricIntermediarySenderFactory denemeanticheat$factory() {
        return DenemeACFabricIntermediaryLoaderPlugin.LOADER.getFabricSenderFactory();
    }

    public UUID deneme$getUniqueId() {
        return denemeanticheat$factory().getUniqueId(denemeanticheat$self());
    }

    public String deneme$getName() {
        return denemeanticheat$factory().getName(denemeanticheat$self());
    }

    public void deneme$sendMessage(String message) {
        denemeanticheat$factory().sendMessage(denemeanticheat$self(), message);
    }

    public void deneme$sendMessage(Component message) {
        denemeanticheat$factory().sendMessage(denemeanticheat$self(), message);
    }

    public boolean deneme$hasPermission(String permission) {
        return denemeanticheat$factory().hasPermission(denemeanticheat$self(), permission);
    }

    public boolean deneme$hasPermission(String permission, boolean defaultIfUnset) {
        return denemeanticheat$factory().hasPermission(denemeanticheat$self(), permission, defaultIfUnset);
    }

    public void deneme$performCommand(String commandLine) {
        denemeanticheat$factory().performCommand(denemeanticheat$self(), commandLine);
    }

    public boolean deneme$isConsole() {
        return denemeanticheat$factory().isConsole(denemeanticheat$self());
    }

    public Object deneme$getNativeSender() {
        return denemeanticheat$self();
    }

    public @Nullable PlatformPlayer deneme$getPlatformPlayer() {
        return DenemeAPI.INSTANCE.getPlatformPlayerFactory().getFromUUID(deneme$getUniqueId());
    }
}
