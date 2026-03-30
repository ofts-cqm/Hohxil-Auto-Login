package net.ofts.hohxilAutoLogin.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class HohxilAutoLoginClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("HohxilAutoLogin");
    public static boolean shouldAutoLogin = true;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.DISCONNECT.register(
                (a, client) -> reconnect(client)
        );
        ClientLoginConnectionEvents.DISCONNECT.register(
                (a, client) -> reconnect(client)
        );
        ClientPlayConnectionEvents.JOIN.register(
                (a, b, c) -> onJoin()
        );
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("autologin")

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
                            )

            );
        });
    }

    private static int changeAddress(CommandContext<FabricClientCommandSource> ctx){
        String input = StringArgumentType.getString(ctx, "address").toLowerCase();
        AutoLoginConfig config = AutoLoginConfig.get();
        config.address = input;
        AutoLoginConfig.save();

        LOGGER.info("updated server address to: " + config.address);

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

        LOGGER.info("updated password to: " + config.password);

        MinecraftClient.getInstance().player.sendMessage(
                Text.literal("§a[可可西里自动登录] 密码更新成功！"),
                false
        );

        return 1;
    }

    public static void reconnect(MinecraftClient client) {
        shouldAutoLogin = true;

        new Thread(() -> {
            try {
                Thread.sleep(3000); // 3 seconds
            } catch (InterruptedException ignored) {
            }

            if (!(client.currentScreen instanceof DisconnectedScreen)) return;

            client.execute(() -> {
                LOGGER.info("reconnecting...");

                String address = AutoLoginConfig.get().address;

                ConnectScreen.connect(
                        client.currentScreen,
                        client,
                        ServerAddress.parse(address),
                        new ServerInfo("Minecraft Server", address, ServerInfo.ServerType.OTHER),
                        false,
                        null
                );
            });
        }).start();
    }

    public static void onJoin() {
        if (!shouldAutoLogin) return;

        ServerInfo server = MinecraftClient.getInstance().getCurrentServerEntry();

        if (server == null || !server.address.equals(AutoLoginConfig.get().address)) {
            return;
        }

        shouldAutoLogin = false;
        LOGGER.info("Server Joined, wait 1s to send login credentials");

        new Thread(() -> {
            try {
                Thread.sleep(1000); // 2 seconds
            } catch (InterruptedException ignored) {}

            MinecraftClient client = MinecraftClient.getInstance();

            String password = AutoLoginConfig.get().password;

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
                    client.player.sendMessage(Text.literal("§a[可可西里自动登录] 正在尝试登录"), false);
                }
            });

            AtomicBoolean flag = new AtomicBoolean(true);
            for (int i = 0; i < 20 && flag.get(); i++) { // retry for ~10 seconds
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {}

                client.execute(() -> {
                    if (client.player == null) return;

                    var stack = client.player.getInventory().getStack(4);

                    if (!stack.isEmpty()) {
                        client.player.getInventory().setSelectedSlot(4);
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                        flag.set(false);
                    }
                });
            }
        }).start();
    }
}
