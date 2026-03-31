package net.ofts.hohxilAutoLogin.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.ofts.hohxilAutoLogin.client.AutoLoginConfig;
import net.ofts.hohxilAutoLogin.client.AutomaticChooser;
import net.ofts.hohxilAutoLogin.client.HohxilAutoLoginClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void onSetScreen(Screen screen, CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException {
        if (screen instanceof HandledScreen<?> handledScreen) {
            AutomaticChooser.choose(handledScreen);
        } else if (screen instanceof DisconnectedScreen disconnected) {
            if (AutoLoginConfig.get().matchBlacklist(disconnected.getTitle().getString())) return;
            HohxilAutoLoginClient.reconnect(MinecraftClient.getInstance());
        } else if (screen instanceof BookScreen bookScreen && AutoLoginConfig.get().closeAnnouncement) {
            Field field = BookScreen.class.getDeclaredField("contents");
            field.setAccessible(true);
            if (field.get(bookScreen) instanceof BookScreen.Contents(java.util.List<Text> pages)){
                for (Text text : pages){
                    if (text.getString().contains("点击查看上个公告")){
                        MinecraftClient.getInstance().setScreen(null);
                        MinecraftClient.getInstance().player.sendMessage(
                                Text.literal("已为您自动关闭公告").withColor(0x00FFFF), false
                        );
                        return;
                    }
                }
            }
        }
    }
}