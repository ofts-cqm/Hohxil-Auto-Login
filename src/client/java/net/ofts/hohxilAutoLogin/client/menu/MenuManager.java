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
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MenuManager {
    public static final int SERVER_CHOOSER = 0;
    public static final int CHECK_IN = 1;
    public static final int AFK_REWARD = 2;
    public static final int MAIN_MENU = 3;

    private static final int TYPE_COUNT = 4;

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

    private static void auditTask(){
        for (int i = 0; i < TYPE_COUNT; i++) {
            Task task = taskQueue[i];
            if (task == null) continue;
            if (task.time.isAfter(LocalDateTime.now().plusNanos(config.openMenuDelay * 1000L))) {
                if (task.tried >= config.retryCount) taskQueue[i] = null;
                else task.open();
            }
        }
    }

    public static synchronized boolean handleMenu(HandledScreen<?> menu){
        for (MenuHandler handler : handlers){
            if (handler == null || !menu.getTitle().getString().contains(handler.menuMatcher())) continue;

            int id = handler.id();
            auditTask();

            if (arrivedList[id] != null || taskQueue[id] == null) continue;

            arrivedList[id] = menu;
            taskQueue[id] = null;

            synchronized (lock) {
                lock.notify();
            }

            return true;
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

            try {
                Thread.sleep(config.clickDelay);
            } catch (InterruptedException ignored) {}

            handlers[arrivedTask].handleMenu(menu);

            closeMenu(menu);
        }
    }

    private static void closeMenu(HandledScreen<?> menu){
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            if (config.hideMenu){
                menu.close();
            }else {
                client.setScreen(null);
            }
        });
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
                (a) -> getSlotForTarget(config.targetServer),
                MenuManager::openServerSelectionMenu,
                MenuHandler::NOTHING
        );

        handlers[CHECK_IN] = new MenuHandler(CHECK_IN, "签到菜单",
                (a) -> getSlotWith(a, Items.YELLOW_TERRACOTTA),
                () -> openCommandMenu("签到"),
                MenuHandler::NOTHING
        );

        handlers[AFK_REWARD] = new MenuHandler(AFK_REWARD, "在线奖励",
                (a) -> getSlotWith(a, Items.EXPERIENCE_BOTTLE),
                () -> openCommandMenu("zxjl"),
                (inventory) -> {
                    if (getSlotWith(inventory, Items.EXPERIENCE_BOTTLE) != -1)
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                checkMenu(AFK_REWARD);
                            }
                        }, 1000);
                }
        );

        handlers[MAIN_MENU] = new MenuHandler(MAIN_MENU, "主菜单",
                (a) -> getSlotWith(a, Items.PAPER),
                () -> openCommandMenu("cd"),
                (a) -> checkMenu(AFK_REWARD)
        );
    }

    private static int getSlotWith(DefaultedList<Slot> inventory, Item item){
        for (int i = 0; i < inventory.size(); i++){
            Slot slot = inventory.get(i);
            if (slot.getStack().isOf(item)) return i;
        }

        return -1;
    }

    private static void openCommandMenu(String command){
        if (command.isEmpty()) return;

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
