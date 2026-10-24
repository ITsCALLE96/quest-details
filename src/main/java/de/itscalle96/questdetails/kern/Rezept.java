package de.itscalle96.questdetails.kern;

import java.util.List;

/**
 * Ein Rezept, so wie die Seitenleiste es zeigt. {@code slots} enthält pro Feld die möglichen Zutaten (Item-IDs
 * oder {@code #tags}); ein leeres Feld ist eine leere Liste. Bei der Werkbank sind es immer 9 Felder (3×3,
 * zeilenweise), bei Öfen und Steinsäge 1, beim Schmiedetisch 3.
 */
public record Rezept(Art art, List<List<String>> slots, String ergebnis, int anzahl) {
	public enum Art {
		WERKBANK("minecraft:crafting_table"),
		OFEN("minecraft:furnace"),
		SCHMELZOFEN("minecraft:blast_furnace"),
		RAEUCHEROFEN("minecraft:smoker"),
		LAGERFEUER("minecraft:campfire"),
		STEINSAEGE("minecraft:stonecutter"),
		SCHMIEDETISCH("minecraft:smithing_table");

		/** Block, dessen Symbol neben dem Raster steht. */
		public final String block;

		Art(String block) {
			this.block = block;
		}

		public boolean istOfen() {
			return this == OFEN || this == SCHMELZOFEN || this == RAEUCHEROFEN || this == LAGERFEUER;
		}
	}

	/** Felder, die tatsächlich belegt sind, in Reihenfolge – in dieser Reihenfolge fliegen die Zutaten ein. */
	public List<Integer> belegteSlots() {
		return java.util.stream.IntStream.range(0, slots.size()).filter(i -> !slots.get(i).isEmpty()).boxed().toList();
	}
}
