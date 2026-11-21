package com.uniquebattery;

import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SkillIconManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UniqueBatteryPanelTest
{
	@Test
	public void displaysLatestChargeWhenReopened() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicInteger resetCount = new AtomicInteger();
			UniqueBatteryPanel panel = new UniqueBatteryPanel(
				new ActivityIcons(new SkillIconManager(), null), resetCount::incrementAndGet);
			JTabbedPane tabs = (JTabbedPane) panel.getComponent(0);
			assertEquals("Dry streaks", tabs.getTitleAt(1));
			JPanel battery = (JPanel) tabs.getComponentAt(0);
			JProgressBar meter = (JProgressBar) battery.getComponent(0);
			JPanel belowMeter = (JPanel) battery.getComponent(1);
			JPanel actions = (JPanel) belowMeter.getComponent(0);
			JButton reset = (JButton) actions.getComponent(0);
			assertEquals("Log in to view", meter.getString());
			assertFalse(reset.isVisible());

			panel.showCharge(80);
			assertEquals(0, meter.getValue());
			panel.onActivate();
			assertEquals(80, meter.getValue());
			assertTrue(reset.isVisible());
			reset.doClick(0);
			assertEquals(1, resetCount.get());

			panel.onDeactivate();
			panel.showCharge(65);
			panel.showCharge(27);
			assertEquals(80, meter.getValue());
			panel.onActivate();
			assertEquals("27%", meter.getString());
		});
	}
}
