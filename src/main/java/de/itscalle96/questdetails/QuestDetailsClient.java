package de.itscalle96.questdetails;

import de.itscalle96.questdetails.ui.Seitenleiste;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

/** Hängt die Seitenleiste an jeden Fortschrittsbildschirm (Vanilla oder Better Advancements). */
public final class QuestDetailsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((mc, screen, breite, hoehe) -> {
			if (Seitenleiste.istQuestbuch(screen)) {
				Seitenleiste.anmelden(screen);
			}
		});
	}
}
