package com.uniquebattery;

final class Battery
{
	static final double FULL = 100;
	private static final double TICKS_PER_MINUTE = 100;

	private Battery()
	{
	}

	static double drain(double charge, double pointsPerMinute)
	{
		return clamp(charge - pointsPerMinute / TICKS_PER_MINUTE);
	}

	static double refill(double charge, double denominator, long value, int rarityStrength, int valueStrength)
	{
		double rarityPoints = Math.min(50, 8 * Math.log10(denominator));
		double valuePoints = Math.min(50, 5 * Math.log10(1 + Math.max(0, value) / 1_000.0));
		return clamp(charge + rarityPoints * rarityStrength / 100 + valuePoints * valueStrength / 100);
	}

	static double clamp(double charge)
	{
		return Double.isFinite(charge) ? Math.max(0, Math.min(FULL, charge)) : FULL;
	}
}
