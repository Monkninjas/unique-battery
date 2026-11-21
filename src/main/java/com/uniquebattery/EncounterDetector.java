package com.uniquebattery;

import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;

import static com.uniquebattery.BossingLadder.Tier.*;

final class EncounterDetector
{
	private static final int WINTERTODT_REGION = 6461;
	private static final int TEMPOROSS_REGION = 12588;
	private static final int FIGHT_CAVES_REGION = 9551;
	private static final int INFERNO_REGION = 9043;
	private static final int CORRUPTED_GAUNTLET_REGION = 7768;

	private static final Set<Integer> HARD_THEATRE_NPCS = Set.of(
		NpcID.TOB_MAIDEN_100_HARD, NpcID.TOB_MAIDEN_70_HARD, NpcID.TOB_MAIDEN_50_HARD, NpcID.TOB_MAIDEN_30_HARD,
		NpcID.TOB_BLOAT_HARD, NpcID.NYLOCAS_BOSS_MELEE_HARD, NpcID.NYLOCAS_BOSS_MAGIC_HARD, NpcID.NYLOCAS_BOSS_RANGED_HARD,
		NpcID.TOB_SOTETSEG_COMBAT_HARD, NpcID.TOB_XARPUS_COMBAT_HARD,
		NpcID.VERZIK_PHASE1_HARD, NpcID.VERZIK_PHASE2_HARD, NpcID.VERZIK_PHASE3_HARD);

	private final Client client;
	private boolean hardTheatre;
	private String activityName;

	@Inject
	EncounterDetector(Client client)
	{
		this.client = client;
	}

	BossingLadder.Tier tier(Player player)
	{
		activityName = null;
		Actor target = player.getInteracting();
		NPC npc = target instanceof NPC ? (NPC) target : null;
		WorldView world = client.getTopLevelWorldView();
		boolean instanced = world.isInstance();

		BossingLadder.Tier raidTier = raidTier(instanced, npc);
		if (raidTier != null)
		{
			return raidTier;
		}

		int region = WorldPoint.fromLocalInstance(world.getScene(), player.getLocalLocation(), world.getPlane()).getRegionID();
		BossingLadder.Tier areaTier = areaTier(instanced, region);
		if (areaTier != null)
		{
			return areaTier;
		}
		if (npc == null)
		{
			return null;
		}

		String name = npc.getName();
		if ("The Doom of Mokhaiotl".equalsIgnoreCase(name))
		{
			name = DoomRates.NAME;
		}
		if ("Yama".equalsIgnoreCase(name) && client.getVarbitValue(VarbitID.YAMA_IMP_CONTRACT_SIGNED) > 0)
		{
			return identify("Yama contract", GRANDMASTER);
		}
		boolean soloGodWars = instanced && BossingLadder.isGodWarsGeneral(name) && isOnlyPlayer(world, player);
		return identify(name, BossingLadder.npcTier(name, soloGodWars));
	}

	String activityName()
	{
		return activityName;
	}

	void reset()
	{
		hardTheatre = false;
		activityName = null;
	}

	private BossingLadder.Tier identify(String name, BossingLadder.Tier tier)
	{
		activityName = tier == null ? null : name;
		return tier;
	}

	private BossingLadder.Tier raidTier(boolean instanced, NPC npc)
	{
		if (instanced && client.getVarbitValue(VarbitID.TOB_CLIENT_PARTYSTATUS) == 2)
		{
			hardTheatre |= npc != null && HARD_THEATRE_NPCS.contains(npc.getId());
			return hardTheatre ? identify("Hard Mode Theatre of Blood", GRANDMASTER)
				: identify("Theatre of Blood", MASTER);
		}
		hardTheatre = false;
		if (client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1)
		{
			return client.getVarbitValue(VarbitID.RAIDS_CHALLENGE_MODE) == 1
				? identify("Challenge Mode Chambers of Xeric", GRANDMASTER) : identify("Chambers of Xeric", MASTER);
		}
		if (instanced && client.getVarbitValue(VarbitID.TOA_CLIENT_PARTYSTATUS) == 2)
		{
			return identify("Tombs of Amascut", BossingLadder.tombsTier(client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL)));
		}
		if (instanced && client.getVarbitValue(VarbitID.TFA_CLIENT_PARTYSTATUS) == 2)
		{
			return identify("The Fractured Archive", MASTER);
		}
		return null;
	}

	private BossingLadder.Tier areaTier(boolean instanced, int region)
	{
		if (region == WINTERTODT_REGION || region == TEMPOROSS_REGION)
		{
			return identify(region == WINTERTODT_REGION ? "Wintertodt" : "Tempoross", EASY);
		}
		if (!instanced)
		{
			return null;
		}
		if (client.getVarbitValue(VarbitID.PLAYER_IN_GAUNTLET) == 1)
		{
			return region == CORRUPTED_GAUNTLET_REGION ? identify("Corrupted Gauntlet", ELITE) : identify("Gauntlet", HARD);
		}
		if (client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING) == 1)
		{
			return identify("Barbarian Assault", MEDIUM);
		}
		if (region == FIGHT_CAVES_REGION)
		{
			return identify("Fight Caves", HARD);
		}
		return region == INFERNO_REGION ? identify("Inferno", MASTER) : null;
	}

	private static boolean isOnlyPlayer(WorldView world, Player player)
	{
		for (Player other : world.players())
		{
			if (other != player)
			{
				return false;
			}
		}
		return true;
	}
}
