package com.uniquebattery;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class UniqueDrops
{
	private static final Map<String, Map<String, Double>> BUILT_IN = builtIn();

	private UniqueDrops()
	{
	}

	static Map<String, Double> forSource(Map<String, Map<String, Double>> drops, String source)
	{
		return drops.getOrDefault(normalize(source), Map.of());
	}

	static double denominator(Map<String, Double> sourceDrops, String item)
	{
		return sourceDrops.getOrDefault(normalize(item), 0.0);
	}

	static Map<String, Map<String, Double>> withOverrides(String rules)
	{
		if (rules.isBlank())
		{
			return BUILT_IN;
		}
		Map<String, Map<String, Double>> overrides = new HashMap<>();
		for (String rule : rules.split(";"))
		{
			String[] fields = rule.split("\\|", -1);
			if (fields.length != 3 || fields[0].isBlank() || fields[1].isBlank())
			{
				continue;
			}
			try
			{
				double denominator = Double.parseDouble(fields[2].trim());
				if (Double.isFinite(denominator) && denominator > 1)
				{
					String source = normalize(fields[0]);
					overrides.computeIfAbsent(source, key -> new HashMap<>(BUILT_IN.getOrDefault(key, Map.of())))
						.put(normalize(fields[1]), denominator);
				}
			}
			catch (NumberFormatException ignored)
			{
				// Individual malformed entries leave the rest of the rules usable.
			}
		}
		Map<String, Map<String, Double>> combined = new HashMap<>(BUILT_IN);
		overrides.forEach((source, items) -> combined.put(source, Map.copyOf(items)));
		return Map.copyOf(combined);
	}

	private static String normalize(String name)
	{
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}

	private static Map<String, Map<String, Double>> builtIn()
	{
		Map<String, Map<String, Double>> drops = new HashMap<>();
		add(drops, "Zulrah", 512, "Tanzanite fang|Magic fang|Serpentine visage|Uncut onyx");
		add(drops, "Zulrah", 3_000, "Pet snakeling|Jar of swamp");
		add(drops, "Zulrah", 6_554, "Tanzanite mutagen|Magma mutagen");
		add(drops, "Vorkath", 5_000, "Draconic visage|Skeletal visage");
		add(drops, "Vorkath", 3_000, "Vorki|Jar of decay");
		add(drops, "Kraken", 512, "Trident of the seas (full)");
		add(drops, "Kraken", 400, "Kraken tentacle");
		add(drops, "Kraken", 3_000, "Pet kraken");
		add(drops, "Kraken", 1_000, "Jar of dirt");
		add(drops, "Abyssal demon", 512, "Abyssal whip");
		add(drops, "K'ril Tsutsaroth", 127, "Zamorakian spear");
		add(drops, "General Graardor", 381, "Bandos chestplate|Bandos tassets|Bandos boots");
		add(drops, "Commander Zilyana", 508, "Armadyl crossbow|Saradomin's light");
		add(drops, "Kree'arra", 381, "Armadyl helmet|Armadyl chestplate|Armadyl chainskirt");

		add(drops, "Reward cart (Wintertodt)", 1_000, "Tome of fire (empty)|Tome of fire");
		add(drops, "Reward cart (Wintertodt)", 10_000, "Dragon axe");
		add(drops, "Reward pool (Tempoross)", 400, "Fish barrel|Fish barrel (closed)|Tackle box");
		add(drops, "Reward pool (Tempoross)", 1_600, "Tome of water (empty)|Big harpoonfish");
		add(drops, "Reward pool (Tempoross)", 8_000, "Dragon harpoon");

		// Reference odds for scoring raid rewards. Actual odds depend on the run.
		add(drops, "Chambers of Xeric", 345, "Twisted bow|Kodai insignia|Elder maul");
		add(drops, "Chambers of Xeric", 86.25, "Dragon claws|Ancestral hat|Ancestral robe top|Ancestral robe bottom");
		add(drops, "Chambers of Xeric", 57.5, "Twisted buckler|Dragon hunter crossbow|Dinh's bulwark");
		add(drops, "Chambers of Xeric", 17.25, "Dexterous prayer scroll|Arcane prayer scroll");
		add(drops, "Theatre of Blood", 171, "Scythe of vitur (uncharged)");
		add(drops, "Theatre of Blood", 57, "Ghrazi rapier|Sanguinesti staff (uncharged)|Justiciar faceguard|Justiciar chestguard|Justiciar legguards");
		add(drops, "Theatre of Blood", 21.375, "Avernic defender hilt");
		add(drops, "Tombs of Amascut", 480, "Tumeken's shadow (uncharged)");
		add(drops, "Tombs of Amascut", 160, "Masori mask|Masori body|Masori chaps");
		add(drops, "Tombs of Amascut", 120, "Elidinis' ward");
		add(drops, "Tombs of Amascut", 68.57, "Osmumten's fang|Lightbearer");
		drops.replaceAll((source, items) -> Map.copyOf(items));
		return Map.copyOf(drops);
	}

	private static void add(Map<String, Map<String, Double>> drops, String source, double denominator, String items)
	{
		Map<String, Double> sourceDrops = drops.computeIfAbsent(normalize(source), key -> new HashMap<>());
		for (String item : items.split("\\|"))
		{
			sourceDrops.put(normalize(item), denominator);
		}
	}
}
