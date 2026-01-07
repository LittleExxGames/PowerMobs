# PowerMobs

PowerMobs is a Paper/Bukkit plugin for creating **enhanced custom mobs** with configurable 
stats, abilities, equipment, drops, and spawn rules—plus **randomly generated “PowerMobs”** for variety.
It is to help create vanilla+ content that can be used in a normal survival playthrough without getting in the way.

## What you can do

### Custom / predefined PowerMobs
Define custom mobs in `mobsconfig.yml` with options like:
- **Base mob type** (ZOMBIE, SKELETON, etc.)
- **Custom names** (color codes supported)
- **Stats** (health, damage, speed) with single values or ranges
- **Abilities**
- **Equipment** (vanilla items or custom items)
- **Drops** (items, amounts, chances, enchantments, exp, and more!)
- **Spawn conditions** (dimension, biome groups, time of day, coordinate bounds, delays, and much more!)

## Configuration files

### Global values and Random Mobs
Controls plugin-wide behavior in `config.yml` such as:
- Debug flags
- Spawn chance and announcements
- Spawn timer rules / bypasses
- Random-mob generation rules (abilities, equipment, drops, weights, ranges, etc.)

### Custom items (weapons, armor, uniques)
Define items in `itemsconfig.yml`

Each item can include:
- Material, name, lore, glow, model data, unbreakable
- Enchantments and attributes
- A flexible **effects system** (potion effects, particles, sounds, knockback, AoE effects, immunity effects, and more!)

### Spawn blockers
Define spawn blocks in `spawnblocksconfig.yml`
- Material
- Name and lore
- Chunk range

### Predefined Mobs
Customize your mobs in `mobsconfig.yml` (Can be done in game UI)
- Each mob can define stats, abilities, equipment, drops, and detailed spawn conditions.

### Abilities
Defines ability parameters in `abilitiesconfig.yml` such as:
- Radiuses, chances, cooldowns, damage, durations, and more

## In-game UI
Most random-mob settings and many mob options can be edited in-game through the GUI pages.

## Debugging
Enable debug layers in `config.yml`:

## Compatibility notes

### LeveledMobs compatibility
In `externalplugins.yml` add:
`power-mobs:
  friendly-name: "power-mobs"
  plugin-name: "PowerMobs"
  key-name: "powermob.id"
  key-type: "metadata"
  requirement: "exists"`

Also, in LeveledMobs `rules.yml` under **External Plugins with Vanilla Stats and Minimized Nametags**, add `power-mobs` to the list.

### InfernalMobs compatibility
In InfernalMobs `config.yml`, under `enabledSpawnReasons:`, remove:
- `- CUSTOM`