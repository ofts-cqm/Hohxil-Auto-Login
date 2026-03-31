package net.ofts.hohxilAutoLogin.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ModMenuAPIImpl implements ModMenuApi {
    private static final AutoLoginConfig config = AutoLoginConfig.get();

    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (ConfigScreenFactory<Screen>) parent ->{
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("可可西里自动登录"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            builder.getOrCreateCategory(Text.literal("基础设置"))
                    .addEntry(
                            entryBuilder.startStrField(Text.literal("用户密码"), config.password)
                                    .setDefaultValue("")
                                    .setTooltip(Text.literal("登陆时的密码"))
                                    .setSaveConsumer(ModMenuAPIImpl::setPassword)
                                    .build()
                    )
                    .addEntry(
                            entryBuilder.startEnumSelector(Text.literal("登陆子服"), AutoLoginConfig.TargetServer.class, config.targetServer)
                                    .setDefaultValue(AutoLoginConfig.TargetServer.SURVIVAL)
                                    .setTooltip(Text.literal("登陆时选择的子服务器"))
                                    .setEnumNameProvider(ModMenuAPIImpl::getName)
                                    .setSaveConsumer(ModMenuAPIImpl::setServer)
                                    .build()
                    )
                    .addEntry(
                            entryBuilder.startStrField(Text.literal("服务器ip"), config.address)
                                    .setDefaultValue("cko.cc")
                                    .setTooltip(Text.literal("针对的服务器ip"))
                                    .setSaveConsumer(ModMenuAPIImpl::setAddress)
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
                                    .setSaveConsumer(ModMenuAPIImpl::setBlacklist)
                                    .build()
                    )
                    .addEntry(
                            entryBuilder
                                    .startStrList(Text.literal("指令列表"), config.customCommands)
                                    .setTooltip(Text.literal("除了登陆指令以外，重连时发送的指令列表"))
                                    .setDefaultValue(new ArrayList<>())
                                    .setSaveConsumer(ModMenuAPIImpl::setCommandList)
                                    .build()
                    )
                    .addEntry(generateDelayEntry(entryBuilder, "发送指令延迟", "登陆后延迟多久发送自定义指令", 6))
                    .setDescription(new StringVisitable[] { StringVisitable.plain("所有时间单位均为毫秒，所有次数单位均为次") });

            return builder.build();
        };
    }

    public AbstractConfigListEntry<Integer> generateDelayEntry(ConfigEntryBuilder entryBuilder, String text, String tooltip, int index){
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

    public static void setCommandList(List<String> address) {
        config.customCommands = address;
        AutoLoginConfig.save();
    }

    public static void setBlacklist(List<String> address) {
        address = address.stream().map(str -> {
            String str2 = str.trim();
            if (str2.startsWith("/")) str2 = str2.substring(1);
            return str2;
        }).toList();
        config.reconnectionFilter = address;
        AutoLoginConfig.save();
    }

    public static void setAddress(String address) {
        config.address = address;
        AutoLoginConfig.save();
    }

    public static void setServer(AutoLoginConfig.TargetServer targetServer) {
        config.targetServer = targetServer;
        AutoLoginConfig.save();
    }

    public static void setPassword(String password) {
        config.password = password;
        AutoLoginConfig.save();
    }
}
