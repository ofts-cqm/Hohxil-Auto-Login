package net.ofts.hohxilAutoLogin.client.menu;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public record MenuHandler(int id, String menuMatcher, SlotHandler slotHandler, Runnable opener, Consumer<NonNullList<Slot>> callback) {

    public static void NOTHING(NonNullList<Slot> ignored) {}

    public void handleMenu(AbstractContainerScreen<?> screen){
        NonNullList<Slot> inventory = screen.getMenu().slots;

        int slot = slotHandler.getSlots(inventory);

        if (slot != -1){
            Minecraft client = Minecraft.getInstance();
            int syncId = screen.getMenu().containerId;
            int stateId = screen.getMenu().getStateId();

            if (client.getConnection() == null) return;
            HashedStack carriedItem = HashedStack.create(screen.getMenu().getCarried(), client.getConnection().decoratedHashOpsGenenerator());

            client.getConnection().send(new ServerboundContainerClickPacket(syncId, stateId, (short) slot, (byte) 0, ClickType.PICKUP, new Int2ObjectOpenHashMap<>(), carriedItem));
        }

        callback.accept(inventory);
    }
}
