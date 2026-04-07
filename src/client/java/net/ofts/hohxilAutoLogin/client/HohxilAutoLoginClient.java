package net.ofts.hohxilAutoLogin.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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

import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class HohxilAutoLoginClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("HohxilAutoLogin");
    public static boolean shouldAutoLogin = true;
    public static boolean sendAfterServerCommands = false;
    public static int reconnectionTried = 0;
    public static ServerData oldInfo = null;
    public static Screen lastScreen = null;
    public static final LiteralArgumentBuilder<FabricClientCommandSource> command;

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
            if (msg.contains("加入我们可可西里") && msg.contains("欢迎") && AutoLoginConfig.get().autoGreeting){
                handleGreeting();
            }
            if (msg.contains("你今天还没有签到") && AutoLoginConfig.get().autoCheckin){
                MenuManager.checkMenu(MenuManager.CHECK_IN);
            }
        });
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, _) -> dispatcher.register(command)
        );

        checkDependencies();
    }

    void handleGreeting(){
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

    private static int changeAddress(CommandContext<FabricClientCommandSource> ctx){
        String input = StringArgumentType.getString(ctx, "address").toLowerCase();
        AutoLoginConfig config = AutoLoginConfig.get();
        config.address = input;
        AutoLoginConfig.save();

        LOGGER.info("updated server address to: " + config.address);

        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("§a[可可西里自动登录] 服务器地址更新成功！"));

        return 1;
    }

    private static int changeServer(CommandContext<FabricClientCommandSource> ctx){
        String input = StringArgumentType.getString(ctx, "server").toLowerCase();

        AutoLoginConfig config = AutoLoginConfig.get();

        switch (input) {
            case "survival" -> config.targetServer = AutoLoginConfig.TargetServer.SURVIVAL;
            case "redstone" -> config.targetServer = AutoLoginConfig.TargetServer.REDSTONE;
            case "minigames" -> config.targetServer = AutoLoginConfig.TargetServer.MINIGAMES;
            case "none" -> config.targetServer = AutoLoginConfig.TargetServer.NONE;
            default -> {
                sendMessage("§c无效的服务器！");
                return 0;
            }
        }

        AutoLoginConfig.save();

        sendMessage("§a已选择服务器: " + config.targetServer.name().toLowerCase());
        return 1;
    }

    private static void sendMessage(String s) {
        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.sendSystemMessage(Component.literal(s));
    }

    public static int changePassword(CommandContext<FabricClientCommandSource> ctx){
        String newPassword = StringArgumentType.getString(ctx, "password");

        AutoLoginConfig config = AutoLoginConfig.get();
        config.password = newPassword;
        AutoLoginConfig.save();

        LOGGER.info("updated password to: {}", config.password);

        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("§a[可可西里自动登录] 密码更新成功！"));

        return 1;
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

    public static void onJoin() {
        reconnectionTried = 0;

        if (sendAfterServerCommands){
            sendAfterServerCommands = false;

            new Thread(() -> {
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

            }).start();

            return;
        }

        if (!shouldAutoLogin) return;

        ServerData server = Minecraft.getInstance().getCurrentServer();

        if (server == null || !server.ip.equals(AutoLoginConfig.get().address)) {
            return;
        }

        shouldAutoLogin = false;
        LOGGER.info("Server Joined, wait 1s to send login credentials");

        new Thread(() -> {
            AutoLoginConfig config = AutoLoginConfig.get();

            try {
                Thread.sleep(config.loginDelay); // 2 seconds
            } catch (InterruptedException ignored) {}

            Minecraft client = Minecraft.getInstance();

            String password = config.password;

            if (password.isEmpty()) {
                LOGGER.info("Password is empty, skipping");

                assert client.player != null;
                client.execute(() -> client.player.sendSystemMessage(Component.literal("§a[可可西里自动登录] 警告：您未设置登录密码。使用/autologin setpassword <密码> 设置登录密码")));
                return;
            }

            client.execute(() -> {
                if (client.getConnection() != null) {
                    LOGGER.info("Sending password [{}]", password);
                    client.getConnection().sendCommand("login " + password);
                    assert client.player != null;
                    client.player.sendSystemMessage(Component.literal("§a[可可西里自动登录] 正在尝试登录"));
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
        }).start();
    }

    static {
        command = LiteralArgumentBuilder.<FabricClientCommandSource>literal("autologin")

                // /autologin
                .executes(_ -> {
                    sendMessage("§e使用方法:");
                    sendMessage("§7/autologin setpassword <密码>");
                    sendMessage("§7/autologin chooseserver <survival|minigames|redstone|none>");
                    return 1;
                })

                // /autologin setpassword <password>
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("setpassword")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("password", StringArgumentType.greedyString())
                                .executes(HohxilAutoLoginClient::changePassword)
                        )
                        .executes(_ -> {
                            sendMessage("§e使用方法: /autologin setpassword <password>");
                            return 1;
                        })
                )

                // /autologin chooseserver <...>
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("chooseserver")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("server", StringArgumentType.word())
                                .suggests((_, builder) -> {
                                    builder.suggest("survival");
                                    builder.suggest("redstone");
                                    builder.suggest("minigames");
                                    builder.suggest("none");
                                    return builder.buildFuture();
                                })
                                .executes(HohxilAutoLoginClient::changeServer)
                        )
                        .executes(_ -> {
                            sendMessage("§e使用方法: /autologin chooseserver <survival|minigames|redstone|none>");
                            return 1;
                        })
                )

                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("changeaddress")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("address", StringArgumentType.greedyString())
                                .suggests((_, builder) ->
                                        builder.suggest("cko.cc").suggest("mc.cko.cc:19999").buildFuture()
                                )
                                .executes(HohxilAutoLoginClient::changeAddress)
                        )
                        .executes(_ -> {
                            sendMessage("§e使用方法: /autologin changeaddress <服务器地址>");
                            return 1;
                        })
                )

                // /autologin claim_
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("claim")
                        .executes(_ -> {
                            MenuManager.checkMenu(MenuManager.AFK_REWARD);
                            return 1;
                        })
                );
    }
}
