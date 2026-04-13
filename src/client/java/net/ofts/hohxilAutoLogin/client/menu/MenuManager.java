package net.ofts.hohxilAutoLogin.client.menu;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.ofts.hohxilAutoLogin.client.AutoLoginConfig;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MenuManager {
    public static final int SERVER_CHOOSER = 0;
    public static final int CHECK_IN = 1;
    public static final int AFK_REWARD = 2;
    @Deprecated
    public static final int MAIN_MENU = 3;
    public static final int REFRESH_TITLE = 4;

    private static final int TYPE_COUNT = 5;

    private static final MenuHandler[] handlers = new MenuHandler[TYPE_COUNT];
    private static final Task[] taskQueue = new Task[TYPE_COUNT];
    private static final AbstractContainerScreen<?>[] arrivedList = new AbstractContainerScreen<?>[TYPE_COUNT];
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

    public static boolean checkMenu(String name){
        for (MenuHandler handler : handlers){
            if (handler.name().equals(name)){
                checkMenu(handler.id());
                return true;
            }
        }
        return false;
    }

    private static void auditTask(){
        if (Minecraft.getInstance().getCurrentServer() == null) return;

        for (int i = 0; i < TYPE_COUNT; i++) {
            Task task = taskQueue[i];
            if (task == null) continue;
            if (task.time.isAfter(LocalDateTime.now().plusNanos(config.openMenuDelay * 1000L))) {
                if (task.tried >= config.retryCount) taskQueue[i] = null;
                else task.open();
            }
        }
    }

    public static synchronized boolean handleMenu(AbstractContainerScreen<?> menu){
        for (MenuHandler handler : handlers){
            if (handler == null || !menu.getTitle().getString().contains(handler.menuMatcher())) continue;

            int id = handler.id();
            if (arrivedList[id] != null || taskQueue[id] == null) continue;
            arrivedList[id] = menu;
            taskQueue[id] = null;

            new Thread(MenuManager::thread).start();
            return true;
        }

        return false;
    }

    public static CompletableFuture<Suggestions> getSuggestion(SuggestionsBuilder builder){
        for (MenuHandler handler : handlers) builder.suggest(handler.name());
        return builder.buildFuture();
    }

    private static int anyArrived(){
        for (int i = 0; i < arrivedList.length; i++){
            if (arrivedList[i] != null) return i;
        }
        return -1;
    }

    private static void thread() {
        int arrivedTask = anyArrived();

        AbstractContainerScreen<?> menu = arrivedList[arrivedTask];
        arrivedList[arrivedTask] = null;

        try {
            Thread.sleep(config.clickDelay);
        } catch (InterruptedException ignored) {}

        handlers[arrivedTask].handleMenu(menu);

        closeMenu(menu);
    }

    private static void closeMenu(AbstractContainerScreen<?> menu){
        Minecraft client = Minecraft.getInstance();

        client.execute(() -> {
            if (config.hideMenu){
                menu.onClose();
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
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(MenuManager::auditTask, 0, config.openMenuDelay, TimeUnit.MILLISECONDS);

        handlers[SERVER_CHOOSER] = new MenuHandler(SERVER_CHOOSER, "choose_server", "进入游玩",
                (_) -> getSlotForTarget(config.targetServer),
                MenuManager::openServerSelectionMenu,
                MenuHandler::NOTHING,
                false
        );

        handlers[CHECK_IN] = new MenuHandler(CHECK_IN, "checkin", "签到菜单",
                (a) -> getSlotWith(a, Items.YELLOW_TERRACOTTA),
                () -> openCommandMenu("签到"),
                MenuHandler::NOTHING,
                false
        );

        handlers[AFK_REWARD] = new MenuHandler(AFK_REWARD, "claim_reward","在线奖励",
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
                },
                false
        );

        /*handlers[MAIN_MENU] = new MenuHandler(MAIN_MENU, "", "主菜单",
                (a) -> getSlotWith(a, Items.PAPER),
                () -> openCommandMenu("cd"),
                (_) -> checkMenu(AFK_REWARD),
                false
        );*/

        handlers[REFRESH_TITLE] = new MenuHandler(REFRESH_TITLE, "refresh_title", "称号仓库",
                (a) -> getSlotNamed(a, config.titleToRefresh),
                () -> openCommandMenu("ch"),
                MenuHandler::NOTHING,
                true
        );
    }

    private static int getSlotNamed(NonNullList<Slot> inventory, String name){
        for (int i = 0; i < inventory.size(); i++){
            Slot slot = inventory.get(i);
            if (slot.getItem().getDisplayName().getString().contains(name)) return i;
        }

        return -1;
    }

    private static int getSlotWith(NonNullList<Slot> inventory, Item item){
        for (int i = 0; i < inventory.size(); i++){
            Slot slot = inventory.get(i);
            if (slot.getItem().is(item)) return i;
        }

        return -1;
    }

    private static void openCommandMenu(String command){
        if (command.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.getConnection() != null) {
                client.getConnection().sendCommand(command);
            }
        });
    }

    private static void openServerSelectionMenu(){
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player == null) return;

            var stack = client.player.getInventory().getItem(4);

            if (!stack.isEmpty()) {
                client.player.getInventory().setSelectedSlot(4);
                assert client.gameMode != null;
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            }
        });
    }
}
