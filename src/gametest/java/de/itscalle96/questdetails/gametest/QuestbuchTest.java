package de.itscalle96.questdetails.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import de.itscalle96.questdetails.daten.Datenquelle;
import de.itscalle96.questdetails.ui.Seitenleiste;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

/**
 * Test-Client mit Better Advancements, Sodium und Iris und dem echten Questbuch aus mc-weihnachten-2026:
 * Quest per Maus suchen und anklicken, Inhalt wechseln, Klick daneben und ESC schließen. Danach Screenshots
 * auf Englisch und Deutsch, ohne und mit Shader. Bilder: build/quest-details-screenshots/.
 */
public final class QuestbuchTest implements FabricClientGameTest {
	private static final Path ZIEL = Path.of(System.getProperty("questdetails.screenshots", "screenshots-quest-details"));
	private static final String BETTER_ADVANCEMENTS = "betteradvancements.common.gui.BetterAdvancementsScreen";
	private static final Identifier WERKBANK = id("ankommen/werkbank");
	private static final Identifier EISEN = id("ankommen/eisen");
	private static final Identifier DIAMANTEN = id("bergbau/diamanten");
	private static final Identifier OHNE_DATEI = id("ankommen/start");

	@Override
	public void runTest(ClientGameTestContext ctx) {
		setzeSprache(ctx, "en_us");
		try (TestSingleplayerContext welt = ctx.worldBuilder().create()) {
			welt.getConnection().waitForChunksDownload();
			welt.getServer().runCommand("advancement grant @a everything");
			welt.getServer().runCommand("time set noon");
			ctx.waitTicks(20);
			pruefe(ctx.computeOnClient(mc -> !Datenquelle.quest(WERKBANK).istLeer()), "Questbuch-Datei für die Werkbank geladen");
			pruefe(ctx.computeOnClient(mc -> Datenquelle.quest(OHNE_DATEI).istLeer()), "für den Tab-Anfang gibt es keine Datei");

			pruefeAlleDateien(ctx);

			oeffneQuestbuch(ctx, WERKBANK);
			boolean mitBetterAdvancements = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("betteradvancements");
			String bildschirm = ctx.computeOnClient(mc -> mc.gui.screen().getClass().getName());
			pruefe(mitBetterAdvancements ? bildschirm.equals(BETTER_ADVANCEMENTS) : bildschirm.equals(AdvancementsScreen.class.getName()),
					"Questbuch-Bildschirm passt nicht: " + bildschirm);

			// 1. Echte Maus: eine Questbuch-Quest suchen (Hover-Mixin für Better Advancements) und anklicken
			Identifier erste = sucheQuest(ctx);
			pruefe(erste != null, "keine Questbuch-Quest unter der Maus gefunden – greift der Hover-Mixin?");
			klick(ctx);
			pruefe(erste.equals(offen(ctx)), "Klick öffnet die Seitenleiste mit " + erste + ", offen: " + offen(ctx));

			// 2. Andere Quest anklicken (links neben der Leiste) wechselt den Inhalt
			Identifier andere = sucheAndereQuest(ctx);
			pruefe(andere != null, "eine zweite Quest links neben der Leiste gefunden");
			klick(ctx);
			pruefe(andere.equals(offen(ctx)), "zweite Quest wechselt den Inhalt, offen: " + offen(ctx));

			// 3. Klick daneben schließt, ESC schließt nur die Leiste (Questbuch bleibt offen)
			ctx.getInput().setCursorPos(2, 2);
			ctx.waitTick();
			klick(ctx);
			pruefe(offen(ctx) == null, "Klick daneben schließt die Leiste");
			oeffne(ctx, WERKBANK);
			ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
			ctx.waitTick();
			pruefe(offen(ctx) == null, "ESC schließt die Leiste");
			pruefe(ctx.computeOnClient(mc -> mc.gui.screen() != null), "ESC schließt dabei nicht das Questbuch");

			// 4. Screenshots: Englisch, Deutsch, mit Shader
			bilder(ctx, "en");
			setzeSprache(ctx, "de_de");
			oeffneQuestbuch(ctx, WERKBANK);
			bilder(ctx, "de");
			setzeShader(ctx, true);
			oeffneQuestbuch(ctx, WERKBANK);
			oeffne(ctx, WERKBANK);
			ctx.waitTicks(50);
			foto(ctx, "werkbank_de_shader");
			setzeSprache(ctx, "en_us");
			oeffneQuestbuch(ctx, EISEN);
			oeffne(ctx, EISEN);
			ctx.waitTicks(50);
			foto(ctx, "eisen_en_shader");
			setzeShader(ctx, false);
			ctx.setScreen(() -> null);
		}
		try {
			Files.writeString(ZIEL.resolve("fertig.txt"), "alle Prüfungen bestanden\n");
		} catch (java.io.IOException fehler) {
			throw new java.io.UncheckedIOException(fehler);
		}
	}

