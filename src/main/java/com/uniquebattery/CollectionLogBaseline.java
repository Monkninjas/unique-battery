package com.uniquebattery;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.util.Text;

final class CollectionLogBaseline
{
	private static final Pattern COUNTER = Pattern.compile(
		"(?i)^(?<label>.*(?:kill(?:s| count)|delves completed))\\s*:\\s*(?<count>[0-9][0-9,]*)$");

	private CollectionLogBaseline()
	{
	}

	static int completionCount(String widgetText, boolean doom)
	{
		for (String line : Text.removeTags(widgetText.replace("<br>", "\n")).split("\\R"))
		{
			Matcher match = COUNTER.matcher(line.trim());
			if (match.matches())
			{
				String label = match.group("label").toLowerCase(Locale.ROOT);
				if (doom ? label.contains("deep delves") : !label.contains("delves"))
				{
					try
					{
						return Integer.parseInt(match.group("count").replace(",", ""));
					}
					catch (NumberFormatException ignored)
					{
						return -1;
					}
				}
			}
		}
		return -1;
	}
}
