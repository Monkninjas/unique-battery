package com.uniquebattery;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(UniqueBatteryConfig.GROUP)
public interface UniqueBatteryConfig extends Config
{
	String GROUP = "unique-battery";

	@Range(min = 0, max = 100)
	@ConfigItem(position = 0, keyName = "regularDrain", name = "Regular drain",
		description = "Battery percentage points lost per active minute during regular play.")
	default int regularDrain()
	{
		return 1;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(position = 1, keyName = "skillingDrain", name = "Skilling drain",
		description = "Battery percentage points lost per active minute while gaining non-combat XP outside a recognized boss encounter.")
	default int skillingDrain()
	{
		return 2;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(position = 2, keyName = "easyDrain", name = "Easy drain",
		description = "Battery percentage points lost per active minute during Easy Bossing Ladder encounters.")
	default int easyDrain()
	{
		return 2;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(position = 3, keyName = "mediumDrain", name = "Medium drain",
		description = "Battery percentage points lost per active minute during Medium Bossing Ladder encounters.")
	default int mediumDrain()
	{
		return 3;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(position = 4, keyName = "hardDrain", name = "Hard drain",
		description = "Battery percentage points lost per active minute during Hard Bossing Ladder encounters.")
	default int hardDrain()
	{
		return 4;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(position = 5, keyName = "eliteDrain", name = "Elite drain",
		description = "Battery percentage points lost per active minute during Elite Bossing Ladder encounters.")
	default int eliteDrain()
	{
		return 5;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(position = 6, keyName = "masterDrain", name = "Master drain",
		description = "Battery percentage points lost per active minute during Master Bossing Ladder encounters.")
	default int masterDrain()
	{
		return 6;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(position = 7, keyName = "grandmasterDrain", name = "Grandmaster drain",
		description = "Battery percentage points lost per active minute during Grandmaster Bossing Ladder encounters.")
	default int grandmasterDrain()
	{
		return 8;
	}


	@Range(min = 0, max = 500)
	@ConfigItem(position = 8, keyName = "rarityRefill", name = "Rarity refill strength (%)",
		description = "Scales the refill from drop rarity. Use 100 for the default or 0 to turn off rarity refills.")
	default int rarityRefill()
	{
		return 100;
	}

	@Range(min = 0, max = 500)
	@ConfigItem(position = 9, keyName = "valueRefill", name = "Value refill strength (%)",
		description = "Scales the refill from Grand Exchange value. Use 100 for the default or 0 to turn off value refills.")
	default int valueRefill()
	{
		return 100;
	}

	@ConfigItem(position = 10, keyName = "customUniques", name = "Extra unique drops",
		description = "Add or override drop odds using Source | Item | 1-in-N denominator. Separate entries with semicolons.")
	default String customUniques()
	{
		return "";
	}
}
