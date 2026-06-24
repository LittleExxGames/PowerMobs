V1.3.3 - 6/23/2026
- Fixed a bug where abilities were not reloading properly when calling the command /powermob reload. (Summon ability fixed)
- Fixed default permissions for spawn key use to be enabled by default for everyone.
- Fixed /powermob populate not showing auto-complete options.
- Added the config.yml under /powermob populate under "main" so new configs can be generated.
- Fixed piercing damage not counting for damage tracking.
- Part 1 of a new system for stat tracking! Implementation in gameplay in part 2. The following stats are tracked:
Number of deaths to Power Mobs, Power Mob kills, max damage dealt in one fight against a Power Mob, and the total damage dealt to a Power Mob as a whole.
All of these are per Power Mob and per player. Many commands are now available to view these stats. This is stored on a local database in SQLite next to server configs for casual use
and includes a setup for mysql for multiserver external databases(or at least in theory. I haven't been able to test it).
- Added a new set of commands under "/powermob stats" to view the stats in the database.
- Added the new permissions into the base file for op.

Recommended: Run "/powermob populate main" to generate the missing configurations in config.yml.

V1.3.2 - 6/9/2026
- Fixed the power mobs spawn command to spawn in the center of a block instead of a corner.
- Added a new ability called "Vexed" that spawns rushing projectile like vexes towards a target.
- Added a new command "/powermob populate <abilities|items|mobs|keys|blockers>" to update missing config lines
or to populate the config with missing default values. No need to regenerate configs when new abilities, mobs, or default items are added!
Just run this new command.
- Added the update and populate commands to the help command list.

V1.3.1 - 5/19/2026
- Added structure-based spawn conditions for Power Mobs! This includes support for structures
in most datapacks.
- Shifted some UI items in the Spawn Conditions page to include the structure options. 

V1.3.0 - 5/12/2026
- Added Spawn Keys! You can spawn a specific Power Mob or have a list of potential Power Mobs spawn.
Also, you can have dialogue play before a Power Mob spawns through a key with some particle and sound effects.
- Added Spawn key debug option in the config.yml through "debugSpawnKeys:"
- A new spawnkey.use permission is added to allow players to use spawn keys or not.
- Added the option to have a drop ignore the drop range defined on a Power Mob through "ignore-drop-count" in the mobs config.
This is great for if you want a guaranteed drop from a Power Mob!
- Changed the functionality of the "glow" item config to be more accurate to its name. Shows or removes glow regardless of enchantments.
- Added a new item config option of "hide-enchantments" to hide enchantments from an item.
- Changed particle and sound configurations to use their own config subsection for items for clarity.
- Added a new command "/powermob update" to update the item configuration to the current item format.
- Changed item configuration to use the new format through the update command.
- Fixed a bug in the display command for Power Mobs info to actually display the item name instead of the literal object.

Recommended: Regenerate your config.yml, itemsconfig.yml, and mobsconfig.yml to reflect these changes.
Also, run "/powermob update" in-game to update your custom item configuration to the current item format.
Legacy formats will only be supported for a few versions so make sure you keep on top of this.
Let me know if there are any inconsistencies for updating the item configuration. (BACK UP YOUR CONFIG JUST IN CASE)

V1.2.2 - 3/22/2026
- Added customization for the announcement message that is sent when a mob spawns. (Found in config.yml)
- Changed spawn ranges to be bounding boxes. Now you can define multiple ranges for the mob to spawn in!
- Added a new ability: "Switcheroo" - This ability allows a mob to switch places with an attacking or nearby player.

Configs abilitiesconfig.yml, mobsconfig.yml, and config.yml have been updated to reflect these changes upon regeneration. 

V1.2.1 - 2/28/2026
- Fixed a spawn delay bug that made spawn delay calculations using the wrong values.
- Replaced dimensions settings with world settings so mobs can now specify which world they spawn in.(including dimensions)

Mobs config instructions have been updated to reflect this change upon mobconfig.yml regeneration.

V1.2.0 - 2/20/2026
- BIG UPDATE: Implemented effect stacking! Each effect defined on an item can be placed as a possible effect to trigger based on the result of its parent effect.
- Items target now is separated based on if it is AOE or not. AOE now uses 'center:' and single target use uses the original 'target:'.
- Particles now support much more customization.
- Updated default items to fit new requirements and to better represent example use.
- Adjusted some default values for some items.
- Added new default items - Smoke Bomb, Roulette Sword, and Damaged Ender Chestplate.
- Fixed attributes to actually be stackable when multiple of the same attribute are used.
- Added more warnings when certain item configs are invalid.

#### HIGHLY RECOMMENDED: Delete your config ymls to get the new default values and config instructions to generate. Save what you want to keep for what you made and paste it in after.

V1.1.0 - 1/29/2026
- BIG UPDATE: Custom mobs now can have their own configuration for each ability.
- Fixed and modified some default config values for some abilities.
- Fixed the lighting strike ability to work with projectile attacks.
- Added more config values to the lighting strike ability.
- Fixed spawn blocker items to be able to be added as a droppable item.
- Changed some default values for Random Power Mobs to be more plug-and-play.
- Updated the spawn chances for the default power mobs to make sure they can spawn in the world.
- Added a new default power mob.
- Changed a default item name ID for the Tasty Apple. I didn't follow my own naming convention of avoiding vanilla names...

#### HIGHLY RECOMMENDED: Delete your config ymls to get the new default values and config instructions to generate. Save what you want to keep for what you made and paste it in after.

V1.0.2 - 1/14/2026
- Fixed UI for some PowerMob values not updating max range when doing single values.
- Fixed teleport ability to limit the Y range to be within the defined max distance.

V1.0.1 - 1/11/2026
- UI item selection is now sorted by id.
- Added more default items.
- UI Filter option added to item selection for custom and vanilla items.

V1.0.0 - 1/7/2026
- Release!