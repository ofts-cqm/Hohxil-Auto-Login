package net.ofts.hohxilAutoLogin.client.menu;

import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

@FunctionalInterface
public interface SlotHandler {
    int getSlots(DefaultedList<Slot> inventory);
}
