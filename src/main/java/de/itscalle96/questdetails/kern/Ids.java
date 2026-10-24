package de.itscalle96.questdetails.kern;

import java.util.Locale;
import java.util.regex.Pattern;

/** Prüft und vereinheitlicht IDs wie {@code oak_log}, {@code minecraft:oak_log} oder {@code #minecraft:logs}. */
public final class Ids {
	private static final Pattern ID = Pattern.compile("#?([a-z0-9_.-]+:)?[a-z0-9_./-]+");

	private Ids() {
	}

	/** Vereinheitlichte ID (Namespace ergänzt, Kleinschreibung) oder null, wenn ungültig. */
	public static String normalisiere(String roh) {
		if (roh == null) {
			return null;
		}
		String s = roh.trim().toLowerCase(Locale.ROOT);
		if (!ID.matcher(s).matches()) {
			return null;
		}
		boolean tag = s.startsWith("#");
		String rest = tag ? s.substring(1) : s;
		if (!rest.contains(":")) {
			rest = "minecraft:" + rest;
		}
		return tag ? "#" + rest : rest;
	}

	public static boolean istTag(String id) {
		return id.startsWith("#");
	}

	/** Namespace einer (vereinheitlichten) ID, ohne {@code #}. */
	public static String namespace(String id) {
		String rest = istTag(id) ? id.substring(1) : id;
		return rest.substring(0, rest.indexOf(':'));
	}

	/** Pfad einer (vereinheitlichten) ID, ohne {@code #}. */
	public static String pfad(String id) {
		String rest = istTag(id) ? id.substring(1) : id;
		return rest.substring(rest.indexOf(':') + 1);
	}
}
