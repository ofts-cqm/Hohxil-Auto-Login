package net.ofts.hohxilAutoLogin.client.configUI;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.ofts.hohxilAutoLogin.client.AutoLoginConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfigScreen {
    private static final AutoLoginConfig config = AutoLoginConfig.get();

    public static Screen getConfig(Screen parent){
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("可可西里自动登录"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        builder.getOrCreateCategory(Component.literal("基础设置"))
                .addEntry(
                        entryBuilder.startStrField(Component.literal("用户密码"), config.password)
                                .setDefaultValue("")
                                .setTooltip(Component.literal("登陆时的密码"))
                                .setSaveConsumer(ConfigScreen::setPassword)
                                .build()
                )
                .addEntry(
                        entryBuilder.startEnumSelector(Component.literal("登陆子服"), AutoLoginConfig.TargetServer.class, config.targetServer)
                                .setDefaultValue(AutoLoginConfig.TargetServer.SURVIVAL)
                                .setTooltip(Component.literal("登陆时选择的子服务器"))
                                .setEnumNameProvider(ConfigScreen::getName)
                                .setSaveConsumer(ConfigScreen::setServer)
                                .build()
                )
                .addEntry(
                        entryBuilder.startStrField(Component.literal("服务器ip"), config.address)
                                .setDefaultValue("cko.cc")
                                .setTooltip(Component.literal("针对的服务器ip"))
                                .setSaveConsumer(ConfigScreen::setAddress)
                                .build()
                )
                .addEntry(
                        entryBuilder.startBooleanToggle(Component.literal("自动关闭公告"), config.closeAnnouncement)
                                .setDefaultValue(true)
                                .setTooltip(Component.literal("当进入游戏后，是否自动关闭公告"))
                                .setSaveConsumer(ConfigScreen::setAnnouncement)
                                .build()
                )
                .addEntry(
                        entryBuilder.startBooleanToggle(Component.literal("自动欢迎新人"), config.autoGreeting)
                                .setDefaultValue(true)
                                .setTooltip(Component.literal("当有新人加入时，自动欢迎新人"))
                                .setSaveConsumer(a -> config.autoGreeting = a)
                                .build()
                )
                .addEntry(
                        entryBuilder.startBooleanToggle(Component.literal("自动进入游戏"), config.autoConnect)
                                .setDefaultValue(false)
                                .setTooltip(Component.literal("打开游戏后自动加入服务器"))
                                .setSaveConsumer(ConfigScreen::setAutoConnect)
                                .build()
                )
                .addEntry(
                        entryBuilder.startBooleanToggle(Component.literal("自动签到"), config.autoCheckin)
                                .setDefaultValue(true)
                                .setTooltip(Component.literal("进入游戏后自动签到"))
                                .setSaveConsumer(a -> config.autoCheckin = a)
                                .build()
                )
                .addEntry(
                        entryBuilder.startBooleanToggle(Component.literal("自动续称号"), config.refreshTitle)
                                .setDefaultValue(false)
                                .setTooltip(Component.literal("是否在登录后自动续称号"))
                                .setSaveConsumer(a -> config.refreshTitle = a)
                                .build()
                );
        builder.getOrCreateCategory(Component.literal("高级"))
                .addEntry(generateDelayEntry(entryBuilder, "重连延时", "每次重连时等待的时间", 0))
                .addEntry(generateDelayEntry(entryBuilder, "登陆延时", "进入游戏后多久发送登陆指令", 1))
                .addEntry(generateDelayEntry(entryBuilder, "打开菜单延迟","登陆后多久发送打开菜单指令", 2))
                .addEntry(generateDelayEntry(entryBuilder, "重试次数", "打开菜单失败后重试次数, 0=无限次", 3))
                .addEntry(generateDelayEntry(entryBuilder, "点击延迟", "打开菜单后多久选择服务器", 4))
                .addEntry(generateDelayEntry(entryBuilder, "重连次数", "尝试重连的次数，0=无限次", 5))
                .addEntry(
                        entryBuilder
                                .startStrList(Component.literal("重连黑名单"), config.reconnectionFilter)
                                .setTooltip(Component.literal("当遇到以下断连信息时，放弃重连"))
                                .setDefaultValue(new ArrayList<>())
                                .setSaveConsumer(ConfigScreen::setBlacklist)
                                .build()
                )
                .addEntry(
                        entryBuilder
                                .startStrList(Component.literal("登录指令列表"), config.customCommands)
                                .setTooltip(Component.literal("除了登陆指令以外，重连时发送的指令列表"))
                                .setDefaultValue(new ArrayList<>())
                                .setSaveConsumer(ConfigScreen::setCommandList)
                                .build()
                )
                .addEntry(
                        entryBuilder
                                .startStrList(Component.literal("服务器指令列表"), config.customCommandsAfterServer)
                                .setTooltip(Component.literal("进入服务器（比如生存服）之后发送的指令"))
                                .setDefaultValue(new ArrayList<>())
                                .setSaveConsumer(ConfigScreen::setCommandListAfterServer)
                                .build()
                )
                .addEntry(generateDelayEntry(entryBuilder, "发送指令延迟", "登陆后延迟多久发送自定义指令", 6))
                .addEntry(
                        entryBuilder.startStrList(Component.literal("自动欢迎消息"), config.greetingMessageList)
                                .setDefaultValue(List.of("欢迎欢迎~新人记得res tp XCValkryia.Gallery，这里是服务器最大最全的地图画商店，超多地图画保证您的满意~"))
                                .setTooltip(Component.literal("如果开启自动欢迎，发送的消息"))
                                .setSaveConsumer(a -> config.greetingMessageList = a)
                                .build()
                )
                .addEntry(generateDelayEntry(entryBuilder, "欢迎消息间隔", "每次发送消息时间隔多久", 7))
                .addEntry(
                        entryBuilder.startBooleanToggle(Component.literal("隐藏自动菜单"), config.hideMenu)
                                .setDefaultValue(false)
                                .setTooltip(Component.literal("当模组执行签到，领取在线奖励，进入服务器等自动操作时，隐藏对应的箱子菜单。该选项可能会造成bug"))
                                .setSaveConsumer(a -> config.hideMenu = a)
                                .build()
                )
                .addEntry(
                        entryBuilder.startEnumSelector(
                                    Component.literal("欢迎消息发送方法"), GreetingType.class, GreetingType.from(config.sequential)
                                )
                                .setDefaultValue(GreetingType.SEQUENTIAL)
                                .setSaveConsumer(a -> config.sequential = a.value)
                                .setTooltipSupplier(ConfigScreen::getGreetingTooltip)
                                .setEnumNameProvider(ConfigScreen::getGreetingMethodName)
                                .build()
                )
                .addEntry(
                        entryBuilder.startStrField(Component.literal("称号名称"), config.titleToRefresh)
                                .setDefaultValue("")
                                .setTooltip(Component.literal("登录后自动续的称号"))
                                .setSaveConsumer(a -> config.titleToRefresh = a)
                                .build()
                );

        builder.setSavingRunnable(AutoLoginConfig::save);
        return builder.build();
    }

    private static Component getGreetingMethodName(Enum<GreetingType> val){
        return Component.literal(val.name().equals("SEQUENTIAL") ? "顺序发送" : "随机发送");
    }

    private static Optional<Component[]> getGreetingTooltip(GreetingType type){
        return Optional.of(new Component[]{ Component.nullToEmpty(
                type.value ? "每次按顺序发送列表内全部内容" : "每次随机从列表内提取一条发送"
        )});
    }

    public enum GreetingType{
        SEQUENTIAL(true),
        RANDOM(false);

        GreetingType(boolean value) {
            this.value = value;
        }

        static GreetingType from(boolean value){
            return value ? SEQUENTIAL : RANDOM;
        }

        final boolean value;
    }

    public static AbstractConfigListEntry<Integer> generateDelayEntry(ConfigEntryBuilder entryBuilder, String text, String tooltip, int index){
        return entryBuilder.startIntField(Component.literal(text), config.getDelay(index))
                .setDefaultValue(config.getDefault(index))
                .setMin(index == 3 || index == 5 ? 0 : 100)
                .setMax(index == 3 || index == 5 ? 100 : 10000)
                .setTooltip(Component.literal(tooltip))
                .setSaveConsumer(i -> config.setDelay(i, index))
                .build();
    }

    public static Component getName(Enum<AutoLoginConfig.TargetServer> raw) {
        return switch (raw.name()) {
            case "SURVIVAL" -> Component.literal("生存服");
            case "REDSTONE" -> Component.literal("生电服");
            case "MINIGAMES" -> Component.literal("小游戏");
            case "NONE" -> Component.literal("暂不选择");
            default -> Component.literal(raw.name());
        };
    }

    public static void setAutoConnect(boolean closeAnnouncement){
        config.autoConnect = closeAnnouncement;
    }

    public static void setAnnouncement(boolean closeAnnouncement){
        config.closeAnnouncement = closeAnnouncement;
    }

    public static void setCommandListAfterServer(List<String> address) {
        address = address.stream().map(str -> {
            String str2 = str.trim();
            if (str2.startsWith("/")) str2 = str2.substring(1);
            return str2;
        }).toList();
        config.customCommandsAfterServer = address;
    }

    public static void setCommandList(List<String> address) {
        address = address.stream().map(str -> {
            String str2 = str.trim();
            if (str2.startsWith("/")) str2 = str2.substring(1);
            return str2;
        }).toList();
        config.customCommands = address;
    }

    public static void setBlacklist(List<String> address) {
        config.reconnectionFilter = address;
    }

    public static void setAddress(String address) {
        config.address = address;
    }

    public static void setServer(AutoLoginConfig.TargetServer targetServer) {
        config.targetServer = targetServer;
    }

    public static void setPassword(String password) {
        config.password = password;
    }
}