# Quest-Details: Datenformat (Version 1, 24.10.2026)

## Ablage

Eine Datei pro Quest im **Resourcepack**, Pfad = Fortschritts-ID:

```
Fortschritt  leitfaden:ankommen/werkbank
Datei        assets/leitfaden/questbuch/ankommen/werkbank.json
```

Fehlt die Datei, zeigt die Seitenleiste nur Titel und Beschreibung der Quest. Die Seitenleiste erscheint nur für Fortschritte im Namespace `leitfaden`.

## Inhalt

```json
{
  "schritte": [
    {
      "text": { "translate": "questbuch.ankommen.werkbank.schritt.1", "fallback": "Punch a tree" },
      "items": ["#minecraft:logs"]
    },
    {
      "text": { "translate": "questbuch.ankommen.werkbank.schritt.2", "fallback": "Turn logs into planks" },
      "items": ["#minecraft:planks"],
      "rezept": "minecraft:oak_planks"
    },
    {
      "text": { "translate": "questbuch.ankommen.werkbank.schritt.3", "fallback": "Craft a crafting table" },
      "items": ["minecraft:crafting_table"],
      "rezept": "minecraft:crafting_table"
    }
  ],
  "fundort": [
    {
      "text": { "translate": "questbuch.ankommen.werkbank.fundort", "fallback": "Trees grow almost everywhere" },
      "biome": ["minecraft:forest", "#minecraft:is_taiga"],
      "hoehe": { "von": 60, "bis": 120 },
      "dimension": "minecraft:overworld"
    }
  ]
}
```

| Feld | Pflicht | Bedeutung |
|---|---|---|
| `schritte` | nein | Liste, wird als 1., 2., 3. … angezeigt |
| `schritte[].text` | ja | Text-Komponente wie in den Fortschritten (`translate` + `fallback`), ein reiner String gilt als Übersetzungsschlüssel |
| `schritte[].items` | nein | Item-Symbole vor dem Text. Item-ID oder Tag mit `#` (bei Tags wechseln die Symbole alle 1 s) |
| `schritte[].rezept` | nein | Rezept-ID aus den Spieldaten (z. B. `minecraft:torch`). Zeigt das animierte Raster: Werkbank 3×3 (shaped/shapeless) oder Ofen (smelting, blasting, smoking, campfire_cooking). Unbekannte ID → kein Raster, Hinweis im Log |
| `fundort` | nein | Abschnitt „Wo finde ich das?“, mehrere Einträge möglich |
| `fundort[].text` | nein | freier Text |
| `fundort[].biome` | nein | Biom-IDs oder Biom-Tags (`#`), angezeigt mit Spielnamen (`biome.minecraft.forest`) |
| `fundort[].hoehe` | nein | `von`/`bis` (Y), eins davon reicht, z. B. `{ "bis": 16 }` → „Y ≤ 16“ |
| `fundort[].dimension` | nein | `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end` |

Regeln: Unbekannte Felder werden ignoriert, damit spätere Versionen kompatibel bleiben. Fehlerhafte Einträge werden einzeln übersprungen und mit Dateiname geloggt, die Leiste zeigt den Rest. Übersetzungen kommen in die vorhandenen `lang/de_de.json`/`en_us.json` des Resourcepacks.

Rezepte liest die Mod aus den Vanilla-Daten im Client (`data/minecraft/recipe/*.json`). Das funktioniert auch im Mehrspieler und unabhängig davon, ob das Rezept im Rezeptbuch freigeschaltet ist.
