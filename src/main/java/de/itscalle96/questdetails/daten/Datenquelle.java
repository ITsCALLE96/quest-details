package de.itscalle96.questdetails.daten;

import com.mojang.logging.LogUtils;
import de.itscalle96.questdetails.kern.Ids;
import de.itscalle96.questdetails.kern.QuestDaten;
import de.itscalle96.questdetails.kern.QuestDatenLeser;
import de.itscalle96.questdetails.kern.Rezept;
import de.itscalle96.questdetails.kern.RezeptLeser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lädt Questbuch-Dateien aus den Resourcepacks und Rezepte aus den Spieldaten im Jar (Minecraft und Mods).
 * Beides wird zwischengespeichert; Questbuch-Dateien bis zum nächsten Neuladen der Resourcepacks (F3+T).
 */
public final class Datenquelle {
	private static final Logger LOG = LogUtils.getLogger();
	private static final Map<Identifier, QuestDaten> QUESTS = new HashMap<>();
	private static final Map<String, Optional<Rezept>> REZEPTE = new HashMap<>();
	private static ResourceManager geladenFuer;

	private Datenquelle() {
	}

	/** Daten zur Quest; {@link QuestDaten#LEER}, wenn es keine Datei gibt. */
	public static QuestDaten quest(Identifier fortschritt) {
		ResourceManager rm = Minecraft.getInstance().getResourceManager();
		if (rm != geladenFuer) {
			QUESTS.clear();
			geladenFuer = rm;
		}
		return QUESTS.computeIfAbsent(fortschritt, id -> lade(rm, id));
	}

	private static QuestDaten lade(ResourceManager rm, Identifier fortschritt) {
		Identifier datei = Identifier.fromNamespaceAndPath(fortschritt.getNamespace(), "questbuch/" + fortschritt.getPath() + ".json");
		Optional<Resource> resource = rm.getResource(datei);
		if (resource.isEmpty()) {
			return QuestDaten.LEER;
		}
		try (Reader reader = resource.get().openAsReader()) {
			String name = datei + " (" + resource.get().sourcePackId() + ")";
			StringWriter text = new StringWriter();
			reader.transferTo(text);
			return QuestDatenLeser.lies(text.toString(), w -> LOG.warn("Quest-Details: {}: {}", name, w));
		} catch (IOException fehler) {
			LOG.warn("Quest-Details: {} nicht lesbar", datei, fehler);
			return QuestDaten.LEER;
		}
	}

	/** Rezept aus {@code data/<ns>/recipe/<pfad>.json} im Spiel- oder Mod-Jar; leer, wenn unbekannt oder nicht darstellbar. */
	public static Optional<Rezept> rezept(String id) {
		return REZEPTE.computeIfAbsent(id, Datenquelle::ladeRezept);
	}

	private static Optional<Rezept> ladeRezept(String id) {
		String pfad = "data/" + Ids.namespace(id) + "/recipe/" + Ids.pfad(id) + ".json";
		try (InputStream in = Datenquelle.class.getClassLoader().getResourceAsStream(pfad)) {
			if (in == null) {
				LOG.warn("Quest-Details: Rezept {} nicht gefunden", id);
				return Optional.empty();
			}
			Rezept r = RezeptLeser.lies(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			if (r == null) {
				LOG.warn("Quest-Details: Rezept {} lässt sich nicht als Raster zeigen", id);
			}
			return Optional.ofNullable(r);
		} catch (IOException fehler) {
			LOG.warn("Quest-Details: Rezept {} nicht lesbar", id, fehler);
			return Optional.empty();
		}
	}
}
