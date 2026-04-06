package net.ofts.hohxilAutoLogin.client.configUI;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DependencyErrorScreen extends Screen {
    private final LinearLayout grid;
    private final Component message;
    private final Screen parent;

    public DependencyErrorScreen(Screen parent) {
        super(Component.literal("错误：缺失前置模组"));
        this.message = Component.literal("如果想使用模组菜单，您必须安装Cloth Config");
        this.parent = parent;
        this.grid = LinearLayout.vertical();
    }

    @Override
    protected void init() {
        this.grid.defaultCellSetting().alignHorizontallyCenter().padding(10);
        this.grid.addChild(new StringWidget(this.title, this.font));
        this.grid.addChild(new StringWidget(message, font));

        this.grid.addChild(Button.builder(
                Component.literal("返回菜单"),
                _ -> this.minecraft.setScreen(parent)
        ).bounds(this.width / 2 - 100, this.height / 2 + 20, 200, 20).build());
        
        this.grid.arrangeElements();
        this.grid.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        FrameLayout.centerInRectangle(this.grid, this.getRectangle());
    }
}