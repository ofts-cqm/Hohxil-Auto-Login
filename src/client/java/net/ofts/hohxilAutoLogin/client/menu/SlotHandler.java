package net.ofts.hohxilAutoLogin.client.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;

@FunctionalInterface
public interface SlotHandler {
    int getSlots(NonNullList<Slot> inventory);
}
