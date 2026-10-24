package de.itscalle96.questdetails.mixin;

import de.itscalle96.questdetails.ui.Seitenleiste;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla-Fortschrittsbildschirm: extractHover läuft nur für die Quest unter der Maus. */
@Mixin(AdvancementWidget.class)
abstract class AdvancementWidgetMixin {
	@Shadow
	public abstract AdvancementHolder getAdvancement();

	@Inject(method = "extractHover", at = @At("HEAD"), cancellable = true)
	private void questdetails$hover(GuiGraphicsExtractor g, int x, int y, float fade, int a, int b, int c, CallbackInfo ci) {
		if (!Seitenleiste.hover(getAdvancement())) {
			ci.cancel();
		}
	}
}
