package net.ofts.hohxilAutoLogin.client.configUI;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.ofts.hohxilAutoLogin.client.AutoLoginConfig;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen {
    private static final AutoLoginConfig config = AutoLoginConfig.get();

    public static Screen getConfig(Screen parent){
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("可可西里自动登录"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        builder.getOrCreateCategory(Text.literal("基础设置"))
                .addEntry(
                        entryBuilder.startStrField(Text.literal("用户密码"), config.password)
                                .setDefaultValue("")
                                .setTooltip(Text.literal("登陆时的密码"))
                                .setSaveConsumer(ConfigScreen::setPassword)
                                .build()
                )
                .addEntry(
                        entryBuilder.startEnumSelector(Text.literal("登陆子服"), AutoLoginConfig.TargetServer.class, config.targetServer)
                                .setDefaultValue(AutoLoginConfig.TargetServer.SURVIVAL)
                                .setTooltip(Text.literal("登陆时选择的子服务器"))
                                .setEnumNameProvider(ConfigScreen::getName)
                                .setSaveConsumer(ConfigScreen::setServer)
                                .build()
                )
                .addEntry(
                        entryBuilder.startStrField(Text.literal("服务器ip"), config.address)
                                .setDefaultValue("cko.cc")
                                .setTooltip(Text.literal("针对的服务器ip"))
                                .setSaveConsumer(ConfigScreen::setAddress)
                                .build()
                ).addEntry(
                        entryBuilder.startBooleanToggle(Text.literal("自动关闭公告"), config.closeAnnouncement)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal("当进入游戏后，是否自动关闭公告"))
                                .setSaveConsumer(ConfigScreen::setAnnouncement)
                                .build()
                ).addEntry(
                        entryBuilder.startBooleanToggle(Text.literal("自动欢迎新人"), config.autoGreeting)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal("当有新人加入时，自动欢迎新人"))
                                .setSaveConsumer(a -> config.autoGreeting = a)
                                .build()
                ).addEntry(
                        entryBuilder.startBooleanToggle(Text.literal("自动进入游戏"), config.autoConnect)
                                .setDefaultValue(false)
                                .setTooltip(Text.literal("打开游戏后自动加入服务器"))
                                .setSaveConsumer(ConfigScreen::setAutoConnect)
                                .build()
                ).addEntry(
                        entryBuilder.startBooleanToggle(Text.literal("自动签到"), config.autoCheckin)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal("进入游戏后自动签到"))
                                .setSaveConsumer(a -> config.autoCheckin = a)
                                .build()
                );
        builder.getOrCreateCategory(Text.literal("高级"))
                .addEntry(generateDelayEntry(entryBuilder, "重连延时", "每次重连时等待的时间", 0))
                .addEntry(generateDelayEntry(entryBuilder, "登陆延时", "进入游戏后多久发送登陆指令", 1))
                .addEntry(generateDelayEntry(entryBuilder, "打开菜单延迟","登陆后多久发送打开菜单指令", 2))
                .addEntry(generateDelayEntry(entryBuilder, "重试次数", "打开菜单失败后重试次数, 0=无限次", 3))
                .addEntry(generateDelayEntry(entryBuilder, "点击延迟", "打开菜单后多久选择服务器", 4))
                .addEntry(generateDelayEntry(entryBuilder, "重连次数", "尝试重连的次数，0=无限次", 5))
                .addEntry(
                        entryBuilder
                                .startStrList(Text.literal("重连黑名单"), config.reconnectionFilter)
                                .setTooltip(Text.literal("当遇到以下断连信息时，放弃重连"))
                                .setDefaultValue(new ArrayList<>())
                                .setSaveConsumer(ConfigScreen::setBlacklist)
                                .build()
                )
                .addEntry(
                        entryBuilder
                                .startStrList(Text.literal("登录指令列表"), config.customCommands)
                                .setTooltip(Text.literal("除了登陆指令以外，重连时发送的指令列表"))
                                .setDefaultValue(new ArrayList<>())
                                .setSaveConsumer(ConfigScreen::setCommandList)
                                .build()
                )
                .addEntry(
                        entryBuilder
                                .startStrList(Text.literal("服务器指令列表"), config.customCommandsAfterServer)
                                .setTooltip(Text.literal("进入服务器（比如生存服）之后发送的指令"))
                                .setDefaultValue(new ArrayList<>())
                                .setSaveConsumer(ConfigScreen::setCommandListAfterServer)
                                .build()
                )
                .addEntry(generateDelayEntry(entryBuilder, "发送指令延迟", "登陆后延迟多久发送自定义指令", 6))
                .addEntry(
                        entryBuilder.startStrList(Text.literal("自动欢迎消息"), config.greetingMessageList)
                                .setDefaultValue(List.of("欢迎欢迎~新人记得res tp XCValkryia.Gallery，这里是服务器最大最全的地图画商店，超多地图画保证您的满意~"))
                                .setTooltip(Text.literal("如果开启自动欢迎，发送的消息"))
                                .setSaveConsumer(a -> config.greetingMessageList = a)
                                .build()
                ).addEntry(generateDelayEntry(entryBuilder, "欢迎消息间隔", "每次发送消息时间隔多久", 7))
                .addEntry(
                        entryBuilder.startBooleanToggle(Text.literal("隐藏自动菜单"), config.hideMenu)
                                .setDefaultValue(false)
                                .setTooltip(Text.literal("当模组执行签到，领取在线奖励，进入服务器等自动操作时，隐藏对应的箱子菜单。该选项可能会造成bug"))
                                .setSaveConsumer(a -> config.hideMenu = a)
                                .build()
                );

        builder.setSavingRunnable(AutoLoginConfig::save);
        return builder.build();
    }

    public static AbstractConfigListEntry<Integer> generateDelayEntry(ConfigEntryBuilder entryBuilder, String text, String tooltip, int index){
        return entryBuilder.startIntField(Text.literal(text), config.getDelay(index))
                .setDefaultValue(config.getDefault(index))
                .setMin(index == 3 || index == 5 ? 0 : 100)
                .setMax(index == 3 || index == 5 ? 100 : 10000)
                .setTooltip(Text.literal(tooltip))
                .setSaveConsumer(i -> config.setDelay(i, index))
                .build();
    }

    public static Text getName(Enum<AutoLoginConfig.TargetServer> raw) {
        return switch (raw.name()) {
            case "SURVIVAL" -> Text.literal("生存服");
            case "REDSTONE" -> Text.literal("生电服");
            case "MINIGAMES" -> Text.literal("小游戏");
            case "NONE" -> Text.literal("暂不选择");
            default -> Text.literal(raw.name());
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