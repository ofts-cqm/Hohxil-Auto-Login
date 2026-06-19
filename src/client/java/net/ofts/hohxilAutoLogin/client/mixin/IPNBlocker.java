package net.ofts.hohxilAutoLogin.client.mixin;

import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Debug
@Mixin(targets = {"org.anti_ad.mc.ipnext.event.LockedSlotKeeper"})
public class IPNBlocker {
    @Inject(method="checkNewItems", at = @At("HEAD"), cancellable = true)
    private void blockCheckNewItems(CallbackInfo ci){
        LoggerFactory.getLogger("IPNBlocker").info("CheckNewItems blocked");
        ci.cancel();
    }
}
