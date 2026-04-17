package net.ofts.hohxilAutoLogin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.ofts.hohxilAutoLogin.client.menu.MenuManager;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class RunActionMenu extends Screen {
    private final GridLayout layout;

    protected RunActionMenu() {
        super(Component.literal("运行程序"));
        layout = new GridLayout();
    }

    @Override
    protected void init() {
        super.init();
        layout.defaultCellSetting().padding(4);
        GridLayout.RowHelper helper = layout.createRowHelper(2);

        FittingMultiLineTextWidget details = new FittingMultiLineTextWidget(0, 0, 248, 75, Component.empty(), this.font);

        int textWidth = this.font.width(this.title);
        Objects.requireNonNull(this.font);
        this.addRenderableWidget(new StringWidget(this.width / 2 - textWidth / 2, 40, textWidth, 9, this.title, this.font));

        helper.addChild(new StringWidget(Component.literal("整合程序："), this.font), 2);
        helper.addChild(new StringWidget(Component.literal("所有的一条龙服务"), this.font), 2);

        helper.addChild(new ActionButton("重新链接", "手动重新连接服务器\n\n如果你发现自己卡住了，可以尝试手动重连", "start_reconnection", details));
        helper.addChild(new ActionButton("运行进入后程序", "手动运行进入主服务器（比如生存服）后的程序，比如发送自定义指令，刷新称号，自动签到，等等", "on_join_main_server", details));
        helper.addChild(new ActionButton("运行登录大厅程序", "手动运行进入登录大厅后的程序，比如发送自定义指令，自动选择服务器，等等", "on_join_login_hall", details));

        helper.addChild(new StringWidget(Component.literal("子程序："), this.font), 2);
        helper.addChild(new StringWidget(Component.literal("所有“一条龙服务”中的子程序"), this.font), 2);

        helper.addChild(new ActionButton("欢迎玩家", "运行欢迎新玩家的程序\n\n有些时候（比如生电服）无法收到新玩家进入消息，此时可以手动运行", "start_greeting", details));
        helper.addChild(new ActionButton("选择服务器", "在登录大厅，运行自动进入子服务器程序", "choose_server", details));
        helper.addChild(new ActionButton("签到", "运行自动签到程序", "checkin", details));
        helper.addChild(new ActionButton("领取在线奖励", "运行自动领取在线奖励程序", "claim_reward", details));
        helper.addChild(new ActionButton("刷新称号", "运行刷新称号程序", "refresh_title", details));
        helper.addChild(new ActionButton("登录大厅指令", "手动运行所有进入登录大厅后的自定义指令", "pre_commands", details));
        helper.addChild(new ActionButton("主服务器指令", "手动运行所有进入主服务器后的自定义指令", "post_commands", details));

        helper.addChild(details, 2);

        layout.arrangeElements();
        FrameLayout.centerInRectangle(layout, this.getRectangle());
        layout.visitWidgets(this::addRenderableWidget);
    }

    private static class ActionButton extends Button.Plain{
        private final Component detail;
        private final FittingMultiLineTextWidget detailsWidget;
        private boolean justHovered = false;

        protected ActionButton(String name, String details, String action, FittingMultiLineTextWidget detailsWidget) {
            super(0, 0, 120, 20, Component.literal(name), _ -> runAction(action), Button.DEFAULT_NARRATION);
            this.detail = Component.literal(details);
            this.detailsWidget = detailsWidget;
        }

        private static void runCommand(String action){
            if (MenuManager.checkMenu(action)) return;
            if (HohxilAutoLoginClient.runAction(action)) return;

            Minecraft client = Minecraft.getInstance();
            AutoLoginConfig config = AutoLoginConfig.get();

            if (action.equals("pre_commands") || action.equals("post_commands")){
                client.execute(() -> {
                    for (String customCommand : action.equals("pre_commands") ? config.customCommands : config.customCommandsAfterServer) {
                        Objects.requireNonNull(client.getConnection())
                                .sendCommand(customCommand);
                    }
                });
            }

            assert Minecraft.getInstance().player != null;
            Minecraft.getInstance().player.displayClientMessage(Component.literal("错误：未知指令"), false);
        }

        private static void runAction(String action){
            runCommand(action);
            Minecraft.getInstance().setScreen(null);
        }

        @Override
        public void renderContents(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float a) {
            super.renderContents(graphics, mouseX, mouseY, a);
            if (this.isHovered && !justHovered) detailsWidget.setMessage(detail);
            else if (this.justHovered && !isHovered) detailsWidget.setMessage(Component.empty());
            justHovered = isHovered;
        }
    }
}
