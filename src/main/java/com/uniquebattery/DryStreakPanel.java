package com.uniquebattery;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

final class DryStreakPanel extends JPanel
{
	private static final Color CARD = new Color(38, 44, 46);
	private static final Color TEXT = new Color(235, 240, 241);
	private static final Color MUTED = new Color(176, 186, 186);
	private static final Color DRY = new Color(233, 167, 100);
	private static final Color RECOVERED = new Color(117, 214, 154);
	private final ActivityIcons icons;
	private final Map<String, BossRow> rows = new LinkedHashMap<>();
	private final JTextArea empty = text("Start a supported boss to track its dry streak.");

	DryStreakPanel(ActivityIcons icons)
	{
		this.icons = icons;
		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(10, 2, 0, 2));
		empty.setAlignmentX(LEFT_ALIGNMENT);
		add(empty);
	}

	void showStreaks(List<BossProgress.Status> bosses)
	{
		rows.entrySet().removeIf(entry ->
		{
			if (bosses.stream().anyMatch(boss -> boss.name.equals(entry.getKey())))
			{
				return false;
			}
			remove(entry.getValue());
			return true;
		});
		empty.setVisible(bosses.isEmpty());
		for (BossProgress.Status boss : bosses)
		{
			BossRow row = rows.computeIfAbsent(boss.name, name ->
			{
				BossRow created = new BossRow(name, icons);
				add(created);
				return created;
			});
			row.show(boss);
		}
	}

	private static JTextArea text(String content)
	{
		JTextArea area = new JTextArea(content, 0, 17);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setEditable(false);
		area.setOpaque(false);
		area.setForeground(MUTED);
		return area;
	}

	private static final class BossRow extends JPanel
	{
		private final JLabel score = new JLabel();
		private final JTextArea details = text("");

		BossRow(String name, ActivityIcons icons)
		{
			setOpaque(false);
			setAlignmentX(LEFT_ALIGNMENT);
			setLayout(new BorderLayout());
			setBorder(new EmptyBorder(0, 0, 6, 0));
			JPanel card = new JPanel(new BorderLayout(0, 5));
			card.setBackground(CARD);
			card.setBorder(new EmptyBorder(7, 8, 7, 8));
			JLabel title = new JLabel(name);
			title.setForeground(TEXT);
			title.setFont(title.getFont().deriveFont(Font.BOLD));
			icons.load(name, title::setIcon);
			card.add(title, BorderLayout.NORTH);
			card.add(details, BorderLayout.CENTER);
			score.setFont(score.getFont().deriveFont(Font.BOLD));
			card.add(score, BorderLayout.SOUTH);
			add(card);
		}

		void show(BossProgress.Status boss)
		{
			String content = boss.details + "\n" + boss.baseline;
			if (!details.getText().equals(content))
			{
				details.setText(content);
			}
			boolean neutral = Math.abs(boss.score) < 0.005;
			String sign = neutral ? "" : boss.score > 0 ? "+" : "−";
			score.setText("Dry score " + sign + String.format(Locale.ROOT, "%.2f", neutral ? 0 : Math.abs(boss.score)));
			score.setForeground(neutral ? MUTED : boss.score > 0 ? RECOVERED : DRY);
		}
	}
}
