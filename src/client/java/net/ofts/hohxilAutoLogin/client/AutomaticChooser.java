package net.ofts.hohxilAutoLogin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.SlotActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticChooser {
    public static final Logger LOGGER = LoggerFactory.getLogger("HohxilAutoLogin - Menu Choser");

    public static void choose(HandledScreen<?> screen) {
        new Thread(() -> onScreen(screen)).start();
    }

    private static void onScreen(HandledScreen<?> screen) {
        try {
            Thread.sleep(AutoLoginConfig.get().clickDelay); // 3 seconds
        } catch (InterruptedException ignored) {
        }

        MinecraftClient client = MinecraftClient.getInstance();

        LOGGER.info("Menu Opened, title: {}", screen.getTitle().getString());

        if (client.player == null || client.interactionManager == null) return;

        // Optional: check GUI title (recommended)
        String title = screen.getTitle().getString();
        if (!title.contains("进入游玩")) {
            LOGGER.info("not our menu");
            return;
        }

        // Wait a tick or two (menu needs to fully load)
        client.execute(() -> {
            clickSlot(screen, getSlotForTarget(AutoLoginConfig.get().targetServer)); // Survival slot
        });

        LOGGER.info("clicked");
        HohxilAutoLoginClient.sendAfterServerCommands = true;
    }

    private static void clickSlot(HandledScreen<?> screen, int slotIndex) {
        if (slotIndex == -1) return;

        MinecraftClient client = MinecraftClient.getInstance();

        int syncId = screen.getScreenHandler().syncId;

        assert client.interactionManager != null;
        client.interactionManager.clickSlot(
                syncId,
                slotIndex,
                0, // left click
                SlotActionType.PICKUP,
                client.player
        );
    }

    private static int getSlotForTarget(AutoLoginConfig.TargetServer target) {
        return switch (target) {
            case SURVIVAL -> 11;
            case REDSTONE -> 13;
            case MINIGAMES -> 15;
            case NONE -> -1;
        };
    }
}