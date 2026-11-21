# Unique Battery

A RuneLite side-panel meter that drains while you play and recharges when Loot Tracker reports a recognized unique. Each character starts at 100% and keeps its own charge across sessions.

## Run locally

From this directory, run `.\gradlew.bat run` on Windows with JDK 17. This starts a RuneLite development client with the plugin available. Keep Loot Tracker enabled and open the battery icon in RuneLite's right sidebar. Look for **Unique Battery** in the plugin list to change settings. For Jagex accounts, follow [RuneLite's developer client sign-in instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

The packaged development client can also be run with `java -ea -jar .\build\libs\unique-battery-0.1.0-all.jar --developer-mode`. This build uses RuneLite 1.13.7.

## Plugin Hub

After RuneLite merges the submission, open **Plugin Hub** in RuneLite and search for **Unique Battery**. The submission uses a public source repository and a Plugin Hub pull request that pins the source commit. [RuneLite submission guide](https://github.com/runelite/plugin-hub#submitting-a-plugin)

## Drain

| Activity | Default charge lost per active minute |
| --- | ---: |
| Regular play | 1 point |
| Ordinary skilling | 2 points |
| Easy | 2 points |
| Medium | 3 points |
| Hard | 4 points |
| Elite | 5 points |
| Master | 6 points |
| Grandmaster | 8 points |

Each rate is adjustable. Set a rate to zero to pause drain for that activity. Non-combat XP opens a roughly 20-second skilling window and further gains extend it. Recognized encounters take priority, including the skilling bosses Wintertodt, Tempoross and Zalcano. Drain pauses when logged out or after about a minute without keyboard or mouse activity, unless you're still gaining skilling XP.

The tiers are based on a [bundled snapshot of the OSRS Wiki Bossing Ladder](https://oldschool.runescape.wiki/w/Guide:Bossing_Ladder?oldid=15408640). The plugin uses activity state or location where available and otherwise recognizes the NPC you're targeting. It assigns Elite to Tombs of Amascut below 300 invocation. It uses God Wars solo tiers when you're the only visible player in an instance. Hard Mode Theatre of Blood moves to Grandmaster after you target a recognized Hard Mode boss. Awakened Desert Treasure II bosses currently use the normal Elite tier.

## Refill

Adjust the rarity and value contributions independently. Higher drop denominators and higher Grand Exchange values each give more charge, with diminishing returns. A unique without a market value can still charge from rarity. Raid rates are reference values for scoring, since actual purple odds depend on the run.

The starter list covers selected uniques from Zulrah, Vorkath, Kraken, the Abyssal demon, the four original God Wars generals, Chambers of Xeric, Theatre of Blood, Tombs of Amascut, Wintertodt and Tempoross. You can add a Loot Tracker source and item name or override a built-in rate in **Extra unique drops**. For example, `Zulrah | Magic fang | 512` means approximately one in 512. Separate multiple entries with semicolons.

## In-game check

Open the battery side panel, lower your charge during regular play, and compare it with a recognized boss encounter. To test a refill without waiting for a rare drop, temporarily enter `Goblin | Bones | 10` under **Extra unique drops**, defeat a Goblin, then remove the test rule. Log out and back in to check that the charge persists.

Close the panel while the charge is changing, then reopen it. It should immediately show the current charge.
