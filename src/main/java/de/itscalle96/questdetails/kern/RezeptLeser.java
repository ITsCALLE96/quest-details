package de.itscalle96.questdetails.kern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Liest Rezept-JSON aus den Spieldaten ({@code data/<ns>/recipe/<pfad>.json}). Unterstützt Werkbank (geformt,
 * formlos, Umwandlung), Öfen, Lagerfeuer, Steinsäge und Schmiedetisch-Umwandlung; alles andere ergibt null.
 */
public final class RezeptLeser {
	private static final Map<String, Rezept.Art> OEFEN = Map.of(
			"minecraft:smelting", Rezept.Art.OFEN,
			"minecraft:blasting", Rezept.Art.SCHMELZOFEN,
			"minecraft:smoking", Rezept.Art.RAEUCHEROFEN,
			"minecraft:campfire_cooking", Rezept.Art.LAGERFEUER);

	private RezeptLeser() {
	}

	public static Rezept lies(String json) {
		try {
			JsonElement e = JsonParser.parseString(json);
			return e.isJsonObject() ? lies(e.getAsJsonObject()) : null;
		} catch (JsonParseException | IllegalStateException | UnsupportedOperationException fehler) {
			return null;
		}
	}

	private static Rezept lies(JsonObject o) {
		String typ = o.has("type") ? Ids.normalisiere(o.get("type").getAsString()) : null;
		if (typ == null || !o.has("result")) {
			return null;
		}
		JsonElement ergebnisRoh = o.get("result");
		String ergebnis;
		int anzahl = 1;
		if (ergebnisRoh.isJsonObject()) {
			JsonObject r = ergebnisRoh.getAsJsonObject();
			ergebnis = r.has("id") ? Ids.normalisiere(r.get("id").getAsString()) : null;
			if (r.has("count")) {
				anzahl = Math.max(1, r.get("count").getAsInt());
			}
		} else {
			ergebnis = Ids.normalisiere(ergebnisRoh.getAsString());
		}
		if (ergebnis == null || Ids.istTag(ergebnis)) {
			return null;
		}
		List<List<String>> slots = switch (typ) {
			case "minecraft:crafting_shaped" -> geformt(o);
			case "minecraft:crafting_shapeless" -> formlos(o.getAsJsonArray("ingredients"));
			case "minecraft:crafting_transmute" -> formlos(liste(o.get("input"), o.get("material")));
			case "minecraft:stonecutting" -> List.of(zutat(o.get("ingredient")));
			case "minecraft:smithing_transform" -> List.of(zutat(o.get("template")), zutat(o.get("base")), zutat(o.get("addition")));
			default -> OEFEN.containsKey(typ) ? List.of(zutat(o.get("ingredient"))) : null;
		};
		if (slots == null || slots.stream().allMatch(List::isEmpty)) {
			return null;
		}
		Rezept.Art art = switch (typ) {
			case "minecraft:stonecutting" -> Rezept.Art.STEINSAEGE;
			case "minecraft:smithing_transform" -> Rezept.Art.SCHMIEDETISCH;
			default -> OEFEN.getOrDefault(typ, Rezept.Art.WERKBANK);
		};
		return new Rezept(art, slots, ergebnis, anzahl);
	}

	private static List<List<String>> geformt(JsonObject o) {
		JsonArray muster = o.getAsJsonArray("pattern");
		JsonObject schluessel = o.getAsJsonObject("key");
		if (muster == null || schluessel == null || muster.size() > 3) {
			return null;
		}
		List<List<String>> slots = leer9();
		for (int zeile = 0; zeile < muster.size(); zeile++) {
			String z = muster.get(zeile).getAsString();
			if (z.length() > 3) {
				return null;
			}
			for (int spalte = 0; spalte < z.length(); spalte++) {
				char c = z.charAt(spalte);
				if (c != ' ') {
					List<String> zutat = zutat(schluessel.get(String.valueOf(c)));
					if (zutat.isEmpty()) {
						return null;
					}
					slots.set(zeile * 3 + spalte, zutat);
				}
			}
		}
		return List.copyOf(slots);
	}

	private static List<List<String>> formlos(JsonArray zutaten) {
		if (zutaten == null || zutaten.isEmpty() || zutaten.size() > 9) {
			return null;
		}
		List<List<String>> slots = leer9();
		for (int i = 0; i < zutaten.size(); i++) {
			List<String> zutat = zutat(zutaten.get(i));
			if (zutat.isEmpty()) {
				return null;
			}
			slots.set(i, zutat);
		}
		return List.copyOf(slots);
	}

	/** Zutat: {@code "id"}, {@code "#tag"}, eine Liste davon oder das alte Format {@code {"item"|"tag": …}}. */
	static List<String> zutat(JsonElement e) {
		if (e == null || e.isJsonNull()) {
			return List.of();
		}
		List<String> ids = new ArrayList<>();
		if (e.isJsonArray()) {
			for (JsonElement teil : e.getAsJsonArray()) {
				ids.addAll(zutat(teil));
			}
		} else if (e.isJsonObject()) {
			JsonObject o = e.getAsJsonObject();
			if (o.has("item")) {
				ids.add(Ids.normalisiere(o.get("item").getAsString()));
			} else if (o.has("tag")) {
				ids.add(Ids.normalisiere("#" + o.get("tag").getAsString()));
			}
		} else {
			ids.add(Ids.normalisiere(e.getAsString()));
		}
		ids.removeIf(java.util.Objects::isNull);
		return List.copyOf(ids);
	}

	private static List<List<String>> leer9() {
		List<List<String>> slots = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			slots.add(List.of());
		}
		return slots;
	}

	private static JsonArray liste(JsonElement... teile) {
		JsonArray a = new JsonArray();
		for (JsonElement t : teile) {
			if (t != null) {
				a.add(t);
			}
		}
		return a;
	}
}
