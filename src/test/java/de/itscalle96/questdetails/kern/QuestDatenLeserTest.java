package de.itscalle96.questdetails.kern;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDatenLeserTest {
	private final List<String> warnungen = new ArrayList<>();

	private QuestDaten lies(String json) {
		return QuestDatenLeser.lies(json, warnungen::add);
	}

	@Test
	void beispielAusDerSpezifikation() {
		QuestDaten d = lies("""
				{"schritte":[
				  {"text":{"translate":"questbuch.ankommen.werkbank.schritt.1","fallback":"Punch a tree"},"items":["#minecraft:logs"]},
				  {"text":{"translate":"q.2","fallback":"Turn logs into planks"},"items":["#minecraft:planks"],"rezept":"minecraft:oak_planks"},
				  {"text":{"translate":"q.3","fallback":"Craft a crafting table"},"items":["minecraft:crafting_table"],"rezept":"minecraft:crafting_table"}],
				 "fundort":[{"text":{"translate":"q.f","fallback":"Trees grow almost everywhere"},
				   "biome":["minecraft:forest","#minecraft:is_taiga"],"hoehe":{"von":60,"bis":120},"dimension":"minecraft:overworld"}]}""");
		assertEquals(3, d.schritte().size());
		assertEquals(List.of("#minecraft:logs"), d.schritte().get(0).items());
		assertNull(d.schritte().get(0).rezept());
		assertEquals("minecraft:crafting_table", d.schritte().get(2).rezept());
		QuestDaten.Fundort f = d.fundorte().getFirst();
		assertEquals(List.of("minecraft:forest", "#minecraft:is_taiga"), f.biome());
		assertEquals(60, f.hoeheVon());
		assertEquals(120, f.hoeheBis());
		assertEquals("minecraft:overworld", f.dimension());
		assertTrue(warnungen.isEmpty(), warnungen.toString());
	}

	@Test
	void textAlsStringIstUebersetzungsschluessel() {
		QuestDaten d = lies("{\"schritte\":[{\"text\":\"questbuch.x\"}]}");
		assertEquals("questbuch.x", d.schritte().getFirst().text().getAsJsonObject().get("translate").getAsString());
	}

	@Test
	void idsOhneNamespaceUndGrossschreibungWerdenVereinheitlicht() {
		QuestDaten d = lies("{\"schritte\":[{\"text\":\"a\",\"items\":[\"Oak_Log\",\"#logs\"],\"rezept\":\"torch\"}]}");
		assertEquals(List.of("minecraft:oak_log", "#minecraft:logs"), d.schritte().getFirst().items());
		assertEquals("minecraft:torch", d.schritte().getFirst().rezept());
	}

	@Test
	void fehlerhafteEintraegeWerdenEinzelnUebersprungen() {
		QuestDaten d = lies("""
				{"schritte":[{"items":["minecraft:stone"]}, 42, {"text":"ok","items":["kaputt id!", "minecraft:dirt"],"rezept":"#minecraft:logs"}],
				 "fundort":[{}, {"hoehe":{"bis":16}}], "unbekannt": true}""");
		assertEquals(1, d.schritte().size());
		assertEquals(List.of("minecraft:dirt"), d.schritte().getFirst().items());
		assertNull(d.schritte().getFirst().rezept(), "ein Tag ist kein Rezept");
		assertEquals(1, d.fundorte().size());
		assertNull(d.fundorte().getFirst().hoeheVon());
		assertEquals(16, d.fundorte().getFirst().hoeheBis());
		assertEquals(5, warnungen.size(), warnungen.toString());
	}

	@Test
	void vertauschteHoeheWirdKorrigiert() {
		QuestDaten.Fundort f = lies("{\"fundort\":[{\"hoehe\":{\"von\":16,\"bis\":-64}}]}").fundorte().getFirst();
		assertEquals(-64, f.hoeheVon());
		assertEquals(16, f.hoeheBis());
	}

	@Test
	void kaputteDateiErgibtLeereDaten() {
		assertTrue(lies("{ kein json").istLeer());
		assertTrue(lies("[1,2]").istLeer());
		assertTrue(lies("{\"schritte\": \"nein\"}").istLeer());
		assertEquals(3, warnungen.size());
	}

	@Test
	void idsWerdenZerlegt() {
		assertEquals("leitfaden", Ids.namespace("leitfaden:ankommen/werkbank"));
		assertEquals("ankommen/werkbank", Ids.pfad("leitfaden:ankommen/werkbank"));
		assertEquals("logs", Ids.pfad("#minecraft:logs"));
		assertNull(Ids.normalisiere("a b"));
	}
}
