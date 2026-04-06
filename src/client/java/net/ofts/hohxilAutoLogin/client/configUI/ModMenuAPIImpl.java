package net.ofts.hohxilAutoLogin.client.configUI;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuAPIImpl implements ModMenuApi {
    public static boolean enableAPI = true;

    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!enableAPI) return ModMenuAPIImpl::fallbackScreen;

        return (parent) -> {
            try {
                Class<?> clazz = Class.forName("net.ofts.hohxilAutoLogin.client.configUI.ConfigScreen");
                return (Screen) clazz.getMethod("getConfig", Screen.class).invoke(null, parent);
            } catch (Exception e) {
                return new DependencyErrorScreen(parent);
            }
        };
    }

    public static Screen fallbackScreen(Screen parent){
        return new DependencyErrorScreen(parent);
    }
}
