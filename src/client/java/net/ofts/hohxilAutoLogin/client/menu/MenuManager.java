package net.ofts.hohxilAutoLogin.client.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.ofts.hohxilAutoLogin.client.AutoLoginConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuManager {
    public static final int SERVER_CHOOSER = 0;
    public static final int CHECK_IN = 1;
    public static final int AFK_REWARD = 2;

    private static final int TYPE_COUNT = 3;

    private static final MenuHandler[] handlers = new MenuHandler[TYPE_COUNT];
    private static final Task[] taskQueue = new Task[TYPE_COUNT];
    private static final HandledScreen<?>[] arrivedList = new HandledScreen<?>[TYPE_COUNT];
    private static final Thread worker;
    private static final MenuManager instance = new MenuManager();

    private static final Object lock = new Object();

    private static final AutoLoginConfig config = AutoLoginConfig.get();

    public static void clearTaskQueue() {
        Arrays.fill(taskQueue, null);
        Arrays.fill(arrivedList, null);
    }

    public static void checkMenu(int id){
        if (taskQueue[id] != null) return;
        taskQueue[id] = new Task(id);
        taskQueue[id].open();
    }

    public static synchronized boolean handleMenu(HandledScreen<?> menu){
        for (MenuHandler handler : handlers){
            if (handler != null && menu.getTitle().getString().contains(handler.menuMatcher())){
                int id = handler.id();

                if (arrivedList[id] == null){
                    arrivedList[id] = menu;
                    taskQueue[id] = null;
                    synchronized (lock) {
                        lock.notify();
                    }
                }

                for (int i = 0; i < TYPE_COUNT; i++) {
                    Task task = taskQueue[i];
                    if (task == null) continue;
                    if (task.time.isAfter(LocalDateTime.now().plusNanos(config.openMenuDelay * 1000L))) {
                        if (task.tried >= config.retryCount){
                            taskQueue[id] = null;
                        } else task.open();
                    }
                }

                return true;
            }
        }

        return false;
    }

    private int anyArrived(){
        for (int i = 0; i < arrivedList.length; i++){
            if (arrivedList[i] != null) return i;
        }
        return -1;
    }

    private void thread() {
        while (true){
            int arrivedTask = anyArrived();

            try {
                while (arrivedTask == -1){
                    synchronized (lock) {
                        lock.wait(); // Thread now owns the monitor of 'lock'
                    }
                    arrivedTask = anyArrived();
                }
            } catch (InterruptedException ignored) {}

            HandledScreen<?> menu = arrivedList[arrivedTask];
            arrivedList[arrivedTask] = null;

            handlers[arrivedTask].handleMenu(menu);

            if (MinecraftClient.getInstance().currentScreen == menu) MinecraftClient.getInstance().setScreen(null);
        }
    }

    private static int getSlotForTarget(AutoLoginConfig.TargetServer target) {
        return switch (target) {
            case SURVIVAL -> 11;
            case REDSTONE -> 13;
            case MINIGAMES -> 15;
            case NONE -> -1;
        };
    }

    private static class Task {
        public int tried;
        public int id;
        public LocalDateTime time;

        public Task(int id) {
            this.id = id;
            this.tried = 0;
            time = LocalDateTime.now();
        }

        public void open() {
            handlers[id].opener().run();
        }
    }

    static {
        worker = new Thread(instance::thread);
        worker.start();

        handlers[SERVER_CHOOSER] = new MenuHandler(SERVER_CHOOSER, "进入游玩",
                (a) -> List.of(getSlotForTarget(config.targetServer)),
                MenuManager::openServerSelectionMenu
        );

        handlers[CHECK_IN] = new MenuHandler(CHECK_IN, "签到菜单",
                (a) -> getSlotWith(a, Items.YELLOW_TERRACOTTA),
                () -> openCommandMenu("签到")
        );

        handlers[AFK_REWARD] = new MenuHandler(AFK_REWARD, "在线奖励",
                (a) -> getSlotWith(a, Items.EXPERIENCE_BOTTLE),
                () -> openCommandMenu("在线奖励"));
    }

    private static List<Integer> getSlotWith(DefaultedList<Slot> inventory, Item item){
        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < inventory.size(); i++){
            Slot slot = inventory.get(i);
            if (slot.getStack().isOf(item)) slots.add(i);
        }

        return slots;
    }

    private static void openCommandMenu(String command){
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendChatCommand(command);
            }
        });
    }

    private static void openServerSelectionMenu(){
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player == null) return;

            var stack = client.player.getInventory().getStack(4);

            if (!stack.isEmpty()) {
                client.player.getInventory().setSelectedSlot(4);
                assert client.interactionManager != null;
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            }
        });
    }
}
