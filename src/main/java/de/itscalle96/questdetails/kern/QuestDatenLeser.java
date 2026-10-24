package de.itscalle96.questdetails.kern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Liest eine Questbuch-Datei nachsichtig: Unbekannte Felder werden ignoriert, fehlerhafte Einträge einzeln
 * übersprungen und gemeldet, der Rest bleibt nutzbar.
 */
public final class QuestDatenLeser {
	private QuestDatenLeser() {
	}

	public static QuestDaten lies(String json, Consumer<String> warnung) {
		JsonElement wurzel;
		try {
			wurzel = JsonParser.parseString(json);
		} catch (JsonParseException fehler) {
			warnung.accept("kein gültiges JSON: " + fehler.getMessage());
			return QuestDaten.LEER;
		}
		if (!wurzel.isJsonObject()) {
			warnung.accept("erwartet ein Objekt { \"schritte\": [...], \"fundort\": [...] }");
			return QuestDaten.LEER;
		}
		JsonObject o = wurzel.getAsJsonObject();
		List<QuestDaten.Schritt> schritte = new ArrayList<>();
		for (JsonElement e : liste(o, "schritte", warnung)) {
			QuestDaten.Schritt s = schritt(e, schritte.size() + 1, warnung);
			if (s != null) {
				schritte.add(s);
			}
		}
		List<QuestDaten.Fundort> fundorte = new ArrayList<>();
		for (JsonElement e : liste(o, "fundort", warnung)) {
			QuestDaten.Fundort f = fundort(e, warnung);
			if (f != null) {
				fundorte.add(f);
			}
		}
		return new QuestDaten(List.copyOf(schritte), List.copyOf(fundorte));
	}

	private static QuestDaten.Schritt schritt(JsonElement e, int nummer, Consumer<String> warnung) {
		if (!e.isJsonObject()) {
			warnung.accept("Schritt " + nummer + " ist kein Objekt, übersprungen");
			return null;
		}
		JsonObject o = e.getAsJsonObject();
		JsonElement text = text(o.get("text"));
		if (text == null) {
			warnung.accept("Schritt " + nummer + " ohne \"text\", übersprungen");
			return null;
		}
		String rezept = null;
		if (o.has("rezept")) {
			rezept = o.get("rezept").isJsonPrimitive() ? Ids.normalisiere(o.get("rezept").getAsString()) : null;
			if (rezept == null || Ids.istTag(rezept)) {
				warnung.accept("Schritt " + nummer + ": ungültige Rezept-ID " + o.get("rezept") + ", ohne Raster");
				rezept = null;
			}
		}
		return new QuestDaten.Schritt(text, ids(o, "items", warnung), rezept);
	}

	private static QuestDaten.Fundort fundort(JsonElement e, Consumer<String> warnung) {
		if (!e.isJsonObject()) {
			warnung.accept("Fundort ist kein Objekt, übersprungen");
			return null;
		}
		JsonObject o = e.getAsJsonObject();
		JsonElement text = text(o.get("text"));
		Integer von = null;
		Integer bis = null;
		if (o.has("hoehe")) {
			if (o.get("hoehe").isJsonObject()) {
				JsonObject h = o.getAsJsonObject("hoehe");
				von = ganzzahl(h.get("von"));
				bis = ganzzahl(h.get("bis"));
			}
			if (von == null && bis == null) {
				warnung.accept("Fundort: \"hoehe\" braucht \"von\" und/oder \"bis\" als Zahl");
			} else if (von != null && bis != null && von > bis) {
				int tausch = von;
				von = bis;
				bis = tausch;
			}
		}
		String dimension = null;
		if (o.has("dimension")) {
			dimension = o.get("dimension").isJsonPrimitive() ? Ids.normalisiere(o.get("dimension").getAsString()) : null;
			if (dimension == null || Ids.istTag(dimension)) {
				warnung.accept("Fundort: ungültige Dimension " + o.get("dimension"));
				dimension = null;
			}
		}
		List<String> biome = ids(o, "biome", warnung);
		if (text == null && biome.isEmpty() && von == null && bis == null && dimension == null) {
			warnung.accept("Fundort ohne Inhalt, übersprungen");
			return null;
		}
		return new QuestDaten.Fundort(text, biome, von, bis, dimension);
	}

	/** Text-Komponente; ein reiner String gilt als Übersetzungsschlüssel. */
	private static JsonElement text(JsonElement e) {
		if (e == null || e.isJsonNull()) {
			return null;
		}
		if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
			if (e.getAsString().isBlank()) {
				return null;
			}
			JsonObject uebersetzt = new JsonObject();
			uebersetzt.addProperty("translate", e.getAsString());
			return uebersetzt;
		}
		return e.isJsonObject() || e.isJsonArray() ? e : null;
	}

	private static Integer ganzzahl(JsonElement e) {
		if (e instanceof JsonPrimitive p && p.isNumber()) {
			double d = p.getAsDouble();
			if (d == Math.rint(d) && Math.abs(d) < 100_000) {
				return (int) d;
			}
		}
		return null;
	}

	private static List<String> ids(JsonObject o, String feld, Consumer<String> warnung) {
		List<String> ergebnis = new ArrayList<>();
		for (JsonElement e : liste(o, feld, warnung)) {
			String id = e.isJsonPrimitive() ? Ids.normalisiere(e.getAsString()) : null;
			if (id == null) {
				warnung.accept("\"" + feld + "\": ungültige ID " + e + ", übersprungen");
			} else {
				ergebnis.add(id);
			}
		}
		return List.copyOf(ergebnis);
	}

	private static JsonArray liste(JsonObject o, String feld, Consumer<String> warnung) {
		JsonElement e = o.get(feld);
		if (e == null || e.isJsonNull()) {
			return new JsonArray();
		}
		if (!e.isJsonArray()) {
			warnung.accept("\"" + feld + "\" muss eine Liste sein, ignoriert");
			return new JsonArray();
		}
		return e.getAsJsonArray();
	}
}
