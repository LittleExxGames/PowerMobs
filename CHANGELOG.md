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