package com.uniquebattery;

import com.google.inject.Provides;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.http.api.loottracker.LootRecordType;

@PluginDescriptor(
	name = "Unique Battery",
	description = "A side-panel battery charged by unique drops and drained by play.",
	tags = {"loot", "unique", "battery", "pvm"}
)
@PluginDependency(LootTrackerPlugin.class)
public final class UniqueBatteryPlugin extends Plugin
{
	private static final String CHARGE_KEY = "charge";
	private static final int SAVE_INTERVAL_TICKS = 50;
	private static final int IDLE_CLIENT_TICKS = 3_000;
	private static final int SKILLING_WINDOW_TICKS = 34;
	private static final Set<Skill> COMBAT_SKILLS = Set.of(
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC, Skill.PRAYER
	);

	@Inject private Client client;
	@Inject private ClientToolbar toolbar;
	@Inject private ConfigManager configManager;
	@Inject private UniqueBatteryConfig config;
	@Inject private ItemManager itemManager;
	@Inject private EncounterDetector encounters;

	private final Map<Skill, Integer> skillXp = new EnumMap<>(Skill.class);
	private NavigationButton navigation;
	private UniqueBatteryPanel panel;
	private Map<String, Map<String, Double>> uniqueDrops = Map.of();
	private final int[] encounterDrain = new int[BossingLadder.Tier.values().length];
	private int regularDrain;
	private int skillingDrain;
	private int rarityRefill;
	private int valueRefill;
	private boolean drainEnabled;
	private String profileKey;
	private double charge = Battery.FULL;
	private boolean dirty;
	private int ticksSinceSave;
	private int skillingTicks;