	/**
	 * Liest alle Questbuch-Dateien so, wie die Seitenleiste es tut, und schreibt einen Bericht: Warnungen beim
	 * Lesen, unbekannte Items/Tags und Rezepte ohne Raster. Bricht den Test nicht ab – der Bericht geht an die
	 * Questbuch-Session.
	 */
	private static void pruefeAlleDateien(ClientGameTestContext ctx) {
		String bericht = ctx.computeOnClient(mc -> {
			StringBuilder sb = new StringBuilder();
			var dateien = mc.getResourceManager().listResources("questbuch",
					d -> d.getNamespace().equals(Seitenleiste.NAMESPACE) && d.getPath().endsWith(".json"));
			int schritte = 0;
			int raster = 0;
			for (var eintrag : new java.util.TreeMap<>(dateien).entrySet()) {
				java.util.List<String> probleme = new java.util.ArrayList<>();
				de.itscalle96.questdetails.kern.QuestDaten d;
				try (var reader = eintrag.getValue().openAsReader()) {
					var text = new java.io.StringWriter();
					reader.transferTo(text);
					d = de.itscalle96.questdetails.kern.QuestDatenLeser.lies(text.toString(), probleme::add);
				} catch (java.io.IOException fehler) {
					probleme.add("nicht lesbar: " + fehler);
					d = de.itscalle96.questdetails.kern.QuestDaten.LEER;
				}
				for (var s : d.schritte()) {
					schritte++;
					s.items().stream().filter(i -> de.itscalle96.questdetails.daten.Symbole.items(i).isEmpty())
							.forEach(i -> probleme.add("unbekanntes Item/Tag " + i));
					if (s.rezept() != null) {
						if (Datenquelle.rezept(s.rezept()).isPresent()) {
							raster++;
						} else {
							probleme.add("Rezept ohne Raster: " + s.rezept());
						}
					}
				}
				probleme.forEach(pr -> sb.append(eintrag.getKey()).append(": ").append(pr).append('\n'));
			}
			return dateien.size() + " Dateien, " + schritte + " Schritte, " + raster + " Rezept-Raster\n" + sb;
		});
		try {
			Files.createDirectories(ZIEL);
			Files.writeString(ZIEL.resolve("datencheck.txt"), bericht);
		} catch (java.io.IOException fehler) {
			throw new java.io.UncheckedIOException(fehler);
		}
	}

	private static void bilder(ClientGameTestContext ctx, String sprache) {
		oeffne(ctx, WERKBANK);
		ctx.waitTicks(12);
		foto(ctx, "werkbank_flug_" + sprache);
		ctx.waitTicks(38);
		foto(ctx, "werkbank_" + sprache);
		oeffneQuestbuch(ctx, EISEN);
		oeffne(ctx, EISEN);
		ctx.waitTicks(30);
		foto(ctx, "eisen_" + sprache);
		oeffneQuestbuch(ctx, DIAMANTEN);
		oeffne(ctx, DIAMANTEN);
		ctx.waitTicks(5);
		foto(ctx, "diamanten_" + sprache);
		// Mausrad über der Leiste scrollt bis „Wo finde ich das?“, der Baum dahinter bleibt stehen
		int[] f = ctx.computeOnClient(mc -> new int[] {mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight()});
		ctx.getInput().setCursorPos(f[0] - 40, f[1] / 2.0);
		for (int i = 0; i < 10; i++) {
			ctx.getInput().scroll(-1);
		}
		ctx.waitTicks(2);
		foto(ctx, "diamanten_fundort_" + sprache);
		oeffneQuestbuch(ctx, OHNE_DATEI);
		oeffne(ctx, OHNE_DATEI);
		ctx.waitTicks(5);
		foto(ctx, "ohne_datei_" + sprache);
	}

