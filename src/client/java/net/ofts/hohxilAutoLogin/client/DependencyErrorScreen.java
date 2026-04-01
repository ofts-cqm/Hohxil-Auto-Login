package net.ofts.hohxilAutoLogin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public class DependencyErrorScreen extends Screen {
    private final DirectionalLayoutWidget grid;
    private final Text message;
    private final Screen parent;

    public DependencyErrorScreen(Screen parent) {
        super(Text.literal("错误：缺失前置模组"));
        this.message = Text.literal("如果想使用模组菜单，您必须安装Cloth Config");
        this.parent = parent;
        this.grid = DirectionalLayoutWidget.vertical();
    }

    @Override
    protected void init() {
        this.grid.getMainPositioner().alignHorizontalCenter().margin(10);
        this.grid.add(new TextWidget(this.title, this.textRenderer));
        this.grid.add(new TextWidget(message, textRenderer));

        this.grid.add(ButtonWidget.builder(
                Text.literal("返回菜单"),
                btn -> this.client.setScreen(parent)
        ).dimensions(this.width / 2 - 100, this.height / 2 + 20, 200, 20).build());
        
        this.grid.refreshPositions();
        this.grid.forEachChild(this::addDrawableChild);
        this.refreshWidgetPositions();
    }

    @Override
    protected void refreshWidgetPositions() {
        SimplePositioningWidget.setPos(this.grid, this.getNavigationFocus());
    }
}