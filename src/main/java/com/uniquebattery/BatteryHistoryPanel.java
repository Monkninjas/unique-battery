package com.uniquebattery;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

final class BatteryHistoryPanel extends JPanel
{
	private static final Color CHART_BACKGROUND = new Color(27, 31, 32);
	private static final Color GRID = new Color(68, 75, 76);
	private static final Color MUTED = new Color(166, 176, 177);
	private static final Color TEXT = new Color(235, 240, 241);
	private static final Color REFILL = new Color(117, 214, 154);
	private static final Color DRAIN = new Color(233, 167, 100);
	private static final Color REFILL_ROW = new Color(37, 53, 45);
	private static final Color DRAIN_ROW = new Color(53, 45, 37);
	private final ChargeTrace trace = new ChargeTrace();
	private final HistoryRow[] rows = new HistoryRow[BatteryActivityLog.LIMIT];

	BatteryHistoryPanel(ActivityIcons icons)
	{
		setOpaque(false);
		setAlignmentX(LEFT_ALIGNMENT);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(12, 0, 0, 0));
		JLabel title = new JLabel("Recent activity");
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		title.setBorder(new EmptyBorder(0, 1, 5, 0));
		title.setAlignmentX(LEFT_ALIGNMENT);
		add(title);
		trace.setAlignmentX(LEFT_ALIGNMENT);
		add(trace);
		add(Box.createVerticalStrut(5));
		for (int index = 0; index < rows.length; index++)
		{
			rows[index] = new HistoryRow(icons);
			add(rows[index]);
		}
	}

	void showHistory(List<BatteryActivityLog.Activity> activities, List<BatteryActivityLog.Group> groups, double charge)
	{
		setVisible(!groups.isEmpty() || !activities.isEmpty());
		trace.setVisible(!activities.isEmpty());
		trace.showHistory(activities, charge);
		for (int index = 0; index < rows.length; index++)
		{
			boolean inUse = index < groups.size();
			rows[index].setVisible(inUse);
			if (inUse)
			{
				rows[index].showGroup(groups.get(index));
			}
		}
	}

	private static final class HistoryRow extends JPanel
	{
		private final JLabel name = new JLabel();
		private final JLabel icon = new JLabel();
		private final JLabel detail = new JLabel();
		private final JLabel amount = new JLabel();
		private final JPanel body = new JPanel(new BorderLayout(4, 0));
		private final ActivityIcons icons;

		HistoryRow(ActivityIcons icons)
		{
			this.icons = icons;
			setOpaque(false);
			setAlignmentX(LEFT_ALIGNMENT);
			setLayout(new BorderLayout());
			setBorder(new EmptyBorder(0, 0, 4, 0));
			body.setBorder(new EmptyBorder(5, 7, 5, 7));
			JPanel labels = new JPanel(new BorderLayout());
			labels.setOpaque(false);
			name.setFont(name.getFont().deriveFont(Font.BOLD));
			name.setForeground(TEXT);
			detail.setForeground(MUTED);
			detail.setFont(detail.getFont().deriveFont(11f));
			labels.add(name, BorderLayout.NORTH);
			amount.setFont(amount.getFont().deriveFont(Font.BOLD));
			JPanel footer = new JPanel(new BorderLayout(4, 0));
			footer.setOpaque(false);
			footer.add(detail, BorderLayout.CENTER);
			footer.add(amount, BorderLayout.EAST);
			labels.add(footer, BorderLayout.SOUTH);
			icon.setVisible(false);
			body.add(icon, BorderLayout.WEST);
			body.add(labels, BorderLayout.CENTER);
			add(body);
		}

		void showGroup(BatteryActivityLog.Group group)
		{
			if (!group.name.equals(name.getText()))
			{
				name.setText(group.name);
				name.setToolTipText(group.name);
				icon.setIcon(null);
				icon.setVisible(false);
				icons.load(group.name, image ->
				{
					if (group.name.equals(name.getText()))
					{
						icon.setIcon(image);
						icon.setVisible(true);
					}
				});
			}
			boolean neutral = Math.abs(group.net) < 0.05;
			boolean positive = group.net > 0;
			String sign = neutral ? "" : positive ? "+" : "−";
			amount.setText(sign + String.format(Locale.ROOT, "%.1f pts", neutral ? 0.0 : Math.abs(group.net)));
			amount.setForeground(neutral ? MUTED : positive ? REFILL : DRAIN);
			body.setBackground(neutral ? CHART_BACKGROUND : positive ? REFILL_ROW : DRAIN_ROW);
			detail.setText(group.drainRate > 0 ? BatteryActivityLog.rate(group.drainRate) : "Unique refill");
		}
	}

	private static final class ChargeTrace extends JComponent
	{
		private static final int HEIGHT = 61;
		private List<BatteryActivityLog.Activity> activities = List.of();
		private double charge;

		ChargeTrace()
		{
			setPreferredSize(new Dimension(160, HEIGHT));
			setMinimumSize(new Dimension(0, HEIGHT));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
			setToolTipText("Charge across recent events and manual resets, oldest to newest");
		}

		void showHistory(List<BatteryActivityLog.Activity> recent, double currentCharge)
		{
			activities = recent;
			charge = currentCharge;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics original)
		{
			Graphics2D graphics = (Graphics2D) original.create();
			try
			{
				int width = getWidth();
				int height = getHeight();
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setColor(CHART_BACKGROUND);
				graphics.fillRoundRect(0, 0, width, height, 9, 9);
				int left = 28;
				int right = width - 9;
				int top = 11;
				int bottom = height - 10;
				if (right <= left || activities.isEmpty())
				{
					return;
				}
				graphics.setFont(graphics.getFont().deriveFont(9f));
				graphics.setColor(MUTED);
				graphics.drawString("100", 5, top + 3);
				graphics.drawString("0", 16, bottom + 3);
				graphics.setColor(GRID);
				graphics.drawLine(left, top, right, top);
				graphics.drawLine(left, bottom, right, bottom);
				graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				double level = charge;
				int pointX = right;
				int pointY = rowForCharge(level, top, bottom);
				for (int index = 0; index < activities.size(); index++)
				{
					BatteryActivityLog.Activity event = activities.get(index);
					level -= event.chargeChange();
					int previousX = right - (right - left) * (index + 1) / activities.size();
					int previousY = rowForCharge(level, top, bottom);
					graphics.setColor(event.isRefill() ? REFILL : DRAIN);
					graphics.drawLine(previousX, previousY, pointX, pointY);
					graphics.fillOval(pointX - 2, pointY - 2, 4, 4);
					pointX = previousX;
					pointY = previousY;
				}
			}
			finally
			{
				graphics.dispose();
			}
		}

		private static int rowForCharge(double charge, int top, int bottom)
		{
			return bottom - (int) Math.round((bottom - top) * Battery.clamp(charge) / Battery.FULL);
		}
	}
}
