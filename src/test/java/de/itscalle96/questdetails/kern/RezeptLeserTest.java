package de.itscalle96.questdetails.kern;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Liest echte Rezepte aus dem Minecraft-Jar auf dem Klassenpfad – genau wie die Mod im Spiel. */
class RezeptLeserTest {
	private static Rezept vanilla(String pfad) throws IOException {
		try (InputStream in = RezeptLeserTest.class.getClassLoader().getResourceAsStream("data/minecraft/recipe/" + pfad + ".json")) {
			assertNotNull(in, "Rezept " + pfad + " nicht im Minecraft-Jar");
			return RezeptLeser.lies(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	@Test
	void werkbankGeformt() throws IOException {
		Rezept r = vanilla("crafting_table");
		assertEquals(Rezept.Art.WERKBANK, r.art());
		assertEquals("minecraft:crafting_table", r.ergebnis());
		assertEquals(List.of("#minecraft:planks"), r.slots().get(0));
		assertEquals(List.of("#minecraft:planks"), r.slots().get(4));
		assertEquals(List.of(), r.slots().get(2));
		assertEquals(List.of(0, 1, 3, 4), r.belegteSlots());
	}

	@Test
	void fackelMitZweiMoeglichenZutatenUndAnzahl() throws IOException {
		Rezept r = vanilla("torch");
		assertEquals(List.of("minecraft:coal", "minecraft:charcoal"), r.slots().get(0));
		assertEquals(List.of("minecraft:stick"), r.slots().get(3));
		assertEquals(4, r.anzahl());
	}

	@Test
	void formlos() throws IOException {
		Rezept r = vanilla("oak_planks");
		assertEquals(List.of(0), r.belegteSlots());
		assertEquals(List.of("#minecraft:oak_logs"), r.slots().getFirst());
		assertEquals(4, r.anzahl());
	}

	@Test
	void oefen() throws IOException {
		Rezept ofen = vanilla("iron_ingot_from_smelting_raw_iron");
		assertEquals(Rezept.Art.OFEN, ofen.art());
		assertEquals(List.of(List.of("minecraft:raw_iron")), ofen.slots());
		assertEquals(Rezept.Art.SCHMELZOFEN, vanilla("iron_ingot_from_blasting_raw_iron").art());
		assertEquals(Rezept.Art.LAGERFEUER, vanilla("cooked_beef_from_campfire_cooking").art());
	}

	@Test
	void schmiedetischUndSteinsaege() throws IOException {
		Rezept netherit = vanilla("netherite_pickaxe_smithing");
		assertEquals(Rezept.Art.SCHMIEDETISCH, netherit.art());
		assertEquals(3, netherit.belegteSlots().size());
		assertEquals(Rezept.Art.STEINSAEGE, vanilla("stone_bricks_from_stone_stonecutting").art());
	}

	@Test
	void sonderrezepteUndKaputtesGebenNull() throws IOException {
		assertNull(vanilla("firework_rocket"));
		assertNull(RezeptLeser.lies("{"));
		assertNull(RezeptLeser.lies("{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"#\"],\"key\":{},\"result\":{\"id\":\"minecraft:stone\"}}"));
	}

	@Test
	void altesZutatenFormat() {
		assertEquals(List.of("minecraft:stone", "#minecraft:logs"),
				RezeptLeser.zutat(com.google.gson.JsonParser.parseString("[{\"item\":\"stone\"},{\"tag\":\"minecraft:logs\"}]")));
	}
}
