package net.ofts.hohxilAutoLogin.client.mixin;

import net.minecraft.client.gui.screen.ingame.BookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BookScreen.class)
public interface BookScreenMixin {
    @Accessor("contents")
    BookScreen.Contents getContents();
}
