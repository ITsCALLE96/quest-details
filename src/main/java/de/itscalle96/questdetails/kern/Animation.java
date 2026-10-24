package de.itscalle96.questdetails.kern;

/**
 * Zeitablauf des Rezept-Rasters, in Schleife: Die Zutaten gleiten nacheinander in ihre Felder, kurz danach
 * wächst das Ergebnis heraus, bleibt eine Weile stehen, dann schrumpft alles weg und es beginnt von vorn.
 * Alles geht weich ineinander über – kein Blinken, kein harter Wechsel.
 */
public final class Animation {
	public static final int FLUG_MS = 450;
	public static final int ABSTAND_MS = 300;
	public static final int PAUSE_MS = 350;
	public static final int WACHSEN_MS = 250;
	public static final int ZEIGEN_MS = 2200;
	public static final int AUSBLENDEN_MS = 400;

	private Animation() {
	}

	/**
	 * @param flug     pro Zutat 0 (am Startpunkt) bis 1 (im Feld), schon weich gezeichnet
	 * @param ergebnis Größe des Ergebnisses, 0 bis 1
	 * @param gesamt   Größe von allem, 1 bis zum Ausblenden, dann bis 0
	 */
	public record Zustand(float[] flug, float ergebnis, float gesamt) {
	}

	/** Länge einer Runde in Millisekunden. */
	public static int dauer(int zutaten) {
		return Math.max(0, zutaten - 1) * ABSTAND_MS + FLUG_MS + PAUSE_MS + WACHSEN_MS + ZEIGEN_MS + AUSBLENDEN_MS;
	}

	public static Zustand zustand(long zeitMs, int zutaten) {
		int dauer = dauer(zutaten);
		long t = Math.floorMod(zeitMs, (long) dauer);
		float[] flug = new float[zutaten];
		for (int i = 0; i < zutaten; i++) {
			flug[i] = weich(anteil(t - (long) i * ABSTAND_MS, FLUG_MS));
		}
		long gelandet = (long) Math.max(0, zutaten - 1) * ABSTAND_MS + FLUG_MS;
		float ergebnis = weich(anteil(t - gelandet - PAUSE_MS, WACHSEN_MS));
		float gesamt = 1f - weich(anteil(t - (dauer - AUSBLENDEN_MS), AUSBLENDEN_MS));
		return new Zustand(flug, ergebnis, gesamt);
	}

	private static float anteil(long t, int laenge) {
		return Math.clamp((float) t / laenge, 0f, 1f);
	}

	/** Smoothstep: sanft an- und auslaufend. */
	static float weich(float x) {
		return x * x * (3f - 2f * x);
	}
}
