package com.uniquebattery;

import com.google.inject.Provides;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;
import net.runelite.http.api.loottracker.LootRecordType;

@PluginDescriptor(
	name = "Unique Battery",
	description = "A side-panel battery charged by unique drops and drained by play.",
	tags = {"loot", "unique", "battery", "pvm"}
)
public final class UniqueBatteryPlugin extends Plugin
{
	private static final String CHARGE_KEY = "charge";
	private static final String DRY_STREAKS_KEY = "dryStreaks";
	private static final int ACTIVITY_REFRESH_TICKS = 10;
	private static final int SAVE_INTERVAL_TICKS = 50;
	private static final int IDLE_CLIENT_TICKS = 3_000;
	private static final int SKILLING_WINDOW_TICKS = 34;
	private static final Set<Skill> COMBAT_SKILLS = Set.of(
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC, Skill.PRAYER
	);

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar toolbar;
	@Inject private ConfigManager configManager;
	@Inject private UniqueBatteryConfig config;
	@Inject private ItemManager itemManager;
	@Inject private EncounterDetector encounters;
	@Inject private ActivityIcons activityIcons;

	private final Map<Skill, Integer> skillXp = new EnumMap<>(Skill.class);
	private final BossProgress bossProgress = new BossProgress();
	private final BatteryActivityLog activityLog = new BatteryActivityLog();
	private NavigationButton navigation;
	private UniqueBatteryPanel panel;
	private Map<String, Map<String, Double>> uniqueDrops = Map.of();
	private Map<String, Double> personalBossRates = Map.of();
	private final int[] encounterDrain = new int[BossingLadder.Tier.values().length];
	private int regularDrain;
	private int skillingDrain;
	private int rarityRefill;
	private int valueRefill;
	private String profileKey;
	private double charge = Battery.FULL;
	private boolean dirty;
	private boolean progressDirty;
	private int ticksSinceSave;
	private int skillingTicks;
	private Skill currentSkill;
	private int activityTicks;
	private String activeActivity;
	private double activeDrainRate;

