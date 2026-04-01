package net.ofts.hohxilAutoLogin.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
public class AutoLoginConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("autologin.json");

    public String password = "";
    public String address = "cko.cc";
    public TargetServer targetServer = TargetServer.SURVIVAL;
    public int connectionRetryCount = 0;
    public List<String> reconnectionFilter = new ArrayList<>();
    public List<String> customCommands = new ArrayList<>();
    public List<String> customCommandsAfterServer = new ArrayList<>();
    public int joinDelay = 3000;
    public int loginDelay = 1000;
    public int openMenuDelay = 500;
    public int retryCount = 20;
    public int clickDelay = 1000;
    public int commandDelay = 500;
    public boolean closeAnnouncement = true;
    public boolean autoConnect = false;
    public boolean autoGreeting = false;
    public String greetingMessage = "欢迎欢迎～";

    public enum TargetServer {
        SURVIVAL,
        REDSTONE,
        MINIGAMES,
        NONE
    }

    private static AutoLoginConfig INSTANCE;

    public boolean matchBlacklist(String message){
        for(String str : reconnectionFilter){
            if (message.contains(str)) return true;
        }
        return false;
    }

    public void setDelay(int delay, int field) {
        switch (field) {
            case 0:
                joinDelay = delay;
                break;
            case 1:
                loginDelay = delay;
                break;
            case 2:
                openMenuDelay = delay;
                break;
            case 3:
                retryCount = delay;
                break;
            case 5:
                connectionRetryCount = delay;
                break;
            case 6:
                commandDelay = delay;
                break;
            default:
                clickDelay = delay;
                break;
        }
    }

    public int getDelay(int field) {
        return switch (field){
            case 0 -> joinDelay;
            case 1 -> loginDelay;
            case 2 -> openMenuDelay;
            case 3 -> retryCount;
            case 5 -> connectionRetryCount;
            case 6 -> commandDelay;
            default -> clickDelay;
        };
    }

    public int getDefault(int index){
        return switch (index){
            case 0 -> 3000;
            case 2, 6 -> 500;
            case 3 -> 20;
            case 5 -> 0;
            default -> 1000;
        };
    }

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