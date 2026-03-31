package net.ofts.hohxilAutoLogin.client.mixin;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;
import net.ofts.hohxilAutoLogin.client.HohxilAutoLoginClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerConnectMixin {

    @Inject(method = "connect", at = @At("HEAD"))
    private void onConnect(ServerInfo entry, CallbackInfo ci) {
        HohxilAutoLoginClient.oldInfo = entry;
    }
}