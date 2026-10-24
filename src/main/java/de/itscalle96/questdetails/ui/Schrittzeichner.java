package de.itscalle96.questdetails.ui;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import de.itscalle96.questdetails.daten.Datenquelle;
import de.itscalle96.questdetails.daten.Symbole;
import de.itscalle96.questdetails.kern.Animation;
import de.itscalle96.questdetails.kern.Ids;
import de.itscalle96.questdetails.kern.QuestDaten;
import de.itscalle96.questdetails.kern.Rezept;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Zeichnet einen Schritt (Nummer, Item-Symbole, Text, Rezept-Raster) und einen Fundort. */
final class Schrittzeichner {
	private static final int SLOT = 18;
	private static final int EINRUECKEN = 14;
	private static final int MAX_BIOME = 6;
	private static final Map<JsonElement, Component> TEXTE = new HashMap<>();

	private Schrittzeichner() {
	}

	static int schritt(GuiGraphicsExtractor g, Font font, QuestDaten.Schritt schritt, int nummer, int x, int y,
			int breite, long zeit, int mx, int my) {
		int innen = x + EINRUECKEN;
		int innenBreite = breite - EINRUECKEN;
		if (g != null) {
			g.text(font, nummer + ".", x, y + (schritt.items().isEmpty() ? 0 : 4), 0xFFFFC94D);
		}
		if (!schritt.items().isEmpty()) {
			int ix = innen;
			for (String id : schritt.items()) {
				if (ix + 16 > innen + innenBreite) {
					ix = innen;
					y += SLOT;
				}
				Seitenleiste.symbol(g, font, Seitenleiste.wechselnd(Symbole.items(id)), ix, y, mx, my);
				ix += SLOT;
			}
			y += SLOT + 2;
		}
		y = Seitenleiste.absatz(g, font, text(schritt.text()), innen, y, innenBreite, 0xFFFFFFFF);
		if (schritt.rezept() != null) {
			Optional<Rezept> rezept = Datenquelle.rezept(schritt.rezept());
			if (rezept.isPresent()) {
				y += 3 + raster(g, font, rezept.get(), innen + 4, y + 3, zeit, mx, my);
			}
		}
		return y;
	}

	static int fundort(GuiGraphicsExtractor g, Font font, QuestDaten.Fundort fundort, int x, int y, int breite) {
		int farbe = 0xFFC8D0D8;
		if (fundort.text() != null) {
			y = Seitenleiste.absatz(g, font, text(fundort.text()), x, y, breite, 0xFFFFFFFF);
		}
		if (!fundort.biome().isEmpty()) {
			List<Component> namen = Symbole.biomNamen(fundort.biome(), MAX_BIOME);
			if (!namen.isEmpty()) {
				MutableComponent liste = Component.empty();
				for (int i = 0; i < namen.size(); i++) {
					liste.append(i == 0 ? Component.empty() : Component.literal(", ")).append(namen.get(i));
				}
				if (namen.size() == MAX_BIOME) {
					liste.append(", …");
				}
				y = Seitenleiste.absatz(g, font, Component.translatable("questdetails.biome", liste), x, y, breite, farbe);
			}
		}
		if (fundort.hoeheVon() != null || fundort.hoeheBis() != null) {
			Component hoehe;
			if (fundort.hoeheVon() != null && fundort.hoeheBis() != null) {
				hoehe = Component.translatable("questdetails.hoehe.bereich", fundort.hoeheVon(), fundort.hoeheBis());
			} else if (fundort.hoeheVon() != null) {
				hoehe = Component.translatable("questdetails.hoehe.ab", fundort.hoeheVon());
			} else {
				hoehe = Component.translatable("questdetails.hoehe.bis", fundort.hoeheBis());
			}
			y = Seitenleiste.absatz(g, font, hoehe, x, y, breite, farbe);
		}
		if (fundort.dimension() != null) {
			Component name = Component.translatableWithFallback(
					"questdetails.dimension." + Ids.pfad(fundort.dimension()), fundort.dimension());
			y = Seitenleiste.absatz(g, font, Component.translatable("questdetails.dimension", name), x, y, breite, farbe);
		}
		return y;
	}

