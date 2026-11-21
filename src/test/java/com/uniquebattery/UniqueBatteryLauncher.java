package com.uniquebattery;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class UniqueBatteryLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(UniqueBatteryPlugin.class);
		RuneLite.main(args);
	}
}
