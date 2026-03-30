package net.ofts.hohxilAutoLogin.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;
public class AutoLoginConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("autologin.json");

    public String password = "";
    public TargetServer targetServer = TargetServer.SURVIVAL;

    public enum TargetServer {
        SURVIVAL,
        REDSTONE,
        MINIGAMES,
        NONE
    }

    private static AutoLoginConfig INSTANCE;

    public static AutoLoginConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(Files.readString(CONFIG_PATH), AutoLoginConfig.class);
            } else {
                INSTANCE = new AutoLoginConfig();
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
            INSTANCE = new AutoLoginConfig();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}