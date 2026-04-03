package net.ofts.hohxilAutoLogin.client.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.function.Consumer;

public record MenuHandler(int id, String menuMatcher, SlotHandler slotHandler, Runnable opener, Consumer<DefaultedList<Slot>> callback) {

    public static void NOTHING(DefaultedList<Slot> ignored) {}

    public void handleMenu(HandledScreen<?> screen){
        DefaultedList<Slot> inventory = screen.getScreenHandler().slots;

        int slot = slotHandler.getSlots(inventory);

        if (slot != -1){
            MinecraftClient client = MinecraftClient.getInstance();
            int syncId = screen.getScreenHandler().syncId;
            assert client.interactionManager != null;

            client.interactionManager.clickSlot(
                    syncId,
                    slot,
                    0, // left click
                    SlotActionType.PICKUP,
                    client.player
            );
        }

        callback.accept(inventory);
    }
}
