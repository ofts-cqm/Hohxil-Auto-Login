package net.ofts.hohxilAutoLogin.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
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

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    @Final
    private Window window;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onInitSetScreen(Screen screen, CallbackInfo ci){
        if (screen instanceof HandledScreen<?> handledScreen) {
            if (MenuManager.handleMenu(handledScreen) && AutoLoginConfig.get().hideMenu) {
                screen.init(this.window.getScaledWidth(), this.window.getScaledHeight());
                ci.cancel();
            }
        } else if (screen instanceof BookScreen bookScreen && AutoLoginConfig.get().closeAnnouncement) {
            for (Text text : ((BookScreenMixin) bookScreen).getContents().pages()){
                if (text.getString().contains("点击查看上个公告")){
                    assert MinecraftClient.getInstance().player != null;
                    MinecraftClient.getInstance().player.sendMessage(
                            Text.literal("已为您自动关闭公告").withColor(0x00FFFF), false
                    );
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
            HohxilAutoLoginClient.reconnect(MinecraftClient.getInstance(), false);
        }else if (screen instanceof TitleScreen){
            if (lastScreen == null) HohxilAutoLoginClient.onLoaded((MinecraftClient)(Object)this);
        }

        if (!(screen instanceof MessageScreen)) lastScreen = screen;
    }
}