	/** Text-Komponente aus der Datei (wie in Fortschritten), zwischengespeichert. */
	private static Component text(JsonElement json) {
		return TEXTE.computeIfAbsent(json, j -> ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, j)
				.result().orElseGet(() -> Component.literal(j.toString())));
	}

	/** Animiertes Raster; liefert seine Höhe. */
	private static int raster(GuiGraphicsExtractor g, Font font, Rezept rezept, int gx, int gy, long zeit, int mx, int my) {
		int[][] felder = felder(rezept.art());
		int hoehe = 0;
		int rechts = 0;
		for (int[] f : felder) {
			hoehe = Math.max(hoehe, f[1] + SLOT);
			rechts = Math.max(rechts, f[0] + SLOT);
		}
		int pfeilX = gx + rechts + 4;
		int ergebnisX = pfeilX + 20;
		int ergebnisY = gy + (hoehe - SLOT) / 2;
		if (g == null) {
			return hoehe;
		}
		for (int[] f : felder) {
			slot(g, gx + f[0], gy + f[1]);
		}
		slot(g, ergebnisX, ergebnisY);
		g.text(font, "→", pfeilX + 4, ergebnisY + 5, 0xFFC8D0D8);
		// Arbeitsplatz rechts neben dem Ergebnis, mit Namen als Tooltip
		Seitenleiste.symbol(g, font, new ItemStack(Symbole.items(rezept.art().block).stream().findFirst()
				.map(ItemStack::getItem).orElse(Items.CRAFTING_TABLE)), ergebnisX + SLOT + 6, ergebnisY + 1, mx, my);
		if (rezept.art().istOfen() && rezept.art() != Rezept.Art.LAGERFEUER) {
			Seitenleiste.symbol(g, font, new ItemStack(Items.COAL), gx + 1, gy + 2 * SLOT + 1, mx, my);
		}

		List<Integer> belegt = rezept.belegteSlots();
		Animation.Zustand z = Animation.zustand(zeit, belegt.size());
		float startX = gx - EINRUECKEN;
		float startY = gy + hoehe / 2f - 8;
		for (int i = 0; i < belegt.size(); i++) {
			float t = z.flug()[i];
			if (t <= 0f) {
				continue;
			}
			int[] f = felder[belegt.get(i)];
			float zielX = gx + f[0] + 1;
			float zielY = gy + f[1] + 1;
			float px = startX + (zielX - startX) * t;
			float py = startY + (zielY - startY) * t - (float) Math.sin(Math.PI * t) * 10f;
			ItemStack stapel = Seitenleiste.wechselnd(Symbole.items(rezept.slots().get(belegt.get(i))));
			item(g, stapel, px, py, z.gesamt() * (0.5f + 0.5f * t));
			if (t >= 1f) {
				merkeTooltip(font, stapel, gx + f[0] + 1, gy + f[1] + 1, mx, my);
			}
		}
		if (z.ergebnis() > 0f) {
			ItemStack ergebnis = Symbole.items(rezept.ergebnis()).stream().findFirst().orElse(ItemStack.EMPTY).copyWithCount(rezept.anzahl());
			float groesse = z.ergebnis() * z.gesamt();
			item(g, ergebnis, ergebnisX + 1, ergebnisY + 1, groesse);
			if (groesse > 0.99f && rezept.anzahl() > 1) {
				g.itemDecorations(font, ergebnis, ergebnisX + 1, ergebnisY + 1);
			}
			merkeTooltip(font, ergebnis, ergebnisX + 1, ergebnisY + 1, mx, my);
		}
		return hoehe;
	}

	private static void merkeTooltip(Font font, ItemStack stapel, int x, int y, int mx, int my) {
		if (mx >= x && mx < x + 16 && my >= y && my < y + 16) {
			Seitenleiste.merkeTooltip(stapel);
		}
	}

	/** Lage der Felder (links oben, relativ zum Raster) je Arbeitsplatz. */
	private static int[][] felder(Rezept.Art art) {
		return switch (art) {
			case WERKBANK -> new int[][] {
					{0, 0}, {SLOT, 0}, {2 * SLOT, 0},
					{0, SLOT}, {SLOT, SLOT}, {2 * SLOT, SLOT},
					{0, 2 * SLOT}, {SLOT, 2 * SLOT}, {2 * SLOT, 2 * SLOT}};
			case OFEN, SCHMELZOFEN, RAEUCHEROFEN -> new int[][] {{0, 0}, {0, 2 * SLOT}};
			case SCHMIEDETISCH -> new int[][] {{0, 0}, {SLOT, 0}, {2 * SLOT, 0}};
			default -> new int[][] {{0, 0}};
		};
	}

	private static void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + SLOT, y + SLOT, 0xFF555D66);
		g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFF22272D);
	}

	private static void item(GuiGraphicsExtractor g, ItemStack stapel, float x, float y, float groesse) {
		if (stapel.isEmpty() || groesse <= 0.01f) {
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x + 8, y + 8);
		g.pose().scale(groesse, groesse);
		g.item(stapel, -8, -8);
		g.pose().popMatrix();
	}
}
