package net.ofts.hohxilAutoLogin.client.mixin;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screens.multiplayer.CodeOfConductScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
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
    private BooleanConsumer resultConsumer;

    @Shadow
    @Final
    private ServerData serverData;

    @Shadow
    @Final
    private String codeOfConductText;

    @Inject(method = "<init>*", at=@At("TAIL"))
    private void onCreate(CallbackInfo ci){
        this.resultConsumer.accept(true);
        this.serverData.acceptCodeOfConduct(this.codeOfConductText);
        ServerList.saveSingleServer(this.serverData);
    }
}
