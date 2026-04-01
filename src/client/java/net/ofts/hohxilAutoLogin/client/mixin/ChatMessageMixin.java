package net.ofts.hohxilAutoLogin.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.ofts.hohxilAutoLogin.client.AutoLoginConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatMessageMixin {
    @Inject(method="onGameMessage", at=@At("TAIL"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet.content().getString().contains("加入我们可可西里～") && AutoLoginConfig.get().autoGreeting){
            ((ClientPlayNetworkHandler)(Object) this).sendChatMessage(AutoLoginConfig.get().greetingMessage);
        }
    }
}
