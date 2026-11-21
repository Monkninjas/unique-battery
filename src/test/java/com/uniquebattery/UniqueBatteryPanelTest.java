package com.uniquebattery;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UniqueBatteryPanelTest
{
	@Test
	public void displaysLatestChargeWhenReopened() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			UniqueBatteryPanel panel = new UniqueBatteryPanel();
			JProgressBar meter = (JProgressBar) panel.getComponent(0);
			assertEquals("Log in to view", meter.getString());

			panel.showCharge(80);
			assertEquals(0, meter.getValue());
			panel.onActivate();
			assertEquals(80, meter.getValue());

			panel.onDeactivate();
			panel.showCharge(65);
			panel.showCharge(27);
			assertEquals(80, meter.getValue());
			panel.onActivate();
			assertEquals("27%", meter.getString());
		});
	}
}
