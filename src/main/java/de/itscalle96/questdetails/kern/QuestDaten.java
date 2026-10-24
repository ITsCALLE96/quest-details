package de.itscalle96.questdetails.kern;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * Inhalt einer Questbuch-Datei ({@code assets/<ns>/questbuch/<pfad>.json}): nummerierte Schritte und
 * „Wo finde ich das?“. Texte bleiben JSON-Text-Komponenten, übersetzt wird erst im Spiel.
 */
public record QuestDaten(List<Schritt> schritte, List<Fundort> fundorte) {
	public static final QuestDaten LEER = new QuestDaten(List.of(), List.of());

	/** Ein Schritt: Text, Item-Symbole (IDs oder {@code #tags}) und optional eine Rezept-ID für das Raster. */
	public record Schritt(JsonElement text, List<String> items, String rezept) {
	}

	/** Ein Fundort: freier Text, Biome (IDs oder {@code #tags}), Höhenbereich und Dimension – alles optional. */
	public record Fundort(JsonElement text, List<String> biome, Integer hoeheVon, Integer hoeheBis, String dimension) {
	}

	public boolean istLeer() {
		return schritte.isEmpty() && fundorte.isEmpty();
	}
}
