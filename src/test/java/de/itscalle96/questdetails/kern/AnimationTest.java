package de.itscalle96.questdetails.kern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationTest {
	private static final int N = 4;

	@Test
	void amAnfangIstNochNichtsDa() {
		Animation.Zustand z = Animation.zustand(0, N);
		assertArrayEquals(new float[N], z.flug());
		assertEquals(0f, z.ergebnis());
		assertEquals(1f, z.gesamt());
	}

	@Test
	void zutatenLandenNacheinander() {
		Animation.Zustand z = Animation.zustand(Animation.FLUG_MS, N);
		assertEquals(1f, z.flug()[0], "erste Zutat ist gelandet");
		assertTrue(z.flug()[1] > 0f && z.flug()[1] < 1f, "zweite ist unterwegs");
		assertEquals(0f, z.flug()[3], "letzte ist noch nicht gestartet");
		assertEquals(0f, z.ergebnis(), "Ergebnis erst nach der letzten Zutat");
	}

	@Test
	void ergebnisStehtDanachEineWeile() {
		long gelandet = (N - 1) * Animation.ABSTAND_MS + Animation.FLUG_MS;
		long mitte = gelandet + Animation.PAUSE_MS + Animation.WACHSEN_MS + Animation.ZEIGEN_MS / 2;
		Animation.Zustand z = Animation.zustand(mitte, N);
		assertArrayEquals(new float[] {1f, 1f, 1f, 1f}, z.flug());
		assertEquals(1f, z.ergebnis());
		assertEquals(1f, z.gesamt());
	}

	@Test
	void amEndeSchrumpftAllesUndEsGehtVonVornLos() {
		int dauer = Animation.dauer(N);
		assertTrue(Animation.zustand(dauer - 1, N).gesamt() < 0.01f);
		assertEquals(Animation.zustand(123, N).ergebnis(), Animation.zustand(dauer + 123, N).ergebnis());
		assertArrayEquals(Animation.zustand(500, N).flug(), Animation.zustand(3L * dauer + 500, N).flug());
	}

	@Test
	void ohneHarteSpruenge() {
		int dauer = Animation.dauer(N);
		Animation.Zustand vorher = Animation.zustand(0, N);
		for (int t = 10; t <= dauer; t += 10) {
			Animation.Zustand jetzt = Animation.zustand(t, N);
			if (t % dauer != 0) {
				assertTrue(Math.abs(jetzt.gesamt() - vorher.gesamt()) < 0.1f, "Sprung bei " + t);
				assertTrue(Math.abs(jetzt.ergebnis() - vorher.ergebnis()) < 0.1f, "Sprung bei " + t);
			}
			vorher = jetzt;
		}
	}

	@Test
	void eineZutatUndKeineZutatGehen() {
		assertEquals(1, Animation.zustand(10_000, 1).flug().length);
		assertEquals(0, Animation.zustand(10_000, 0).flug().length);
	}
}
