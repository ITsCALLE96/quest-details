package de.itscalle96.questdetails.daten;

import de.itscalle96.questdetails.kern.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Wandelt IDs und Tags aus den Questbuch-Dateien in Item-Symbole und Biom-Namen um. */
public final class Symbole {
	/** So lange steht jedes Item eines Tags, bevor das nächste kommt. */
	public static final int TAG_WECHSEL_MS = 1000;

	private Symbole() {
	}

	/** Alle Items zu einer ID bzw. einem Tag; leer, wenn unbekannt. */
	public static List<ItemStack> items(String id) {
		Identifier name = Identifier.tryParse(Ids.istTag(id) ? id.substring(1) : id);
		if (name == null) {
			return List.of();
		}
		if (!Ids.istTag(id)) {
			return BuiltInRegistries.ITEM.getOptional(name).map(i -> List.of(new ItemStack(i))).orElse(List.of());
		}
		List<ItemStack> stapel = new ArrayList<>();
		for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, name))) {
			stapel.add(new ItemStack(h));
		}
		return stapel;
	}

	/** Das Item, das gerade gezeigt wird: bei einer Liste (Tag oder Alternativen) wechselnd. */
	public static ItemStack aktuell(List<ItemStack> auswahl, long zeitMs) {
		if (auswahl.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return auswahl.get((int) Math.floorMod(zeitMs / TAG_WECHSEL_MS, (long) auswahl.size()));
	}

	/** Alle Items mehrerer Alternativen (z. B. ein Rezeptfeld mit Kohle oder Holzkohle). */
	public static List<ItemStack> items(List<String> alternativen) {
		List<ItemStack> alle = new ArrayList<>();
		alternativen.forEach(a -> alle.addAll(items(a)));
		return alle;
	}

	/** Spielnamen der Biome zu IDs bzw. Tags, höchstens {@code max} Stück. */
	public static List<Component> biomNamen(List<String> ids, int max) {
		List<Component> namen = new ArrayList<>();
		Optional<Registry<Biome>> biome = Optional.ofNullable(Minecraft.getInstance().level)
				.flatMap(l -> l.registryAccess().lookup(Registries.BIOME));
		for (String id : ids) {
			Identifier name = Identifier.tryParse(Ids.istTag(id) ? id.substring(1) : id);
			if (name == null) {
				continue;
			}
			if (!Ids.istTag(id)) {
				namen.add(biomName(name));
			} else if (biome.isPresent()) {
				for (Holder<Biome> h : biome.get().getTagOrEmpty(TagKey.create(Registries.BIOME, name))) {
					h.unwrapKey().map(ResourceKey::identifier).map(Symbole::biomName).ifPresent(namen::add);
				}
			}
		}
		return namen.stream().distinct().limit(max).toList();
	}

	private static Component biomName(Identifier id) {
		return Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
	}
}
