package de.itscalle96.questdetails.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Wendet den Better-Advancements-Mixin nur an, wenn die Mod installiert ist; sonst bleibt nur Vanilla. */
public final class MixinPlugin implements IMixinConfigPlugin {
	@Override
	public boolean shouldApplyMixin(String zielKlasse, String mixinKlasse) {
		if (mixinKlasse.endsWith(".BetterAdvancementWidgetMixin")) {
			return FabricLoader.getInstance().isModLoaded("betteradvancements");
		}
		return true;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> eigene, Set<String> andere) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String zielKlasse, ClassNode ziel, String mixinKlasse, IMixinInfo info) {
	}

	@Override
	public void postApply(String zielKlasse, ClassNode ziel, String mixinKlasse, IMixinInfo info) {
	}
}
