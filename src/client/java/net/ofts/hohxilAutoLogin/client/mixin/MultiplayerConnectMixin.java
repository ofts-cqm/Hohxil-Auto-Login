package net.ofts.hohxilAutoLogin.client.mixin;

import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.ofts.hohxilAutoLogin.client.HohxilAutoLoginClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MultiplayerConnectMixin {

    @Inject(method = "join", at = @At("HEAD"))
    private void onConnect(ServerData entry, CallbackInfo ci) {
        HohxilAutoLoginClient.oldInfo = entry;
    }
}