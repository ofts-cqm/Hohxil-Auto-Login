package net.ofts.hohxilAutoLogin.client.mixin;

import net.ofts.hohxilAutoLogin.client.AutoLoginConfig;
import net.ofts.hohxilAutoLogin.client.HohxilAutoLoginClient;
import net.ofts.hohxilAutoLogin.client.menu.MenuManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.ofts.hohxilAutoLogin.client.HohxilAutoLoginClient.lastScreen;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    @Final
    private Window window;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onInitSetScreen(Screen screen, CallbackInfo ci){
        if (screen instanceof AbstractContainerScreen<?> handledScreen) {
            if (MenuManager.handleMenu(handledScreen) && AutoLoginConfig.get().hideMenu) {
                screen.init(this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
                ci.cancel();
            }
        } else if (screen instanceof BookViewScreen bookScreen && AutoLoginConfig.get().closeAnnouncement) {
            for (Component text : ((BookScreenMixin) bookScreen).getContents().pages()){
                if (text.getString().contains("点击查看上个公告")){
                    assert Minecraft.getInstance().player != null;
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("已为您自动关闭公告").withColor(0x00FFFF));
                    ci.cancel();
                    return;
                }
            }
        }
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void onSetScreen(Screen screen, CallbackInfo ci)  {
        if (screen instanceof DisconnectedScreen disconnected) {
            if (AutoLoginConfig.get().matchBlacklist(disconnected.getTitle().getString())) return;
            HohxilAutoLoginClient.reconnect(Minecraft.getInstance(), false);
        }else if (screen instanceof TitleScreen){
            if (lastScreen == null) HohxilAutoLoginClient.onLoaded((Minecraft)(Object)this);
        }

        if (!(screen instanceof GenericMessageScreen)) lastScreen = screen;
    }
}