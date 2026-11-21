package com.uniquebattery;

final class DoomRates
{
	static final String NAME = "Doom of Mokhaiotl";
	// Per-floor rates for the three gear uniques, from the OSRS Wiki.
	private static final int[] GEAR_DENOMINATOR = {0, 0, 2500, 2000, 1350, 810, 765, 720, 630, 540};
	private static final int[] GEAR_COUNT = {0, 0, 1, 2, 3, 3, 3, 3, 3, 3};

	private DoomRates()
	{
	}

	static double gearExposure(int depth)
	{
		double exposure = 0;
		for (int level = 2; level <= Math.min(depth, 9); level++)
		{
			exposure += GEAR_COUNT[level] / (double) GEAR_DENOMINATOR[level];
		}
		return exposure + Math.max(0, depth - 9) * deepDelveExposure();
	}

	static double deepDelveExposure()
	{
		return 3.0 / GEAR_DENOMINATOR[9];
	}

	static double itemDenominator(int depth, String item, double fallback)
	{
		String name = item.toLowerCase(java.util.Locale.ROOT);
		int first = "mokhaiotl cloth".equals(name) ? 2
			: name.startsWith("eye of ayak") ? 3 : "avernic treads".equals(name) ? 4 : 0;
		if (first == 0 || depth < first)
		{
			return fallback;
		}
		double miss = 1;
		for (int level = first; level <= Math.min(depth, 9); level++)
		{
			miss *= 1 - 1.0 / GEAR_DENOMINATOR[level];
		}
		miss *= Math.pow(1 - 1.0 / GEAR_DENOMINATOR[9], Math.max(0, depth - 9));
		return 1 / (1 - miss);
	}
}
