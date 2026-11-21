package com.uniquebattery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

final class BossProgress
{
	private final Map<String, Entry> bosses = new LinkedHashMap<>();

	BossProgress()
	{
		for (String boss : new String[]{"Zulrah", "Vorkath", "Kraken", "General Graardor", "Kree'arra",
			"Commander Zilyana", "K'ril Tsutsaroth"})
		{
			bosses.put(boss.toLowerCase(Locale.ROOT), new Entry(boss, false));
		}
		bosses.put(DoomRates.NAME.toLowerCase(Locale.ROOT), new Entry(DoomRates.NAME, true));
	}

	Entry forBoss(String name)
	{
		return name == null ? null : bosses.get(name.toLowerCase(Locale.ROOT));
	}

	void updateRates(Map<String, Map<String, Double>> uniques)
	{
		bosses.forEach((boss, entry) -> entry.expectedKills = expectedKills(UniqueDrops.forSource(uniques, boss)));
	}

	private static double expectedKills(Map<String, Double> items)
	{
		// Independent-roll approximation for the tracked subset of a drop table.
		double chanceOfMissing = 1;
		for (double denominator : items.values())
		{
			chanceOfMissing *= 1 - 1 / denominator;
		}
		return chanceOfMissing < 1 ? 1 / (1 - chanceOfMissing) : Double.POSITIVE_INFINITY;
	}

	void load(String saved)
	{
		bosses.values().forEach(Entry::reset);
		if (saved == null || saved.isEmpty())
		{
			return;
		}
		for (String row : saved.split(";"))
		{
			String[] fields = row.split("=", 2);
			Entry entry = forBoss(fields[0]);
			if (fields.length == 2 && entry != null)
			{
				try
				{
					entry.read(fields[1]);
				}
				catch (NumberFormatException ignored)
				{
					// A malformed saved entry only affects that boss.
				}
			}
		}
	}

	String save()
	{
		StringJoiner encoded = new StringJoiner(";");
		bosses.forEach((boss, entry) ->
		{
			if (entry.started || entry.dryKills > 0 || entry.seeded)
			{
				encoded.add(boss + "=" + entry.encode());
			}
		});
		return encoded.toString();
	}

	List<Status> statuses()
	{
		List<Status> result = new ArrayList<>();
		for (Entry entry : bosses.values())
		{
			if (entry.started)
			{
				result.add(new Status(entry.name, entry.score(), entry.summary(), entry.baselineNote()));
			}
		}
		return List.copyOf(result);
	}

	static final class Status
	{
		final String name;
		final double score;
		final String details;
		final String baseline;

		private Status(String name, double score, String details, String baseline)
		{
			this.name = name;
			this.score = score;
			this.details = details;
			this.baseline = baseline;
		}
	}

	static final class Entry
	{
		private final String name;
		private final boolean variableDepth;
		private double expectedKills = Double.POSITIVE_INFINITY;
		private int dryKills;
		private double depthExposure;
		private double settledScore;
		private boolean settled;
		private boolean seeded;
		private boolean estimated;
		private boolean started;
		private long completions;
		private long activeTicks;

		private Entry(String name, boolean variableDepth)
		{
			this.name = name;
			this.variableDepth = variableDepth;
		}

		private void reset()
		{
			dryKills = 0;
			depthExposure = 0;
			settledScore = 0;
			settled = false;
			seeded = false;
			estimated = false;
			started = false;
			completions = 0;
			activeTicks = 0;
		}

		void recordActiveTick()
		{
			activeTicks++;
		}

		boolean startTracking()
		{
			if (started)
			{
				return false;
			}
			started = true;
			return true;
		}

		void complete(int count, boolean unique)
		{
			complete(count, unique, 0);
		}

		void complete(int count, boolean unique, double exposure)
		{
			startTracking();
			completions += count;
			dryKills = (int) Math.min(Integer.MAX_VALUE, (long) dryKills + count);
			depthExposure += exposure;
			settled = unique;
			if (unique)
			{
				double atRate = variableDepth ? 1 : Math.round(expectedKills) / expectedKills;
				settledScore = Math.max(0, exposure() - atRate);
				dryKills = 0;
				depthExposure = 0;
				seeded = true;
				estimated = false;
			}
		}

		boolean seed(int count, int uniques)
		{
			if (seeded || (!variableDepth && !Double.isFinite(expectedKills)))
			{
				return false;
			}
			if (variableDepth)
			{
				depthExposure = Math.max(depthExposure, count * DoomRates.deepDelveExposure() - uniques);
			}
			else
			{
				long conservativeKills = Math.round(Math.max(0, count - uniques * expectedKills));
				dryKills = (int) Math.max(dryKills, conservativeKills);
			}
			seeded = true;
			estimated = true;
			startTracking();
			return true;
		}

		private double exposure()
		{
			return variableDepth ? depthExposure : dryKills / expectedKills;
		}

		double score()
		{
			return settled ? settledScore : -exposure();
		}

		boolean hasBaseline()
		{
			return seeded;
		}

		String summary()
		{
			String pace = completions == 0 || activeTicks < 100 ? "Measuring completion pace"
				: String.format(Locale.ROOT, "%.1f completions/h", completions * 6_000.0 / activeTicks);
			if (variableDepth)
			{
				return pace + String.format(Locale.ROOT, "\n%.2f expected gear uniques", exposure());
			}
			if (!Double.isFinite(expectedKills))
			{
				return pace;
			}
			return pace + String.format(Locale.ROOT, "\n%,d dry · ~%,d kills per unique", dryKills, Math.round(expectedKills));
		}

		private String baselineNote()
		{
			if (!seeded)
			{
				return "Open this boss’s Collection Log page once.";
			}
			return estimated ? "Collection Log estimate" : "Tracking observed drops";
		}

		private String encode()
		{
			int flags = 16 | (seeded ? 1 : 0) | (estimated ? 2 : 0) | (settled ? 4 : 0) | (started ? 8 : 0);
			return dryKills + "," + depthExposure + "," + settledScore + "," + flags;
		}

		private void read(String value)
		{
			String[] fields = value.split(",", -1);
			int count = Math.max(0, Integer.parseInt(fields[0]));
			if (fields.length == 1)
			{
				dryKills = count;
				started = count > 0;
				return;
			}
			if (fields.length != 4)
			{
				return;
			}
			double exposure = Double.parseDouble(fields[1]);
			double score = Double.parseDouble(fields[2]);
			int flags = Integer.parseInt(fields[3]);
			if (Double.isFinite(exposure) && exposure >= 0 && Double.isFinite(score) && score >= 0)
			{
				dryKills = count;
				depthExposure = exposure;
				settledScore = score;
				seeded = (flags & 1) != 0;
				estimated = (flags & 2) != 0;
				settled = (flags & 4) != 0;
				started = (flags & 16) != 0 ? (flags & 8) != 0 : count > 0 || settled;
			}
		}
	}
}
