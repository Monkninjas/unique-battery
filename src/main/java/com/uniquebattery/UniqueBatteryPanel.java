package com.uniquebattery;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

final class UniqueBatteryPanel extends PluginPanel
{
	private static final Color HIGH = new Color(85, 171, 111);
	private static final Color MEDIUM = new Color(210, 154, 67);
	private static final Color LOW = new Color(202, 95, 89);
	private final JProgressBar meter = new BatteryMeter();
	private final JButton resetButton = new JButton("Reset battery");
	private final JPanel activityPanel = new JPanel();
	private final JTextArea currentActivity = textArea();
	private final BatteryHistoryPanel activityHistory;
	private final DryStreakPanel dryStreaks;
	private volatile ActivityView activityView = new ActivityView("", List.of(), List.of(), 0, List.of());
	private ActivityView shownActivity;
	private volatile int currentPercent = -1;
	private volatile boolean active;
	private int displayedPercent = -1;

	UniqueBatteryPanel(ActivityIcons icons, Runnable resetBattery)
	{
		activityHistory = new BatteryHistoryPanel(icons);
		dryStreaks = new DryStreakPanel(icons);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(7, 7, 9, 7));
		JPanel batteryTab = new JPanel(new BorderLayout());
		batteryTab.setOpaque(false);
		batteryTab.setBorder(new EmptyBorder(12, 4, 6, 4));
		meter.getAccessibleContext().setAccessibleName("Unique Battery charge");
		batteryTab.add(meter, BorderLayout.NORTH);
		activityPanel.setOpaque(false);
		activityPanel.setBorder(new EmptyBorder(14, 1, 0, 1));
		activityPanel.setLayout(new BorderLayout());
		currentActivity.getAccessibleContext().setAccessibleName("Current activity and drain rate");
		activityPanel.add(currentActivity, BorderLayout.NORTH);
		activityPanel.add(activityHistory, BorderLayout.CENTER);
		activityPanel.setVisible(false);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
		actions.setOpaque(false);
		resetButton.setToolTipText("Restore the battery to 100%");
		resetButton.addActionListener(event -> resetBattery.run());
		resetButton.setVisible(false);
		actions.add(resetButton);
		JButton rateGuide = new JButton("Rate guide");
		rateGuide.addActionListener(event -> showRateGuide());
		actions.add(rateGuide);

		JPanel belowMeter = new JPanel(new BorderLayout());
		belowMeter.setOpaque(false);
		belowMeter.add(actions, BorderLayout.NORTH);
		belowMeter.add(activityPanel, BorderLayout.CENTER);
		batteryTab.add(belowMeter, BorderLayout.CENTER);
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Battery", batteryTab);
		tabs.addTab("Dry streaks", dryStreaks);
		add(tabs, BorderLayout.CENTER);
	}

	private void showRateGuide()
	{
		JTextArea guide = textArea();
		guide.setColumns(43);
		guide.setRows(12);
		guide.setText("Open Configuration → Unique Battery → Personal boss drain rates.\n\n"
			+ "Use the activity name shown on the battery panel, followed by | and a base rate from 0 to 100 points per minute. "
			+ "Separate multiple bosses with semicolons.\n\n"
			+ "Zulrah | 3.5; Vorkath | 0\n\n"
			+ "This sets Zulrah to 3.5 points per minute and pauses drain at Vorkath. "
			+ "Bosses without an entry use their ladder rate.");
		guide.setCaretPosition(0);
		JOptionPane.showMessageDialog(this, guide, "Personal boss drain rates", JOptionPane.PLAIN_MESSAGE);
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

	void showActivity(String activeActivity, List<BatteryActivityLog.Activity> recentActivity,
		List<BatteryActivityLog.Group> groups, double charge, List<BossProgress.Status> streaks)
	{
		activityView = new ActivityView(activeActivity, recentActivity, groups, charge, streaks);
		if (active)
		{
			SwingUtilities.invokeLater(this::refreshActivity);
		}
	}

	@Override
	public void onActivate()
	{
		active = true;
		refreshMeter();
		refreshActivity();
	}

	private void refreshActivity()
	{
		ActivityView view = activityView;
		if (!active || shownActivity == view)
		{
			return;
		}
		shownActivity = view;
		if (!view.status.equals(currentActivity.getText()))
		{
			currentActivity.setText(view.status);
		}
		activityHistory.showHistory(view.history, view.groups, view.charge);
		dryStreaks.showStreaks(view.streaks);
		currentActivity.setVisible(!view.status.isEmpty());
		activityPanel.setVisible(!view.status.isEmpty() || !view.groups.isEmpty() || !view.history.isEmpty());
	}

	private static final class ActivityView
	{
		private final String status;
		private final List<BatteryActivityLog.Activity> history;
		private final List<BatteryActivityLog.Group> groups;
		private final double charge;
		private final List<BossProgress.Status> streaks;

		private ActivityView(String status, List<BatteryActivityLog.Activity> history,
			List<BatteryActivityLog.Group> groups, double charge, List<BossProgress.Status> streaks)
		{
			this.status = status;
			this.history = history;
			this.groups = groups;
			this.charge = charge;
			this.streaks = streaks;
		}
	}

	private static JTextArea textArea()
	{
		JTextArea area = new JTextArea(0, 16);
		area.setEditable(false);
		area.setOpaque(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setAlignmentX(LEFT_ALIGNMENT);
		return area;
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
			resetButton.setVisible(percent >= 0);
			displayedPercent = percent;
		}
	}

	private static final class BatteryMeter extends JProgressBar
	{
		private static final int WIDTH = 90;
		private static final int HEIGHT = 182;
		private static final int CASE_X = 9;
		private static final int CASE_WIDTH = WIDTH - 2 * CASE_X;
		private static final int CASE_HEIGHT = 140;
		private static final Color CASE_EDGE = new Color(163, 173, 178);
		private static final Color INTERIOR = new Color(23, 28, 31);
		private static final Color LABEL = new Color(237, 241, 242);
		private static final Color EMPTY_BOLT = new Color(223, 173, 76);
		private static final Path2D BOLT = createBolt();

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
				graphics.setColor(EMPTY_BOLT);
				graphics.fill(BOLT);
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
			graphics.setFont(getFont().deriveFont(Font.BOLD, signedIn ? 20f : 14f));
			FontMetrics metrics = graphics.getFontMetrics();
			int x = (WIDTH - metrics.stringWidth(label)) / 2;
			int baseline = signedIn ? 88 : 174;
			if (signedIn)
			{
				graphics.setColor(INTERIOR);
				graphics.drawString(label, x, baseline + 1);
			}
			graphics.setColor(LABEL);
			graphics.drawString(label, x, baseline);
		}

		private static Path2D createBolt()
		{
			Path2D bolt = new Path2D.Float();
			bolt.moveTo(49, 100);
			bolt.lineTo(35, 118);
			bolt.lineTo(44, 118);
			bolt.lineTo(41, 132);
			bolt.lineTo(55, 113);
			bolt.lineTo(46, 113);
			bolt.closePath();
			return bolt;
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
