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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import net.ofts.hohxilAutoLogin.client.configUI.ModMenuAPIImpl;
import net.ofts.hohxilAutoLogin.client.menu.MenuManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class HohxilAutoLoginClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("HohxilAutoLogin");
    public static boolean shouldAutoLogin = true;
    public static boolean sendAfterServerCommands = false;
    public static int reconnectionTried = 0;
    public static ServerInfo oldInfo = null;
    public static Screen lastScreen = null;
    public static final LiteralArgumentBuilder<FabricClientCommandSource> command;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register(
                (a, b, c) -> onJoin()
        );
        ClientPlayConnectionEvents.DISCONNECT.register(
                (a, b) -> handleDisconnection()
        );
        ClientLoginConnectionEvents.DISCONNECT.register(
                (a, b) -> handleDisconnection()
        );
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String msg = message.getString();
            if (msg.contains("加入我们可可西里") && msg.contains("欢迎") && AutoLoginConfig.get().autoGreeting){
                Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatMessage(AutoLoginConfig.get().greetingMessage);
            }
            if (msg.contains("你今天还没有签到") && AutoLoginConfig.get().autoCheckin){
                MenuManager.checkMenu(MenuManager.CHECK_IN);
            }
        });
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> dispatcher.register(command)
        );

        checkDependencies();
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

    public static void onLoaded(MinecraftClient client){
        AutoLoginConfig config = AutoLoginConfig.get();
        if (!config.autoConnect) return;

        ServerList serverList = new ServerList(MinecraftClient.getInstance());
        serverList.loadFile();
        for (int i = 0; i < serverList.size(); i++) {
            ServerInfo info = serverList.get(i);

            if (info.address.trim().equals(config.address.trim())){
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

        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(
                Text.literal("§a[可可西里自动登录] 服务器地址更新成功！"),
                false
        );

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
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(
                Text.literal(s),
                false
        );
    }

    public static int changePassword(CommandContext<FabricClientCommandSource> ctx){
        String newPassword = StringArgumentType.getString(ctx, "password");

        AutoLoginConfig config = AutoLoginConfig.get();
        config.password = newPassword;
        AutoLoginConfig.save();

        LOGGER.info("updated password to: {}", config.password);

        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(
                Text.literal("§a[可可西里自动登录] 密码更新成功！"),
                false
        );

        return 1;
    }

    public static void reconnect(MinecraftClient client, boolean force) {
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

            if (!(force || client.currentScreen instanceof DisconnectedScreen)) return;

            String address = AutoLoginConfig.get().address;
            if (oldInfo == null){
                oldInfo = new ServerInfo("Minecraft Server", address, ServerInfo.ServerType.OTHER);
            }

            client.execute(() -> {
                LOGGER.info("reconnecting...");

                ConnectScreen.connect(
                        client.currentScreen,
                        client,
                        ServerAddress.parse(address),
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

                MinecraftClient client = MinecraftClient.getInstance();

                client.execute(() -> {
                    LOGGER.info("Sending ({}) custom commands after joining server", config.customCommandsAfterServer.size());
                    for (String customCommand : config.customCommandsAfterServer) {
                        Objects.requireNonNull(client.getNetworkHandler())
                                .sendChatCommand(customCommand);
                    }
                });

            }).start();

            return;
        }

        if (!shouldAutoLogin) return;

        ServerInfo server = MinecraftClient.getInstance().getCurrentServerEntry();

        if (server == null || !server.address.equals(AutoLoginConfig.get().address)) {
            return;
        }

        shouldAutoLogin = false;
        LOGGER.info("Server Joined, wait 1s to send login credentials");

        new Thread(() -> {
            AutoLoginConfig config = AutoLoginConfig.get();

            try {
                Thread.sleep(config.loginDelay); // 2 seconds
            } catch (InterruptedException ignored) {}

            MinecraftClient client = MinecraftClient.getInstance();

            String password = config.password;

            if (password.isEmpty()) {
                LOGGER.info("Password is empty, skipping");

                assert client.player != null;
                client.execute(() -> client.player.sendMessage(Text.literal("§a[可可西里自动登录] 警告：您未设置登录密码。使用/autologin setpassword <密码> 设置登录密码"), false));
                return;
            }

            client.execute(() -> {
                if (client.getNetworkHandler() != null) {
                    LOGGER.info("Sending password [{}]", password);
                    client.getNetworkHandler().sendChatCommand("login " + password);
                    assert client.player != null;
                    client.player.sendMessage(Text.literal("§a[可可西里自动登录] 正在尝试登录"), false);
                }
            });

            try {
                Thread.sleep(config.commandDelay); // 2 seconds
            } catch (InterruptedException ignored) {}

            client.execute(() -> {
                LOGGER.info("Sending ({}) custom commands", config.customCommands.size());
                for (String customCommand : config.customCommands) {
                    Objects.requireNonNull(client.getNetworkHandler())
                            .sendChatCommand(customCommand);
                }
            });

            if (config.targetServer != AutoLoginConfig.TargetServer.NONE) MenuManager.checkMenu(MenuManager.SERVER_CHOOSER);
        }).start();
    }

    static {
        command = literal("autologin")

                // /autologin
                .executes(ctx -> {
                    sendMessage("§e使用方法:");
                    sendMessage("§7/autologin setpassword <密码>");
                    sendMessage("§7/autologin chooseserver <survival|minigames|redstone|none>");
                    return 1;
                })

                // /autologin setpassword <password>
                .then(literal("setpassword")
                        .then(argument("password", StringArgumentType.greedyString())
                                .executes(HohxilAutoLoginClient::changePassword)
                        )
                        .executes(ctx -> {
                            sendMessage("§e使用方法: /autologin setpassword <password>");
                            return 1;
                        })
                )

                // /autologin chooseserver <...>
                .then(literal("chooseserver")
                        .then(argument("server", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("survival");
                                    builder.suggest("redstone");
                                    builder.suggest("minigames");
                                    builder.suggest("none");
                                    return builder.buildFuture();
                                })
                                .executes(HohxilAutoLoginClient::changeServer)
                        )
                        .executes(ctx -> {
                            sendMessage("§e使用方法: /autologin chooseserver <survival|minigames|redstone|none>");
                            return 1;
                        })
                )

                .then(literal("changeaddress")
                        .then(argument("address", StringArgumentType.greedyString())
                                .suggests((context, builder) ->
                                        builder.suggest("cko.cc").suggest("mc.cko.cc:19999").buildFuture()
                                )
                                .executes(HohxilAutoLoginClient::changeAddress)
                        )
                        .executes(ctx -> {
                            sendMessage("§e使用方法: /autologin changeaddress <服务器地址>");
                            return 1;
                        })
                );
    }
}
