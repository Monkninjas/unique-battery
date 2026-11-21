package com.uniquebattery;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
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
	private final JProgressBar meter = new BatteryMeter();
	private volatile int currentPercent = -1;
	private volatile boolean active;
	private int displayedPercent = -1;

	UniqueBatteryPanel()
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(16, 10, 12, 10));
		meter.getAccessibleContext().setAccessibleName("Unique Battery charge");
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
			meter.setString(percent < 0 ? "Log in to view" : percent + "%");
			displayedPercent = percent;
		}
	}

	private static final class BatteryMeter extends JProgressBar
	{
		private static final int WIDTH = 90;
		private static final int HEIGHT = 174;
		private static final int CASE_X = 9;
		private static final int CASE_WIDTH = WIDTH - 2 * CASE_X;
		private static final int CASE_HEIGHT = 140;
		private static final Color CASE_EDGE = new Color(163, 173, 178);
		private static final Color INTERIOR = new Color(23, 28, 31);
		private static final Color LABEL = new Color(237, 241, 242);
		private static final Color SIGN_IN_LABEL = new Color(179, 190, 195);

		private BatteryMeter()
		{
			super(0, 100);
			setPreferredSize(new Dimension(WIDTH, HEIGHT));
			setOpaque(false);
			setBorderPainted(false);
			setStringPainted(true);
			setString("Log in to view");
		}

		@Override
		protected void paintComponent(Graphics original)
		{
			Graphics2D graphics = (Graphics2D) original.create();
			try
			{
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				double scale = Math.min(1.0, getWidth() / (double) WIDTH);
				graphics.translate((getWidth() - WIDTH * scale) / 2, (getHeight() - HEIGHT * scale) / 2);
				graphics.scale(scale, scale);

				graphics.setColor(CASE_EDGE);
				graphics.fillRoundRect(31, 1, 28, 15, 9, 9);
				graphics.fillRoundRect(CASE_X, 11, CASE_WIDTH, CASE_HEIGHT, 16, 16);

				paintCharge(graphics, getValue());
				paintLabel(graphics, getString());
			}
			finally
			{
				graphics.dispose();
			}
		}

		private void paintCharge(Graphics2D graphics, int percent)
		{
			final int x = CASE_X + 3;
			final int y = 14;
			final int width = CASE_WIDTH - 6;
			final int height = CASE_HEIGHT - 6;
			RoundRectangle2D interior = new RoundRectangle2D.Float(x, y, width, height, 11, 11);
			graphics.setColor(INTERIOR);
			graphics.fill(interior);
			if (percent == 0)
			{
				return;
			}

			Color chargeColor = percent <= 20 ? LOW : percent <= 50 ? MEDIUM : HIGH;
			int fillHeight = (int) Math.ceil(height * percent / 100.0);
			Graphics2D fill = (Graphics2D) graphics.create();
			try
			{
				fill.clip(interior);
				fill.setPaint(new GradientPaint(x, y, chargeColor.brighter(), x + width, y, chargeColor.darker()));
				fill.fillRect(x, y + height - fillHeight, width, fillHeight);
			}
			finally
			{
				fill.dispose();
			}
		}

		private void paintLabel(Graphics2D graphics, String label)
		{
			boolean signedIn = label.endsWith("%");
			graphics.setFont(getFont().deriveFont(signedIn ? Font.BOLD : Font.PLAIN, signedIn ? 20f : 11f));
			FontMetrics metrics = graphics.getFontMetrics();
			int x = (WIDTH - metrics.stringWidth(label)) / 2;
			int baseline = signedIn ? 88 : 169;
			if (signedIn)
			{
				graphics.setColor(INTERIOR);
				graphics.drawString(label, x, baseline + 1);
			}
			graphics.setColor(signedIn ? LABEL : SIGN_IN_LABEL);
			graphics.drawString(label, x, baseline);
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
