package net.ofts.hohxilAutoLogin.client;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.ofts.hohxilAutoLogin.client.configUI.ModMenuAPIImpl;
import net.ofts.hohxilAutoLogin.client.menu.MenuManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class HohxilAutoLoginClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("HohxilAutoLogin");
    public static boolean shouldAutoLogin = true;
    public static boolean sendAfterServerCommands = false;
    public static int reconnectionTried = 0;
    public static ServerData oldInfo = null;
    public static Screen lastScreen = null;
    public static final AutoLoginConfig config = AutoLoginConfig.get();

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register(
                (_, _, _) -> onJoin()
        );
        ClientPlayConnectionEvents.DISCONNECT.register(
                (_, _) -> handleDisconnection()
        );
        ClientLoginConnectionEvents.DISCONNECT.register(
                (_, _) -> handleDisconnection()
        );
        ClientReceiveMessageEvents.GAME.register((message, _) -> {
            String msg = message.getString();
            if (msg.contains("加入我们可可西里") && msg.contains("欢迎") && config.autoGreeting){
                handleGreeting();
            }
            if (msg.contains("你今天还没有签到") && config.autoCheckin){
                MenuManager.checkMenu(MenuManager.CHECK_IN);
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> CommandBuilder.buildCommand(dispatcher));

        checkDependencies();
    }

    public static CompletableFuture<Suggestions> getSuggestion(SuggestionsBuilder builder){
        builder.suggest("start_greeting");
        builder.suggest("start_reconnection");
        builder.suggest("on_join_main_server");
        builder.suggest("on_join_login_hall");
        builder.suggest("on_joined_general");

        return builder.buildFuture();
    }

    public static boolean runAction(String trigger){
        return switch (trigger) {
            case "start_greeting" -> {
                handleGreeting();
                yield true;
            }
            case "start_reconnection" -> {
                reconnect(Minecraft.getInstance(), true);
                yield true;
            }
            case "on_join_main_server" -> {
                new Thread(HohxilAutoLoginClient::onJoinedMainServer).start();
                yield true;
            }
            case "on_join_login_hall" -> {
                new Thread(HohxilAutoLoginClient::onJoinedLoginHall).start();
                yield true;
            }
            case "on_joined_general" -> {
                onJoin();
                yield true;
            }
            default -> false;
        };
    }

    private static void handleGreeting(){
        List<String> messages = AutoLoginConfig.get().greetingMessageList;
        boolean sequential = AutoLoginConfig.get().sequential;
        Minecraft client = Minecraft.getInstance();

        new Thread(() -> {
            if (!sequential){
                if (messages.isEmpty()) return;

                Random rand = new Random();
                String message =  messages.get(rand.nextInt(messages.size()));

                LOGGER.info("sending message {} on thread {}", message, Thread.currentThread().getName());
                client.execute(() -> Objects.requireNonNull(client.getConnection()).sendChat(message));
                return;
            }

            for (int i = 0; i < messages.size(); i++){
                String message = messages.get(i);

                try {
                    Thread.sleep(AutoLoginConfig.get().greetingInterval);
                } catch (InterruptedException ignored) {}

                LOGGER.info("sending message {} on thread {}", message, Thread.currentThread().getName());
                client.execute(() -> Objects.requireNonNull(client.getConnection()).sendChat(message));
            }
        }).start();
    }

    void handleDisconnection(){
        shouldAutoLogin = true;
        MenuManager.clearTaskQueue();
    }

    void checkDependencies(){
        boolean hasModMenu = FabricLoader.getInstance().isModLoaded("modmenu");
        boolean hasClothConfig = FabricLoader.getInstance().isModLoaded("cloth-config");

        if (hasModMenu && !hasClothConfig){
            ModMenuAPIImpl.enableAPI = false;
        }
    }

    public static void onLoaded(Minecraft client){
        AutoLoginConfig config = AutoLoginConfig.get();
        if (!config.autoConnect) return;

        ServerList serverList = new ServerList(Minecraft.getInstance());
        serverList.load();
        for (int i = 0; i < serverList.size(); i++) {
            ServerData info = serverList.get(i);

            if (info.ip.trim().equals(config.address.trim())){
                oldInfo = info;
                break;
            }
        }

        reconnect(client, true);
    }

    public static void reconnect(Minecraft client, boolean force) {
        AutoLoginConfig config = AutoLoginConfig.get();

        if (!force && config.connectionRetryCount != 0 && reconnectionTried > config.connectionRetryCount) return;

        reconnectionTried++;
        shouldAutoLogin = true;
        sendAfterServerCommands = false;

        new Thread(() -> {
            if (!force) {
                try {
                    Thread.sleep(config.joinDelay); // 3 seconds
                } catch (InterruptedException ignored) {
                }
            }

            if (!(force || client.screen instanceof DisconnectedScreen)) return;

            String address = AutoLoginConfig.get().address;
            if (oldInfo == null){
                oldInfo = new ServerData("Minecraft Server", address, ServerData.Type.OTHER);
            }

            client.execute(() -> {
                LOGGER.info("reconnecting...");

                assert client.screen != null;
                ConnectScreen.startConnecting(
                        client.screen,
                        client,
                        ServerAddress.parseString(address),
                        oldInfo,
                        false,
                        null
                );
            });
        }).start();
    }

    private static void onJoinedLoginHall(){
        AutoLoginConfig config = AutoLoginConfig.get();

        try {
            Thread.sleep(config.loginDelay); // 2 seconds
        } catch (InterruptedException ignored) {}

        Minecraft client = Minecraft.getInstance();

        String password = config.password;

        if (password.isEmpty()) {
            LOGGER.info("Password is empty, skipping");

            assert client.player != null;
            client.execute(() -> client.player.displayClientMessage(Component.literal("§a[可可西里自动登录] 警告：您未设置登录密码。使用/autologin setpassword <密码> 设置登录密码"), false));
            return;
        }

        client.execute(() -> {
            if (client.getConnection() != null) {
                LOGGER.info("Sending password [{}]", password);
                client.getConnection().sendCommand("login " + password);
                assert client.player != null;
                client.player.displayClientMessage(Component.literal("§a[可可西里自动登录] 正在尝试登录"), false);
            }
        });

        try {
            Thread.sleep(config.commandDelay); // 2 seconds
        } catch (InterruptedException ignored) {}

        client.execute(() -> {
            LOGGER.info("Sending ({}) custom commands", config.customCommands.size());
            for (String customCommand : config.customCommands) {
                Objects.requireNonNull(client.getConnection())
                        .sendCommand(customCommand);
            }
        });

        if (config.targetServer != AutoLoginConfig.TargetServer.NONE) MenuManager.checkMenu(MenuManager.SERVER_CHOOSER);
    }

    private static void onJoinedMainServer(){
        AutoLoginConfig config = AutoLoginConfig.get();

        try {
            Thread.sleep(config.commandDelay); // 2 seconds
        } catch (InterruptedException ignored) {
        }

        Minecraft client = Minecraft.getInstance();

        client.execute(() -> {
            LOGGER.info("Sending ({}) custom commands after joining server", config.customCommandsAfterServer.size());
            for (String customCommand : config.customCommandsAfterServer) {
                Objects.requireNonNull(client.getConnection())
                        .sendCommand(customCommand);
            }
        });

        if (!config.refreshTitle) return;
        if (config.titleToRefresh.isEmpty()) {
            client.execute(() -> {
                assert client.player != null;
                client.player.displayClientMessage(Component.literal("§a[可可西里自动登录] 您未设置自动刷新的称号！"), false);
            });
            return;
        }

        try {
            Thread.sleep(config.commandDelay); // 2 seconds
        } catch (InterruptedException ignored) {
        }

        MenuManager.checkMenu(MenuManager.REFRESH_TITLE);
    }

    private static void onJoin() {
        reconnectionTried = 0;

        if (sendAfterServerCommands){
            sendAfterServerCommands = false;

            new Thread(HohxilAutoLoginClient::onJoinedMainServer).start();
            return;
        }

        sendAfterServerCommands = true;

        if (!shouldAutoLogin) return;

        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null || !server.ip.equals(AutoLoginConfig.get().address)) {
            return;
        }

        shouldAutoLogin = false;
        LOGGER.info("Server Joined, wait 1s to send login credentials");

        new Thread(HohxilAutoLoginClient::onJoinedLoginHall).start();
    }
}
