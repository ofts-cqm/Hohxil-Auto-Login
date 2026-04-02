package net.ofts.hohxilAutoLogin.client.menu;

import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

@FunctionalInterface
public interface SlotHandler {
    List<Integer> getSlots(DefaultedList<Slot> inventory);
}
