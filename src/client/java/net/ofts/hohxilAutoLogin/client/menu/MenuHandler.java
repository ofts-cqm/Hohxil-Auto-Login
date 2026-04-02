package net.ofts.hohxilAutoLogin.client.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public record MenuHandler(int id, String menuMatcher, SlotHandler slotHandler, Runnable opener) {

    public void handleMenu(HandledScreen<?> screen){
        List<Integer> slots = slotHandler.getSlots(screen.getScreenHandler().slots);
        MinecraftClient client = MinecraftClient.getInstance();
        int syncId = screen.getScreenHandler().syncId;
        assert client.interactionManager != null;

        for (int slot : slots){

            client.interactionManager.clickSlot(
                    syncId,
                    slot,
                    0, // left click
                    SlotActionType.PICKUP,
                    client.player
            );
        }
    }
}
