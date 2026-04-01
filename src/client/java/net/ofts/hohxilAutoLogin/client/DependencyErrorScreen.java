package net.ofts.hohxilAutoLogin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class DependencyErrorScreen extends Screen {

    private final Text message;

    public DependencyErrorScreen() {
        super(Text.literal("错误：缺失前置模组"));
        this.message = Text.literal("如果想使用模组菜单，您必须安装Cloth Config");
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(
                Text.literal("返回菜单"),
                btn -> this.client.setScreen(new TitleScreen())
        ).dimensions(this.width / 2 - 100, this.height / 2 + 20, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                textRenderer,
                this.title,
                this.width / 2,
                this.height / 2 - 40,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                message,
                this.width / 2,
                this.height / 2 - 10,
                0xFF5555
        );

        super.render(context, mouseX, mouseY, delta);
    }
}