package de.itscalle96.questdetails.ui;

import com.mojang.blaze3d.platform.InputConstants;
import de.itscalle96.questdetails.daten.Datenquelle;
import de.itscalle96.questdetails.daten.Symbole;
import de.itscalle96.questdetails.kern.QuestDaten;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Die Seitenleiste rechts im Questbuch. Klick auf eine Quest (Namespace {@code leitfaden}) öffnet sie, eine
 * andere Quest wechselt den Inhalt, ESC oder ein Klick daneben schließt sie. Gezeichnet wird über den
 * Fortschrittsbildschirm (Vanilla oder Better Advancements), der Quest-Baum bleibt links sichtbar.
 */
public final class Seitenleiste {
	public static final String NAMESPACE = "leitfaden";
	private static final String BETTER_ADVANCEMENTS = "betteradvancements.common.gui.BetterAdvancementsScreen";
	private static final int RAND = 8;
	private static final int ZEILE = 10;

	private static AdvancementHolder offen;
	private static long offenSeit;
	private static int scroll;
	private static AdvancementHolder hoverJetzt;
	private static AdvancementHolder hoverLetztes;
	private static int mausX = -1;
	private static int mausY = -1;
	private static int bildschirmBreite;
	private static ItemStack tooltipItem = ItemStack.EMPTY;

	private Seitenleiste() {
	}

	public static boolean istQuestbuch(Screen screen) {
		return screen instanceof AdvancementsScreen || screen.getClass().getName().equals(BETTER_ADVANCEMENTS);
	}

