package net.ofts.hohxilAutoLogin.client.mixin;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.multiplayer.CodeOfConductScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CodeOfConductScreen.class)
public abstract class CodeOfConductScreenMixin {

    @Shadow
    @Final
    private BooleanConsumer callback;

    @Shadow
    @Final
    private ServerInfo serverInfo;

    @Shadow
    @Final
    private String rawCodeOfConduct;

    @Inject(method = "<init>*", at=@At("TAIL"))
    private void onCreate(CallbackInfo ci){
        this.callback.accept(true);
        this.serverInfo.setAcceptedCodeOfConduct(this.rawCodeOfConduct);
        ServerList.updateServerListEntry(this.serverInfo);
    }
}
