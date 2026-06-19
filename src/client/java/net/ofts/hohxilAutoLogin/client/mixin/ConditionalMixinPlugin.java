package net.ofts.hohxilAutoLogin.client.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ConditionalMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.equals("net.ofts.hohxilAutoLogin.client.mixin.IPNBlocker")) return true;
        boolean loaded = FabricLoader.getInstance().isModLoaded("inventoryprofilesnext");
        LoggerFactory.getLogger("ConditionalMixin").info(loaded ? "IPN Detected, Loading Mixin" : "IPN Not Installed");
        return loaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
