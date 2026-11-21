package com.uniquebattery;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

final class ActivityIcons
{
	private static final Map<String, Skill> SKILLS = skills();
	private static final Map<String, HiscoreSkill> BOSSES = bosses();
	private final SkillIconManager skills;
	private final SpriteManager sprites;

	@Inject
	ActivityIcons(SkillIconManager skills, SpriteManager sprites)
	{
		this.skills = skills;
		this.sprites = sprites;
	}

	void load(String activity, Consumer<Icon> showIcon)
	{
		String key = activity.toLowerCase(Locale.ROOT);
		Skill skill = SKILLS.get(key);
		if (skill != null)
		{
			showIcon.accept(new ImageIcon(skills.getSkillImage(skill, true)));
			return;
		}
		HiscoreSkill boss = BOSSES.get(key);
		if (boss != null)
		{
			sprites.getSpriteAsync(boss.getSpriteId(), 0,
				image -> SwingUtilities.invokeLater(() -> showIcon.accept(new ImageIcon(image))));
		}
	}

	private static Map<String, Skill> skills()
	{
		Map<String, Skill> icons = new HashMap<>();
		for (Skill skill : Skill.values())
		{
			icons.put(skill.getName().toLowerCase(Locale.ROOT), skill);
		}
		return Map.copyOf(icons);
	}

	private static Map<String, HiscoreSkill> bosses()
	{
		Map<String, HiscoreSkill> icons = new HashMap<>();
		for (HiscoreSkill boss : HiscoreSkill.values())
		{
			if (boss.getType() == HiscoreSkillType.BOSS)
			{
				icons.put(boss.getName().toLowerCase(Locale.ROOT), boss);
			}
		}
		icons.put("gauntlet", HiscoreSkill.THE_GAUNTLET);
		icons.put("corrupted gauntlet", HiscoreSkill.THE_CORRUPTED_GAUNTLET);
		icons.put("fight caves", HiscoreSkill.TZTOK_JAD);
		icons.put("inferno", HiscoreSkill.TZKAL_ZUK);
		icons.put("challenge mode chambers of xeric", HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE);
		icons.put("hard mode theatre of blood", HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE);
		icons.put("yama contract", HiscoreSkill.YAMA);
		icons.put("dawn", HiscoreSkill.GROTESQUE_GUARDIANS);
		icons.put("dusk", HiscoreSkill.GROTESQUE_GUARDIANS);
		icons.put("the mimic", HiscoreSkill.MIMIC);
		icons.put("barrows", HiscoreSkill.BARROWS_CHESTS);
		for (String brother : new String[]{"ahrim the blighted", "dharok the wretched", "guthan the infested",
			"karil the tainted", "torag the corrupted", "verac the defiled"})
		{
			icons.put(brother, HiscoreSkill.BARROWS_CHESTS);
		}
		icons.put("branda the fire queen", HiscoreSkill.THE_ROYAL_TITANS);
		icons.put("eldric the ice king", HiscoreSkill.THE_ROYAL_TITANS);
		return Map.copyOf(icons);
	}
}
