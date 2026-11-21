package com.uniquebattery;

import java.util.List;
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

	@Test
	public void dryScorePersistsAndSettlesPositiveAfterAnOverdueUnique()
	{
		BossProgress progress = new BossProgress();
		progress.updateRates(Map.of("zulrah", Map.of("magic fang", 100.0)));
		BossProgress.Entry zulrah = progress.forBoss("Zulrah");
		zulrah.complete(100, false);
		assertEquals(-1, zulrah.score(), 1e-9);
		zulrah.complete(50, false);
		assertEquals(-1.5, zulrah.score(), 1e-9);
		progress.load(progress.save());
		assertEquals(-1.5, zulrah.score(), 1e-9);
		zulrah.complete(150, false);
		assertEquals(-3, zulrah.score(), 1e-9);
		zulrah.complete(1, true);
		assertTrue(zulrah.score() > 0);
		progress.load(progress.save());
		assertTrue(zulrah.score() > 0);
	}

	@Test
	public void activeTimeDrivesPaceAndLogGroupsConsecutiveDrain()
	{
		BossProgress progress = new BossProgress();
		progress.updateRates(Map.of("zulrah", Map.of("magic fang", 100.0)));
		BossProgress.Entry zulrah = progress.forBoss("Zulrah");
		zulrah.complete(2, false);
		for (int tick = 0; tick < 200; tick++)
		{
			zulrah.recordActiveTick();
		}
		assertTrue(zulrah.summary().contains("60.0 completions/h"));

		BatteryActivityLog log = new BatteryActivityLog();
		log.drain("Zulrah", 5, 0.2);
		log.drain("Zulrah", 5, 0.3);
		log.drain("Fishing", 2, 1.0);
		log.drain("ZULRAH", 8, 1.0);
		log.refill("Zulrah", 27);
		List<BatteryActivityLog.Activity> entries = log.snapshot();
		assertEquals(4, entries.size());
		assertTrue(entries.get(0).isRefill());
		assertEquals(27, entries.get(0).chargeChange(), 0);
		assertEquals(-0.5, entries.get(3).chargeChange(), 0);
		assertEquals(2, log.groups().size());
		assertEquals("Zulrah", log.groups().get(0).name);
		assertEquals(25.5, log.groups().get(0).net, 0);
		log.reset(15);
		assertEquals(15, log.snapshot().get(0).chargeChange(), 0);
		assertEquals(2, log.groups().size());
		assertEquals(25.5, log.groups().get(0).net, 0);
	}

	@Test
	public void personalBossRatesAllowZeroAndIgnoreInvalidRows()
	{
		Map<String, Double> rates = BossingLadder.readPersonalRates(
			" Zulrah | 3.5; Vorkath | 0;Invalid;Kraken|NaN;Kree'arra|101;General Graardor|-1");
		assertEquals(Map.of("zulrah", 3.5, "vorkath", 0.0), rates);
	}

	@Test
	public void estimatesCollectionLogBaselineAndSettlesOnRateAsNeutral()
	{
		assertEquals(1_234, CollectionLogBaseline.completionCount("<col=ff00ff>Zulrah kills: 1,234</col>", false));
		assertEquals(42, CollectionLogBaseline.completionCount("Delves completed: 123<br>Deep delves completed: 42", true));
		assertEquals(1_234, CollectionLogBaseline.completionCount(
			"Total delves: 2,000<br>Deep delves:\u00a0<col=ffffff>1,234</col><br>Deepest delve: 9", true));
		assertEquals(-1, CollectionLogBaseline.completionCount("Total delves: 2,000<br>Deepest delve: 9", true));
		assertEquals(-1, CollectionLogBaseline.completionCount("Obtained: 3/6", false));
		BossProgress progress = new BossProgress();
		progress.updateRates(Map.of("zulrah", Map.of("magic fang", 100.0)));
		BossProgress.Entry zulrah = progress.forBoss("Zulrah");
		assertTrue(zulrah.seed(199, 1));
		assertEquals("Collection Log estimate", progress.statuses().get(0).baseline);
		progress.load(progress.save());
		assertEquals("Collection Log estimate", progress.statuses().get(0).baseline);
		assertEquals(-0.99, zulrah.score(), 1e-9);
		zulrah.complete(1, true);
		assertEquals(0, zulrah.score(), 1e-9);
		assertTrue(!zulrah.seed(300, 1));
		zulrah.complete(50, false);
		assertEquals(-0.5, zulrah.score(), 1e-9);
	}

	@Test
	public void doomUsesTheDepthOfEachClaimedRun()
	{
		assertEquals(0, DoomRates.gearExposure(1), 0);
		assertEquals(1.0 / 2500, DoomRates.gearExposure(2), 1e-9);
		assertEquals(2 * DoomRates.deepDelveExposure(), DoomRates.gearExposure(11) - DoomRates.gearExposure(9), 1e-9);
		assertTrue(DoomRates.itemDenominator(9, "Mokhaiotl cloth", 140)
			< DoomRates.itemDenominator(8, "Mokhaiotl cloth", 140));
		BossProgress.Entry doom = new BossProgress().forBoss(DoomRates.NAME);
		doom.complete(1, false, DoomRates.gearExposure(8));
		assertEquals(-DoomRates.gearExposure(8), doom.score(), 1e-9);

		BossProgress progress = new BossProgress();
		int deepDelves = CollectionLogBaseline.completionCount("Total delves: 400<br>Deep delves: 180", true);
		assertTrue(progress.forBoss(DoomRates.NAME).seed(deepDelves, 0));
		assertEquals(DoomRates.NAME, progress.statuses().get(0).name);
		assertEquals(-1, progress.statuses().get(0).score, 1e-9);
	}
}
