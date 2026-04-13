package net.ofts.hohxilAutoLogin.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.ofts.hohxilAutoLogin.client.menu.MenuManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class CommandBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger("HohxilAutoLogin");

    public static void buildCommand(CommandDispatcher<FabricClientCommandSource> dispatcher){
        LiteralArgumentBuilder<FabricClientCommandSource> builder = LiteralArgumentBuilder.literal("autologin");

        builder.executes(_ -> sendUsageGuide());

        builder.then(buildChoosePassword());

        builder.then(buildChooseServer());

        builder.then(buildChooseAddress());

        builder.then(buildTrigger());

        dispatcher.register(builder);
    }

    private static void sendMessage(String s) {
        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.displayClientMessage(Component.literal(s), false);
    }

    private static int changePassword(CommandContext<FabricClientCommandSource> ctx){
        String newPassword = StringArgumentType.getString(ctx, "password");

        AutoLoginConfig config = AutoLoginConfig.get();
        config.password = newPassword;
        AutoLoginConfig.save();

        LOGGER.info("updated password to: {}", config.password);

        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.displayClientMessage(Component.literal("§a[可可西里自动登录] 密码更新成功！"), false);

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

    private static int changeAddress(CommandContext<FabricClientCommandSource> ctx){
        String input = StringArgumentType.getString(ctx, "address").toLowerCase();
        AutoLoginConfig config = AutoLoginConfig.get();
        config.address = input;
        AutoLoginConfig.save();

        LOGGER.info("updated server address to: {}", config.address);

        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.displayClientMessage(Component.literal("§a[可可西里自动登录] 服务器地址更新成功！"), false);

        return 1;
    }

    private static int sendUsageGuide(){
        sendMessage("§e使用方法:");
        sendMessage("§7/autologin setpassword <密码>");
        sendMessage("§7/autologin chooseserver <survival|minigames|redstone|none>");
        sendMessage("§7/autologin changeaddress <目标服务器IP>");
        return 1;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildChoosePassword(){
        return LiteralArgumentBuilder.<FabricClientCommandSource>literal("setpassword")
                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("password", StringArgumentType.greedyString())
                        .executes(CommandBuilder::changePassword)
                )
                .executes(_ -> {
                    sendMessage("§e使用方法: /autologin setpassword <password>");
                    return 1;
                });
    }

    private static CompletableFuture<Suggestions> buildServerSuggest(SuggestionsBuilder builder){
        builder.suggest("survival");
        builder.suggest("redstone");
        builder.suggest("minigames");
        builder.suggest("none");
        return builder.buildFuture();
    }

    private static int runAction(CommandContext<FabricClientCommandSource> ctx){
        String arg = StringArgumentType.getString(ctx, "trigger").toLowerCase();

        if (MenuManager.checkMenu(arg)) return 1;
        if (HohxilAutoLoginClient.runAction(arg)) return 1;
        return 0;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildChooseServer(){
        return LiteralArgumentBuilder.<FabricClientCommandSource>literal("chooseserver")
                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("server", StringArgumentType.word())
                        .suggests((_, builder) -> buildServerSuggest(builder))
                        .executes(CommandBuilder::changeServer)
                )
                .executes(_ -> {
                    sendMessage("§e使用方法: /autologin chooseserver <survival|minigames|redstone|none>");
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildChooseAddress(){
        return LiteralArgumentBuilder.<FabricClientCommandSource>literal("changeaddress")
                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("address", StringArgumentType.greedyString())
                        .suggests((_, builder) ->
                                builder.suggest("cko.cc").suggest("mc.cko.cc:19999").buildFuture()
                        )
                        .executes(CommandBuilder::changeAddress)
                )
                .executes(_ -> {
                    sendMessage("§e使用方法: /autologin changeaddress <服务器地址>");
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildTrigger(){
        return LiteralArgumentBuilder.<FabricClientCommandSource>literal("trigger")
                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("action", StringArgumentType.greedyString())
                        .suggests((_, builder) -> MenuManager.getSuggestion(builder))
                        .suggests((_, builder) -> HohxilAutoLoginClient.getSuggestion(builder))
                        .executes(CommandBuilder::runAction)
                )
                .executes(_ -> {
                    sendMessage("§e使用方法: /autologin trigger <要运行的项目>");
                    return 1;
                });
    }
}
