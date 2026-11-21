package com.uniquebattery;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class BossingLadder
{
	enum Tier
	{
		EASY, MEDIUM, HARD, ELITE, MASTER, GRANDMASTER
	}

	// OSRS Wiki, Guide:Bossing Ladder, revision 15408640. Names here match RuneLite NPC names.
	// https://oldschool.runescape.wiki/w/Guide:Bossing_Ladder?oldid=15408640
	private static final Map<String, Tier> NPC_TIERS = npcTiers();

	private BossingLadder()
	{
	}

	static Tier npcTier(String npcName, boolean soloGodWars)
	{
		if (npcName == null)
		{
			return null;
		}
		String name = npcName.toLowerCase(Locale.ROOT);
		if (soloGodWars)
		{
			switch (name)
			{
				case "commander zilyana":
				case "k'ril tsutsaroth":
					return Tier.HARD;
				case "kree'arra":
				case "general graardor":
					return Tier.ELITE;
				default:
					break;
			}
		}
		return NPC_TIERS.get(name);
	}

	static boolean isGodWarsGeneral(String name)
	{
		return name != null && npcTier(name, true) != npcTier(name, false);
	}

	static Map<String, Double> readPersonalRates(String settings)
	{
		Map<String, Double> rates = new HashMap<>();
		for (String rule : settings.split(";"))
		{
			String[] fields = rule.split("\\|", -1);
			if (fields.length != 2 || fields[0].isBlank())
			{
				continue;
			}
			try
			{
				double rate = Double.parseDouble(fields[1].trim());
				if (Double.isFinite(rate) && rate >= 0 && rate <= 100)
				{
					rates.put(fields[0].trim().toLowerCase(Locale.ROOT), rate == 0 ? 0.0 : rate);
				}
			}
			catch (NumberFormatException ignored)
			{
				// A malformed override only affects that entry.
			}
		}
		return Map.copyOf(rates);
	}

	static Tier tombsTier(int raidLevel)
	{
		if (raidLevel >= 500)
		{
			return Tier.GRANDMASTER;
		}
		// The ladder starts at 300. Use Elite for lower-invocation runs.
		return raidLevel >= 300 ? Tier.MASTER : Tier.ELITE;
	}

	private static Map<String, Tier> npcTiers()
	{
		Map<String, Tier> tiers = new HashMap<>();
		add(tiers, Tier.EASY, "Brutus|Wintertodt|Tempoross|Obor|Bryophyta|Deranged archaeologist|Scurrius"
			+ "|Ahrim the Blighted|Dharok the Wretched|Guthan the Infested|Karil the Tainted|Torag the Corrupted|Verac the Defiled");
		add(tiers, Tier.MEDIUM, "Shellbane Gryphon|Penance Queen|Amoxliatl|The Hueycoatl|Hespori|Giant Mole"
			+ "|Crazy archaeologist|Chaos Fanatic|Kraken|Sarachnis|King Black Dragon|Zalcano|Thermonuclear smoke devil"
			+ "|The Mimic|Branda the Fire Queen|Eldric the Ice King|Blood Moon|Blue Moon|Eclipse Moon|Mad Angel"
			+ "|Commander Zilyana|K'ril Tsutsaroth|Kree'arra|General Graardor");
		add(tiers, Tier.HARD, "Scorpia|Chaos Elemental|Vet'ion|Calvar'ion|Venenatis|Spindel|Callisto|Artio"
			+ "|Dagannoth Prime|Dagannoth Rex|Dagannoth Supreme|Dawn|Dusk|Skotizo|TzTok-Jad|Demonic gorilla"
			+ "|Tormented Demon|Crystalline Hunllef|Abyssal Sire|Cerberus|Araxxor|Alchemical Hydra|Corporeal Beast|Kalphite Queen");
		add(tiers, Tier.ELITE, "Zulrah|Vorkath|Phantom Muspah|Duke Sucellus|Maggot King|Corrupted Hunllef"
			+ "|The Whisperer|The Leviathan|Vardorvis|Nex|Yama");
		add(tiers, Tier.MASTER, "The Nightmare|Nightmare|Phosani's Nightmare|Demonic Brutus|Doom of Mokhaiotl|The Doom of Mokhaiotl|Sol Heredit|TzKal-Zuk");
		return Map.copyOf(tiers);
	}

	private static void add(Map<String, Tier> tiers, Tier tier, String names)
	{
		for (String name : names.split("\\|"))
		{
			tiers.put(name.toLowerCase(Locale.ROOT), tier);
		}
	}
}