	/** Hängt sich an einen frisch initialisierten Fortschrittsbildschirm (Fabric setzt die Ereignisse bei jedem init neu). */
	public static void anmelden(Screen screen) {
		bildschirmBreite = screen.width;
		ScreenEvents.beforeExtract(screen).register((s, g, mx, my, delta) -> {
			mausX = mx;
			mausY = my;
			bildschirmBreite = s.width;
			hoverJetzt = null;
		});
		ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> {
			hoverLetztes = hoverJetzt;
			if (offen != null) {
				zeichne(s, g, mx, my);
			}
		});
		ScreenMouseEvents.allowMouseClick(screen).register((s, ereignis) -> klick(s, ereignis.x(), ereignis.y(), ereignis.button()));
		ScreenMouseEvents.allowMouseDrag(screen).register((s, ereignis, dx, dy) -> !ueberLeiste(ereignis.x()));
		ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, h, v) -> {
			if (!ueberLeiste(x)) {
				return true;
			}
			scroll -= (int) Math.round(v * 12);
			return false;
		});
		ScreenKeyboardEvents.allowKeyPress(screen).register((s, taste) -> {
			if (offen != null && taste.input() == InputConstants.KEY_ESCAPE) {
				schliessen();
				return false;
			}
			return true;
		});
		ScreenEvents.remove(screen).register(s -> schliessen());
	}

	/**
	 * Aus den Mixins: Diese Quest liegt gerade unter der Maus. Liefert false, wenn ihr Hover-Fenster nicht
	 * gezeichnet werden soll, weil die Maus auf der Seitenleiste steht.
	 */
	public static boolean hover(AdvancementHolder quest) {
		if (ueberLeiste(mausX)) {
			return false;
		}
		hoverJetzt = quest;
		return true;
	}

	public static void oeffne(AdvancementHolder quest) {
		if (!quest.equals(offen)) {
			offen = quest;
			offenSeit = Util.getMillis();
			scroll = 0;
		}
	}

	public static void schliessen() {
		offen = null;
	}

	/** Für Tests: die offene Quest oder null. */
	public static AdvancementHolder offen() {
		return offen;
	}

	/** Für Tests: die Quest, die im letzten Bild unter der Maus lag. */
	public static AdvancementHolder unterMaus() {
		return hoverLetztes;
	}

	private static boolean klick(Screen screen, double x, double y, int knopf) {
		if (offen != null && ueberLeiste(x)) {
			if (knopf == InputConstants.MOUSE_BUTTON_LEFT && x >= screen.width - RAND - 10 && y < RAND + 10) {
				schliessen();
			}
			return false;
		}
		if (knopf == InputConstants.MOUSE_BUTTON_LEFT && hoverLetztes != null
				&& hoverLetztes.id().getNamespace().equals(NAMESPACE) && hoverLetztes.value().display().isPresent()) {
			oeffne(hoverLetztes);
			return false;
		}
		if (offen != null) {
			schliessen();
			return false;
		}
		return true;
	}

	private static int breite(int bildschirm) {
		return Math.clamp(Math.round(bildschirm * 0.4f), Math.min(170, bildschirm), 300);
	}

	private static boolean ueberLeiste(double x) {
		return offen != null && x >= bildschirmBreite - breite(bildschirmBreite);
	}

	private static void zeichne(Screen screen, GuiGraphicsExtractor g, int mx, int my) {
		Font font = Minecraft.getInstance().font;
		int b = breite(screen.width);
		int links = screen.width - b;
		g.nextStratum();
		g.fill(links, 0, screen.width, screen.height, 0xFF101418);
		g.fill(links, 0, links + 1, screen.height, 0xFF5A6B7A);

		// Schließen-Kreuz und Hinweis unten bleiben stehen, der Rest scrollt
		boolean ueberKreuz = mx >= screen.width - RAND - 10 && my < RAND + 10 && mx >= links;
		g.text(font, "×", screen.width - RAND - 6, RAND - 2, ueberKreuz ? 0xFFFFFFFF : 0xFFA0A0A0);
		List<FormattedCharSequence> hinweis = font.split(Component.translatable("questdetails.schliessen"), b - 2 * RAND);
		int fuss = screen.height - RAND - hinweis.size() * ZEILE + 1;
		absatz(g, font, Component.translatable("questdetails.schliessen"), links + RAND, fuss, b - 2 * RAND, 0xFF707A84);

		int oben = RAND;
		int unten = fuss - 4;
		int inhaltBreite = b - 2 * RAND - 10;
		long zeit = Util.getMillis() - offenSeit;
		int hoehe = inhalt(null, font, links + RAND, 0, inhaltBreite, zeit, mx, my);
		scroll = Math.clamp(scroll, 0, Math.max(0, hoehe - (unten - oben)));
		tooltipItem = ItemStack.EMPTY;
		g.enableScissor(links + 1, oben, screen.width, unten);
		inhalt(g, font, links + RAND, oben - scroll, inhaltBreite, zeit, mx, my);
		g.disableScissor();
		if (hoehe > unten - oben) {
			int balken = Math.max(12, (unten - oben) * (unten - oben) / hoehe);
			int pos = oben + (unten - oben - balken) * scroll / Math.max(1, hoehe - (unten - oben));
			g.fill(screen.width - 4, pos, screen.width - 2, pos + balken, 0x80FFFFFF);
		}
		if (!tooltipItem.isEmpty() && my >= oben && my < unten) {
			g.setTooltipForNextFrame(font, tooltipItem, mx, my);
		}
	}

	/** Zeichnet (oder misst nur, wenn g == null) den Inhalt ab y und liefert dessen Höhe. */
	private static int inhalt(GuiGraphicsExtractor g, Font font, int x, int y0, int breite, long zeit, int mx, int my) {
		DisplayInfo info = offen.value().display().orElseThrow();
		QuestDaten daten = Datenquelle.quest(offen.id());
		int y = y0;

		// Titel mit Symbol
		symbol(g, font, info.icon().create(), x, y, mx, my);
		List<FormattedCharSequence> titel = font.split(info.title().copy().withStyle(ChatFormatting.BOLD), breite - 22);
		for (int i = 0; i < titel.size(); i++) {
			text(g, font, titel.get(i), x + 22, y + 4 + i * ZEILE, 0xFFFFFFFF);
		}
		y += Math.max(20, 4 + titel.size() * ZEILE) + 4;

		// Beschreibung nur ohne Datei – mit Datei steht sie schon im Hover-Fenster der Quest, die Schritte gehen vor
		if (daten.istLeer()) {
			y = absatz(g, font, info.description(), x, y, breite, 0xFFC8D0D8) + 6;
		}

		if (!daten.schritte().isEmpty()) {
			y = ueberschrift(g, font, Component.translatable("questdetails.schritte"), x, y, breite);
			int nummer = 1;
			for (QuestDaten.Schritt schritt : daten.schritte()) {
				y = Schrittzeichner.schritt(g, font, schritt, nummer++, x, y, breite, zeit, mx, my) + 6;
			}
		}
		if (!daten.fundorte().isEmpty()) {
			y = ueberschrift(g, font, Component.translatable("questdetails.fundort"), x, y, breite);
			for (QuestDaten.Fundort fundort : daten.fundorte()) {
				y = Schrittzeichner.fundort(g, font, fundort, x, y, breite) + 4;
			}
		}
		return y - y0;
	}

	static int ueberschrift(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int breite) {
		if (g != null) {
			g.text(font, text.copy().withStyle(ChatFormatting.BOLD), x, y, 0xFFFFC94D);
			g.fill(x, y + ZEILE, x + breite, y + ZEILE + 1, 0x40FFC94D);
		}
		return y + ZEILE + 5;
	}

	static int absatz(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int breite, int farbe) {
		for (FormattedCharSequence zeile : font.split(text, breite)) {
			text(g, font, zeile, x, y, farbe);
			y += ZEILE;
		}
		return y;
	}

	static void text(GuiGraphicsExtractor g, Font font, FormattedCharSequence text, int x, int y, int farbe) {
		if (g != null) {
			g.text(font, text, x, y, farbe);
		}
	}

	/** Item-Symbol (16×16) mit Tooltip beim Drüberfahren. */
	static void symbol(GuiGraphicsExtractor g, Font font, ItemStack stapel, int x, int y, int mx, int my) {
		if (g == null || stapel.isEmpty()) {
			return;
		}
		g.item(stapel, x, y);
		if (mx >= x && mx < x + 16 && my >= y && my < y + 16) {
			tooltipItem = stapel;
		}
	}

	static void merkeTooltip(ItemStack stapel) {
		tooltipItem = stapel;
	}

	static ItemStack wechselnd(List<ItemStack> auswahl) {
		return Symbole.aktuell(auswahl, Util.getMillis());
	}
}
