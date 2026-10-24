package de.itscalle96.questdetails.mixin;

import betteradvancements.common.gui.BetterAdvancementWidget;
import de.itscalle96.questdetails.ui.Seitenleiste;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Better Advancements: drawHover läuft nur für die Quest unter der Maus. Einziger Berührungspunkt mit der Mod
 * (öffentliche Methode); fehlt sie, überspringt {@link MixinPlugin} diesen Mixin.
 */
@Mixin(value = BetterAdvancementWidget.class, remap = false)
abstract class BetterAdvancementWidgetMixin {
	@Inject(method = "drawHover", at = @At("HEAD"), cancellable = true, require = 0)
	private void questdetails$hover(GuiGraphicsExtractor g, int x, int y, float fade, int a, int b, float zoom, CallbackInfo ci) {
		if (!Seitenleiste.hover(((BetterAdvancementWidget) (Object) this).getAdvancement().holder())) {
			ci.cancel();
		}
	}
}
