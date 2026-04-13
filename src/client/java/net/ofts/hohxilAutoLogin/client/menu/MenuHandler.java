package net.ofts.hohxilAutoLogin.client.menu;

import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.ofts.hohxilAutoLogin.client.AutoLoginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record MenuHandler(int id, String name, String menuMatcher, SlotHandler slotHandler, Runnable opener, Consumer<NonNullList<Slot>> callback, boolean clickTwice) {

    public static final Logger LOGGER = LoggerFactory.getLogger("Auto Clicker");

    public static void NOTHING(NonNullList<Slot> ignored) {}

    public void handleMenu(AbstractContainerScreen<?> screen){
        NonNullList<Slot> inventory = screen.getMenu().slots;

        int slot = slotHandler.getSlots(inventory);

        if (slot != -1) sendClick(screen.getMenu(), slot);

        if (clickTwice){
            AutoLoginConfig config = AutoLoginConfig.get();
            try {
                Thread.sleep(config.clickDelay);
            } catch (InterruptedException ignored) {}

            sendClick(screen.getMenu(), slot);
        }

        callback.accept(inventory);
    }

    public static void sendClick(AbstractContainerMenu menu, int slot){
        LOGGER.info("sending click @ slot={} to menu {}", slot, menu.containerId);

        Minecraft client = Minecraft.getInstance();
        int syncId = menu.containerId;
        int stateId = menu.getStateId();

        if (client.getConnection() == null) return;
        HashedStack carriedItem = HashedStack.create(menu.getCarried(), client.getConnection().decoratedHashOpsGenenerator());

        client.getConnection().send(new ServerboundContainerClickPacket(syncId, stateId, (short) slot, (byte) 0, ClickType.PICKUP, new Int2ObjectOpenHashMap<>(), carriedItem));
    }
}
