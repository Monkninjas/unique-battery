package com.uniquebattery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BatteryActivityLog
{
	static final int LIMIT = 8;
	private final Deque<Entry> entries = new ArrayDeque<>();
	private final Map<String, Total> totals = new LinkedHashMap<>(16, 0.75f, true);

	void drain(String activity, double rate, double amount)
	{
		if (amount <= 0)
		{
			return;
		}
		Total total = total(activity);
		total.drained += amount;
		total.rate = rate;
		Entry latest = entries.peekFirst();
		if (latest != null && latest.rate == rate && latest.activity.equals(activity))
		{
			latest.amount += amount;
			return;
		}
		add(new Entry(activity, rate, amount));
	}

	void refill(String source, double amount)
	{
		total(source).refilled += amount;
		add(new Entry(source, 0, amount));
	}

	void reset(double amount)
	{
		add(new Entry("Manual reset", 0, amount));
	}

	private Total total(String name)
	{
		return totals.computeIfAbsent(name.toLowerCase(Locale.ROOT), key -> new Total(name));
	}

	void clear()
	{
		entries.clear();
		totals.clear();
	}

	List<Group> groups()
	{
		List<Total> ordered = new ArrayList<>(totals.values());
		List<Group> snapshot = new ArrayList<>(Math.min(ordered.size(), LIMIT));
		for (int index = ordered.size() - 1; index >= 0 && snapshot.size() < LIMIT; index--)
		{
			Total total = ordered.get(index);
			snapshot.add(new Group(total.name, total.rate, total.refilled - total.drained));
		}
		return List.copyOf(snapshot);
	}

	List<Activity> snapshot()
	{
		List<Activity> result = new ArrayList<>(entries.size());
		for (Entry entry : entries)
		{
			result.add(new Activity(entry.activity, entry.rate, entry.amount));
		}
		return List.copyOf(result);
	}

	static String rate(double rate)
	{
		return String.format(Locale.ROOT, "%.1f points/min", rate);
	}

	private void add(Entry entry)
	{
		entries.addFirst(entry);
		if (entries.size() > LIMIT)
		{
			entries.removeLast();
		}
	}

	static final class Activity
	{
		final String name;
		final double rate;
		final double amount;

		private Activity(String name, double rate, double amount)
		{
			this.name = name;
			this.rate = rate;
			this.amount = amount;
		}

		boolean isRefill()
		{
			return rate == 0;
		}

		double chargeChange()
		{
			return isRefill() ? amount : -amount;
		}

	}

	static final class Group
	{
		final String name;
		final double drainRate;
		final double net;

		private Group(String name, double drainRate, double net)
		{
			this.name = name;
			this.drainRate = drainRate;
			this.net = net;
		}
	}

	private static final class Total
	{
		private final String name;
		private double rate;
		private double drained;
		private double refilled;

		private Total(String name)
		{
			this.name = name;
		}
	}

	private static final class Entry
	{
		private final String activity;
		private final double rate;
		private double amount;

		private Entry(String activity, double rate, double amount)
		{
			this.activity = activity;
			this.rate = rate;
			this.amount = amount;
		}
	}
}