	/** Öffnet das Questbuch auf dem Tab, zu dem die Quest gehört. */
	private static void oeffneQuestbuch(ClientGameTestContext ctx, Identifier quest) {
		ctx.setScreen(() -> null);
		ctx.runOnClient(mc -> {
			var fortschritte = mc.player.connection.getAdvancements();
			var wurzel = fortschritte.tree().get(quest).root().holder();
			fortschritte.setSelectedTab(wurzel, false);
		});
		ctx.setScreen(() -> new AdvancementsScreen(Minecraft.getInstance().player.connection.getAdvancements()));
		ctx.waitTicks(5);
		ctx.runOnClient(mc -> mc.player.connection.getAdvancements()
				.setSelectedTab(mc.player.connection.getAdvancements().tree().get(quest).root().holder(), true));
		ctx.waitTicks(5);
	}

	/** Fährt die Maus rasterweise über das Questbuch, bis eine Questbuch-Quest darunter liegt. */
	private static Identifier sucheQuest(ClientGameTestContext ctx) {
		// Zweiter Versuch, falls Better Advancements den Tab beim ersten Durchgang noch ausrichtet
		for (int versuch = 0; versuch < 2; versuch++) {
			Identifier gefunden = suche(ctx, 1f);
			if (gefunden != null) {
				return gefunden;
			}
			ctx.waitTicks(20);
		}
		return null;
	}

	/** Sucht links neben der offenen Leiste irgendeine andere Questbuch-Quest. */
	private static Identifier sucheAndereQuest(ClientGameTestContext ctx) {
		return suche(ctx, 0.58f);
	}

	private static Identifier suche(ClientGameTestContext ctx, float bereich) {
		int[] f = ctx.computeOnClient(mc -> new int[] {
				mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mc.getWindow().getGuiScale()});
		Identifier offen = offen(ctx);
		for (int y = 30; y < f[1] - 10; y += 10) {
			for (int x = 6; x < f[0] * bereich - 6; x += 10) {
				ctx.getInput().setCursorPos(x * f[2] + 1, y * f[2] + 1);
				ctx.waitTick();
				AdvancementHolder h = ctx.computeOnClient(mc -> Seitenleiste.unterMaus());
				if (h == null || !h.id().getNamespace().equals(Seitenleiste.NAMESPACE)) {
					continue;
				}
				if (!h.id().equals(offen)) {
					return h.id();
				}
			}
		}
		return null;
	}

	private static void klick(ClientGameTestContext ctx) {
		ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		ctx.waitTick();
	}

	private static void oeffne(ClientGameTestContext ctx, Identifier quest) {
		ctx.runOnClient(mc -> Seitenleiste.oeffne(mc.player.connection.getAdvancements().get(quest)));
		ctx.waitTick();
	}

	private static Identifier offen(ClientGameTestContext ctx) {
		return ctx.computeOnClient(mc -> Seitenleiste.offen() == null ? null : Seitenleiste.offen().id());
	}

	private static void setzeSprache(ClientGameTestContext ctx, String code) {
		CompletableFuture<Void> fertig = ctx.computeOnClient(mc -> {
			mc.options.languageCode = code;
			mc.getLanguageManager().setSelected(code);
			return mc.reloadResourcePacks();
		});
		ctx.waitFor(mc -> fertig.isDone() && mc.gui.overlay() == null, 20 * 120);
	}

	private static void setzeShader(ClientGameTestContext ctx, boolean an) {
		ctx.setScreen(() -> null);
		ctx.runOnClient(mc -> IrisApi.getInstance().getConfig().setShadersEnabledAndApply(an));
		ctx.waitTicks(an ? 100 : 20);
		if (an) {
			pruefe(ctx.computeOnClient(mc -> IrisApi.getInstance().isShaderPackInUse()), "Shader-Pack ist aktiv (shaderpacks/ + config/iris.properties vorhanden?)");
		}
	}

	private static void foto(ClientGameTestContext ctx, String name) {
		Path bild = ctx.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
		try {
			Files.createDirectories(ZIEL);
			Files.copy(bild, ZIEL.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
		} catch (java.io.IOException fehler) {
			throw new java.io.UncheckedIOException(fehler);
		}
	}

	private static Identifier id(String pfad) {
		return Identifier.fromNamespaceAndPath(Seitenleiste.NAMESPACE, pfad);
	}

	private static void pruefe(boolean bedingung, String text) {
		if (!bedingung) {
			throw new AssertionError(text);
		}
	}
}
