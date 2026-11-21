package com.uniquebattery;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BatteryTest
{
	@Test
	public void drainsAtTheConfiguredPerMinuteRate()
	{
		double charge = 100;
		for (int tick = 0; tick < 100; tick++)
		{
			charge = Battery.drain(charge, 5);
		}
		assertEquals(95, charge, 1e-9);
		assertEquals(0, Battery.drain(0.01, 5), 0);
		assertEquals(50, Battery.drain(50, 0), 0);
	}

	@Test
	public void rarerAndMoreValuableDropsRestoreMoreCharge()
	{
		double baseline = Battery.refill(0, 100, 10_000, 100, 100);
		assertTrue(Battery.refill(0, 1_000, 10_000, 100, 100) > baseline);
		assertTrue(Battery.refill(0, 100, 100_000, 100, 100) > baseline);
		assertEquals(0, Battery.refill(0, 100, 10_000, 0, 0), 0);
		assertEquals(24, Battery.refill(0, 1_000, 0, 100, 100), 0);
		assertEquals(100, Battery.refill(99, 100, 10_000, 100, 100), 0);
	}

	@Test
	public void uniqueRulesCheckSourceAndItemAndAllowOverrides()
	{
		Map<String, Map<String, Double>> drops = UniqueDrops.withOverrides(
			" zulrah | Magic Fang | 444;bad row;Kraken | Test drop | 100;Kraken | Invalid | Infinity");
		Map<String, Double> zulrah = UniqueDrops.forSource(drops, "Zulrah");
		Map<String, Double> kraken = UniqueDrops.forSource(drops, "Kraken");
		assertEquals(444, UniqueDrops.denominator(zulrah, "Magic fang"), 0);
		assertEquals(512, UniqueDrops.denominator(zulrah, "Tanzanite fang"), 0);
		assertEquals(0, UniqueDrops.denominator(UniqueDrops.forSource(drops, "Vorkath"), "Tanzanite fang"), 0);
		assertEquals(100, UniqueDrops.denominator(kraken, "Test drop"), 0);
		assertEquals(0, UniqueDrops.denominator(kraken, "Invalid"), 0);
		assertEquals(400, UniqueDrops.denominator(UniqueDrops.forSource(drops, "Reward pool (Tempoross)"), "Fish barrel"), 0);
		assertTrue(UniqueDrops.forSource(drops, "Cow").isEmpty());
	}

	@Test
	public void usesWikiTiersForBossesAndRaidThresholds()
	{
		assertEquals(BossingLadder.Tier.EASY, BossingLadder.npcTier("Scurrius", false));
		assertEquals(BossingLadder.Tier.ELITE, BossingLadder.npcTier("Vorkath", false));
		assertEquals(BossingLadder.Tier.MEDIUM, BossingLadder.npcTier("Kree'arra", false));
		assertEquals(BossingLadder.Tier.ELITE, BossingLadder.npcTier("Kree'arra", true));
		assertEquals(BossingLadder.Tier.ELITE, BossingLadder.tombsTier(299));
		assertEquals(BossingLadder.Tier.MASTER, BossingLadder.tombsTier(300));
		assertEquals(BossingLadder.Tier.MASTER, BossingLadder.tombsTier(499));
		assertEquals(BossingLadder.Tier.GRANDMASTER, BossingLadder.tombsTier(500));
	}
}