	@Provides
	UniqueBatteryConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(UniqueBatteryConfig.class);
	}

	@Override
	protected synchronized void startUp()
	{
		readSettings();
		readUniqueDrops();
		panel = new UniqueBatteryPanel(activityIcons, this::requestBatteryReset);
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
		saveState();
		profileKey = null;
		resetActivity();
		toolbar.removeNavigation(navigation);
		navigation = null;
		panel = null;
	}

	@Subscribe
	public synchronized void onConfigChanged(ConfigChanged event)
	{
		if (!UniqueBatteryConfig.GROUP.equals(event.getGroup()) || CHARGE_KEY.equals(event.getKey())
			|| DRY_STREAKS_KEY.equals(event.getKey()))
		{
			return;
		}
		if ("customUniques".equals(event.getKey()))
		{
			readUniqueDrops();
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
		readUniqueDrops();
	}

	@Subscribe
	public synchronized void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR)
		{
			saveState();
			profileKey = null;
			resetActivity();
			updateMeter();
			updateActivity();
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

		tickActivity(player);
		if (skillingTicks > 0)
		{
			--skillingTicks;
		}
		if ((dirty || progressDirty) && ++ticksSinceSave >= SAVE_INTERVAL_TICKS)
		{
			saveState();
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
			currentSkill = event.getSkill();
		}
	}

	@Subscribe
	public synchronized void onLootReceived(LootReceived event)
	{
		if (client.getGameState() != GameState.LOGGED_IN
			|| (event.getType() != LootRecordType.NPC && event.getType() != LootRecordType.EVENT))
		{
			return;
		}

		Map<String, Double> sourceDrops = UniqueDrops.forSource(uniqueDrops, event.getName());
		int doomDepth = DoomRates.NAME.equals(event.getName()) && event.getMetadata() instanceof Integer
			? (Integer) event.getMetadata() : 0;
		BossProgress.Entry boss = event.getType() == LootRecordType.NPC || doomDepth > 0
			? bossProgress.forBoss(event.getName()) : null;
		if (sourceDrops.isEmpty() || !loadProfile()
			|| (boss == null && (charge >= Battery.FULL || (rarityRefill == 0 && valueRefill == 0))))
		{
			return;
		}
		double restored = charge;
		boolean unique = false;
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
				unique = true;
				if (restored >= Battery.FULL)
				{
					break;
				}
				long unitPrice = valueRefill == 0 ? 0 : itemManager.getItemPrice(id);
				long value = unitPrice > Long.MAX_VALUE / quantity ? Long.MAX_VALUE : unitPrice * quantity;
				if (doomDepth > 0)
				{
					denominator = DoomRates.itemDenominator(doomDepth, item, denominator);
				}
				restored = Battery.refill(restored, denominator, value, rarityRefill, valueRefill);
				if (restored >= Battery.FULL)
				{
					break;
				}
			}
		}
		if (boss != null)
		{
			boss.complete(Math.max(1, event.getAmount()), unique, DoomRates.gearExposure(doomDepth));
			progressDirty = true;
		}
		if (charge != restored)
		{
			activityLog.refill(event.getName(), restored - charge);
			setCharge(restored);
			saveState();
		}
		else if (boss != null && unique)
		{
			saveState();
		}
		if (boss != null || unique)
		{
			updateActivity();
		}
	}

	@Subscribe
	public synchronized void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() != ScriptID.COLLECTION_DRAW_LIST
			|| client.getGameState() != GameState.LOGGED_IN
			|| client.getVarbitValue(VarbitID.COLLECTION_POH_HOST_BOOK_OPEN) == 1)
		{
			return;
		}
		Widget header = client.getWidget(InterfaceID.Collection.HEADER_TEXT);
		Widget items = client.getWidget(InterfaceID.Collection.ITEMS_CONTENTS);
		if (header == null || items == null || header.isHidden())
		{
			return;
		}
		Widget[] headerChildren = collectionLogChildren(header);
		Widget[] itemChildren = collectionLogChildren(items);
		if (headerChildren == null || headerChildren.length == 0 || headerChildren[0] == null || itemChildren == null)
		{
			return;
		}
		String source = Text.removeTags(headerChildren[0].getText()).trim();
		if ("The Doom of Mokhaiotl".equalsIgnoreCase(source))
		{
			source = DoomRates.NAME;
		}
		BossProgress.Entry boss = bossProgress.forBoss(source);
		if (boss == null || !loadProfile())
		{
			return;
		}
		if (boss.hasBaseline())
		{
			if (boss.startTracking())
			{
				saveCollectionBaseline();
			}
			return;
		}
		int completed = -1;
		for (Widget child : headerChildren)
		{
			if (child != null && child.getText() != null)
			{
				completed = CollectionLogBaseline.completionCount(child.getText(), DoomRates.NAME.equalsIgnoreCase(source));
				if (completed >= 0)
				{
					break;
				}
			}
		}
		if (completed < 0)
		{
			return;
		}
		Map<String, Double> tracked = UniqueDrops.forSource(uniqueDrops, source);
		int uniques = 0;
		for (Widget child : itemChildren)
		{
			if (child != null && child.getItemId() > 0 && child.getOpacity() == 0)
			{
				int id = itemManager.canonicalize(child.getItemId());
				if (UniqueDrops.denominator(tracked, itemManager.getItemComposition(id).getName()) > 1)
				{
					uniques += Math.max(1, child.getItemQuantity());
				}
			}
		}
		if (boss.seed(completed, uniques))
		{
			saveCollectionBaseline();
		}
	}

	private static Widget[] collectionLogChildren(Widget widget)
	{
		Widget[] children = widget.getDynamicChildren();
		return children == null || children.length == 0 ? widget.getChildren() : children;
	}

	private void saveCollectionBaseline()
	{
		progressDirty = true;
		saveState();
		updateActivity();
	}

	private void tickActivity(Player player)
	{
		String activity = null;
		double rate = 0;
		if (skillingTicks > 0 || client.getMouseIdleTicks() < IDLE_CLIENT_TICKS
			|| client.getKeyboardIdleTicks() < IDLE_CLIENT_TICKS)
		{
			BossingLadder.Tier tier = encounters.tier(player);
			activity = tier != null ? encounters.activityName()
				: skillingTicks > 0 ? currentSkill.getName() : "Regular play";
			rate = tier != null ? encounterDrain[tier.ordinal()] : skillingTicks > 0 ? skillingDrain : regularDrain;
			if (tier != null && !personalBossRates.isEmpty())
			{
				Double personalRate = personalBossRates.get(activity.toLowerCase(Locale.ROOT));
				if (personalRate != null)
				{
					rate = personalRate;
				}
			}
			BossProgress.Entry boss = tier == null ? null : bossProgress.forBoss(activity);
			if (boss != null)
			{
				progressDirty |= boss.startTracking();
				boss.recordActiveTick();
			}
			double drained = Battery.drain(charge, rate);
			activityLog.drain(activity, rate, charge - drained);
			setCharge(drained);
		}
		boolean changed = !Objects.equals(activeActivity, activity) || activeDrainRate != rate;
		activeActivity = activity;
		activeDrainRate = rate;
		if (changed || ++activityTicks >= ACTIVITY_REFRESH_TICKS)
		{
			updateActivity();
		}
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
		personalBossRates = BossingLadder.readPersonalRates(config.personalBossRates());
	}

	private void readUniqueDrops()
	{
		uniqueDrops = UniqueDrops.withOverrides(config.customUniques());
		bossProgress.updateRates(uniqueDrops);
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
			saveState();
			profileKey = currentKey;
			resetActivity();
			bossProgress.load(configManager.getConfiguration(UniqueBatteryConfig.GROUP, profileKey, DRY_STREAKS_KEY, String.class));
			for (Skill skill : Skill.values())
			{
				skillXp.put(skill, client.getSkillExperience(skill));
			}
			Double saved = configManager.getConfiguration(UniqueBatteryConfig.GROUP, profileKey, CHARGE_KEY, Double.class);
			charge = saved == null ? Battery.FULL : Battery.clamp(saved);
			updateMeter();
			updateActivity();
		}
		return true;
	}

	private void resetActivity()
	{
		skillXp.clear();
		skillingTicks = 0;
		currentSkill = null;
		activeActivity = null;
		activeDrainRate = 0;
		activityLog.clear();
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

	private synchronized void requestBatteryReset()
	{
		String requestedProfile = profileKey;
		if (requestedProfile != null)
		{
			clientThread.invokeLater(() -> resetBattery(requestedProfile));
		}
	}

	private synchronized void resetBattery(String requestedProfile)
	{
		if (panel == null || client.getGameState() != GameState.LOGGED_IN
			|| !requestedProfile.equals(profileKey) || !requestedProfile.equals(configManager.getRSProfileKey())
			|| charge >= Battery.FULL)
		{
			return;
		}
		activityLog.reset(Battery.FULL - charge);
		setCharge(Battery.FULL);
		saveState();
		updateActivity();
	}

	private void saveState()
	{
		if (dirty && profileKey != null)
		{
			dirty = false;
			configManager.setConfiguration(UniqueBatteryConfig.GROUP, profileKey, CHARGE_KEY, charge);
		}
		if (progressDirty && profileKey != null)
		{
			progressDirty = false;
			configManager.setConfiguration(UniqueBatteryConfig.GROUP, profileKey, DRY_STREAKS_KEY, bossProgress.save());
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

	private void updateActivity()
	{
		activityTicks = 0;
		if (panel == null)
		{
			return;
		}
		if (profileKey == null)
		{
			panel.showActivity("", List.of(), List.of(), 0, List.of());
			return;
		}
		String status = activeActivity == null ? "Drain paused"
			: activeActivity + "\n" + BatteryActivityLog.rate(activeDrainRate);
		panel.showActivity(status, activityLog.snapshot(), activityLog.groups(), charge, bossProgress.statuses());
	}
}