	@Provides
	UniqueBatteryConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(UniqueBatteryConfig.class);
	}

	@Override
	protected synchronized void startUp()
	{
		readSettings();
		uniqueDrops = UniqueDrops.withOverrides(config.customUniques());
		panel = new UniqueBatteryPanel();
		navigation = NavigationButton.builder()
			.tooltip("Unique Battery")
			.icon(UniqueBatteryPanel.icon())
			.priority(20)
			.panel(panel)
			.build();
		toolbar.addNavigation(navigation);
	}

	@Override
	protected synchronized void shutDown()
	{
		saveCharge();
		profileKey = null;
		resetActivity();
		toolbar.removeNavigation(navigation);
		navigation = null;
		panel = null;
	}

	@Subscribe
	public synchronized void onConfigChanged(ConfigChanged event)
	{
		if (!UniqueBatteryConfig.GROUP.equals(event.getGroup()) || CHARGE_KEY.equals(event.getKey()))
		{
			return;
		}
		if ("customUniques".equals(event.getKey()))
		{
			uniqueDrops = UniqueDrops.withOverrides(config.customUniques());
		}
		else
		{
			readSettings();
		}
	}

	@Subscribe
	public synchronized void onProfileChanged(ProfileChanged event)
	{
		readSettings();
		uniqueDrops = UniqueDrops.withOverrides(config.customUniques());
	}

	@Subscribe
	public synchronized void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR)
		{
			saveCharge();
			profileKey = null;
			resetActivity();
			updateMeter();
		}
	}

	@Subscribe
	public synchronized void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		if (client.getGameState() != GameState.LOGGED_IN || player == null || !loadProfile())
		{
			return;
		}

		if (drainEnabled && charge > 0 && (skillingTicks > 0
			|| client.getMouseIdleTicks() < IDLE_CLIENT_TICKS || client.getKeyboardIdleTicks() < IDLE_CLIENT_TICKS))
		{
			setCharge(Battery.drain(charge, drainRate(player)));
		}
		if (skillingTicks > 0)
		{
			--skillingTicks;
		}
		if (dirty && ++ticksSinceSave >= SAVE_INTERVAL_TICKS)
		{
			saveCharge();
		}
	}

	@Subscribe
	public synchronized void onStatChanged(StatChanged event)
	{
		if (COMBAT_SKILLS.contains(event.getSkill()) || client.getGameState() != GameState.LOGGED_IN || !loadProfile())
		{
			return;
		}
		Integer previous = skillXp.put(event.getSkill(), event.getXp());
		if (previous != null && event.getXp() > previous)
		{
			skillingTicks = SKILLING_WINDOW_TICKS;
		}
	}

	@Subscribe
	public synchronized void onLootReceived(LootReceived event)
	{
		if ((rarityRefill == 0 && valueRefill == 0) || client.getGameState() != GameState.LOGGED_IN
			|| (event.getType() != LootRecordType.NPC && event.getType() != LootRecordType.EVENT))
		{
			return;
		}

		Map<String, Double> sourceDrops = UniqueDrops.forSource(uniqueDrops, event.getName());
		if (sourceDrops.isEmpty() || !loadProfile() || charge >= Battery.FULL)
		{
			return;
		}
		double restored = charge;
		for (ItemStack stack : event.getItems())
		{
			long quantity = stack.getQuantity();
			if (quantity <= 0)
			{
				continue;
			}
			int id = itemManager.canonicalize(stack.getId());
			String item = itemManager.getItemComposition(id).getName();
			double denominator = UniqueDrops.denominator(sourceDrops, item);
			if (denominator > 1)
			{
				long unitPrice = valueRefill == 0 ? 0 : itemManager.getItemPrice(id);
				long value = unitPrice > Long.MAX_VALUE / quantity ? Long.MAX_VALUE : unitPrice * quantity;
				restored = Battery.refill(restored, denominator, value, rarityRefill, valueRefill);
				if (restored >= Battery.FULL)
				{
					break;
				}
			}
		}
		if (charge != restored)
		{
			setCharge(restored);
			saveCharge();
		}
	}

	private int drainRate(Player player)
	{
		BossingLadder.Tier tier = encounters.tier(player);
		return tier != null ? encounterDrain[tier.ordinal()] : skillingTicks > 0 ? skillingDrain : regularDrain;
	}

	private void readSettings()
	{
		regularDrain = config.regularDrain();
		skillingDrain = config.skillingDrain();
		encounterDrain[BossingLadder.Tier.EASY.ordinal()] = config.easyDrain();
		encounterDrain[BossingLadder.Tier.MEDIUM.ordinal()] = config.mediumDrain();
		encounterDrain[BossingLadder.Tier.HARD.ordinal()] = config.hardDrain();
		encounterDrain[BossingLadder.Tier.ELITE.ordinal()] = config.eliteDrain();
		encounterDrain[BossingLadder.Tier.MASTER.ordinal()] = config.masterDrain();
		encounterDrain[BossingLadder.Tier.GRANDMASTER.ordinal()] = config.grandmasterDrain();
		rarityRefill = config.rarityRefill();
		valueRefill = config.valueRefill();
		drainEnabled = regularDrain != 0 || skillingDrain != 0;
		for (int rate : encounterDrain)
		{
			drainEnabled |= rate != 0;
		}
	}

	private boolean loadProfile()
	{
		String currentKey = configManager.getRSProfileKey();
		if (currentKey == null)
		{
			return false;
		}
		if (!currentKey.equals(profileKey))
		{
			saveCharge();
			profileKey = currentKey;
			resetActivity();
			for (Skill skill : Skill.values())
			{
				skillXp.put(skill, client.getSkillExperience(skill));
			}
			Double saved = configManager.getConfiguration(UniqueBatteryConfig.GROUP, profileKey, CHARGE_KEY, Double.class);
			charge = saved == null ? Battery.FULL : Battery.clamp(saved);
			updateMeter();
		}
		return true;
	}

	private void resetActivity()
	{
		skillXp.clear();
		skillingTicks = 0;
		encounters.reset();
	}

	private void setCharge(double updated)
	{
		if (charge != updated)
		{
			charge = updated;
			dirty = true;
			updateMeter();
		}
	}

	private void saveCharge()
	{
		if (dirty && profileKey != null)
		{
			dirty = false;
			configManager.setConfiguration(UniqueBatteryConfig.GROUP, profileKey, CHARGE_KEY, charge);
		}
		ticksSinceSave = 0;
	}

	private void updateMeter()
	{
		if (panel != null)
		{
			panel.showCharge(profileKey == null ? -1 : (int) Math.ceil(charge));
		}
	}
}
