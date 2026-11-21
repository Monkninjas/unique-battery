package com.uniquebattery;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

final class UniqueBatteryPanel extends PluginPanel
{
	private static final Color HIGH = new Color(85, 171, 111);
	private static final Color MEDIUM = new Color(210, 154, 67);
	private static final Color LOW = new Color(202, 95, 89);
	private final JProgressBar meter = new JProgressBar(0, 100);
	private volatile int currentPercent = -1;
	private volatile boolean active;
	private int displayedPercent = -1;

	UniqueBatteryPanel()
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(16, 10, 12, 10));
		meter.setPreferredSize(new Dimension(160, 26));
		meter.setStringPainted(true);
		meter.getAccessibleContext().setAccessibleName("Unique Battery charge");
		meter.setString("Log in to view");
		add(meter, BorderLayout.NORTH);
	}

	void showCharge(int percent)
	{
		if (currentPercent == percent)
		{
			return;
		}
		currentPercent = percent;
		if (active)
		{
			SwingUtilities.invokeLater(this::refreshMeter);
		}
	}

	@Override
	public void onActivate()
	{
		active = true;
		refreshMeter();
	}

	@Override
	public void onDeactivate()
	{
		active = false;
	}

	private void refreshMeter()
	{
		int percent = currentPercent;
		if (active && displayedPercent != percent)
		{
			meter.setValue(Math.max(0, percent));
			meter.setForeground(percent <= 20 ? LOW : percent <= 50 ? MEDIUM : HIGH);
			meter.setString(percent < 0 ? "Log in to view" : percent + "%");
			displayedPercent = percent;
		}
	}

	static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setColor(new Color(225, 229, 225));
			graphics.drawRoundRect(1, 4, 13, 9, 2, 2);
			graphics.fillRect(15, 7, 2, 4);
			graphics.setColor(HIGH);
			graphics.fillRect(3, 6, 8, 6);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}
}
