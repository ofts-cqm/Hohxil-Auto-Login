package net.ofts.hohxilAutoLogin.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.ofts.hohxilAutoLogin.client.AutoLoginConfig;
import net.ofts.hohxilAutoLogin.client.AutomaticChooser;
import net.ofts.hohxilAutoLogin.client.HohxilAutoLoginClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof HandledScreen<?> handledScreen) {
            AutomaticChooser.choose(handledScreen);
        } else if (screen instanceof DisconnectedScreen disconnected) {
            if (AutoLoginConfig.get().matchBlacklist(disconnected.getTitle().getString())) return;
            HohxilAutoLoginClient.reconnect(MinecraftClient.getInstance());
        }
    }
}