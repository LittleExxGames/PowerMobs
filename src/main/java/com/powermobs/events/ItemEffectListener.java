package com.powermobs.events;

import com.powermobs.PowerMobsPlugin;
import com.powermobs.mobs.equipment.ItemEffect;
import com.powermobs.mobs.equipment.items.EffectType;
import com.powermobs.mobs.equipment.items.TargetType;
import com.powermobs.mobs.equipment.items.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;

public class ItemEffectListener implements Listener {

    private final PowerMobsPlugin plugin;
    private final NamespacedKey projectileItemKey; // tag projectiles with firing item id

    public ItemEffectListener(PowerMobsPlugin plugin) {
        this.plugin = plugin;
        this.projectileItemKey = new NamespacedKey(plugin, "proj-item-id");
        // Periodic HOLDING effects (once per second)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                applyForItems(p, TriggerType.HOLDING, p, null);
                applyForHotbar(p);
                applyForInventory(p);
                applyForArmor(p, TriggerType.EQUIPPED, p, null);

                // Maintain persistent immunities passively
                if (hasPersistentFireImmunity(p)) {
                    p.setFireTicks(0);
                }
            }
        }, 20L, 20L);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent e) {
        LivingEntity attacker = resolveAttacker(e.getDamager());
        LivingEntity victim = e.getEntity() instanceof LivingEntity le ? le : null;

        if (attacker != null) {
            applyForItems(attacker, TriggerType.ON_HIT, attacker, victim);
            applyForArmor(attacker, TriggerType.ON_HIT, attacker, victim); // CHANGED
        }
        if (victim != null) {
            applyForItems(victim, TriggerType.ON_HIT_TAKEN, victim, attacker);
            applyForArmor(victim, TriggerType.ON_HIT_TAKEN, victim, attacker); // CHANGED
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer != null) {
            applyForItems(killer, TriggerType.ON_KILL, killer, e.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        // Process both hands once (only on MAIN hand event to avoid double-fire)
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player player = e.getPlayer();
        processRightClickItem(player, player.getInventory().getItemInMainHand());
        processRightClickItem(player, player.getInventory().getItemInOffHand());
    }

    private void processRightClickItem(Player player, ItemStack item) {
        if (item == null) return;
        var list = plugin.getItemEffectManager().getItemEffects(item, TriggerType.RIGHT_CLICK);
        if (list.isEmpty()) return;
        for (var eff : list) {
            plugin.getItemEffectProcessor().processEffect(eff, player, null, player.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent e) {
        var tool = e.getPlayer().getInventory().getItemInMainHand();
        if (tool == null) return;

        var list = plugin.getItemEffectManager().getItemEffects(tool, TriggerType.BLOCK_BREAK);
        if (list.isEmpty()) return;

        for (var eff : list) {
            plugin.getItemEffectProcessor().processEffect(eff, e.getPlayer(), null, e.getPlayer().getLocation());
        }
    }

    // Block vanilla potion adds/changes (wither, poison, slowness, etc.) if immune
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (event.getNewEffect() == null) return; // nothing added/changed

        PotionEffectType type = event.getNewEffect().getType();
        int levelOneBased = event.getNewEffect().getAmplifier() + 1;
        if (hasPersistentPotionImmunity(living, type, levelOneBased)) {
            event.setCancelled(true);
            plugin.debug("[Immunity] Prevented potion " + type.getName() + " L" + levelOneBased + " on " + living.getName(), "item_effects");
        }
    }

    // Prevent catching fire from lava/blocks/entity damage if immune
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (hasPersistentFireImmunity(living)) {
            event.setCancelled(true);
            living.setFireTicks(0);
            plugin.debug("[Immunity] Prevented combustion on " + living.getName(), "item_effects");
        }
    }

    // Prevent damage sources tied to immunities (fire, freeze, wither, poison)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageVanilla(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        switch (event.getCause()) {
            case FIRE, FIRE_TICK, LAVA -> {
                if (hasPersistentFireImmunity(living)) {
                    event.setCancelled(true);
                    living.setFireTicks(0);
                    plugin.debug("[Immunity] Prevented fire/lava damage for " + living.getName(), "item_effects");
                }
            }
            case WITHER -> {
                int amp = getActiveAmplifier(living, PotionEffectType.WITHER);
                if (amp >= 0 && hasPersistentPotionImmunity(living, PotionEffectType.WITHER, amp + 1)) {
                    event.setCancelled(true);
                    living.removePotionEffect(PotionEffectType.WITHER);
                    plugin.debug("[Immunity] Prevented WITHER damage for " + living.getName(), "item_effects");
                }
            }
            case POISON -> {
                int amp = getActiveAmplifier(living, PotionEffectType.POISON);
                if (amp >= 0 && hasPersistentPotionImmunity(living, PotionEffectType.POISON, amp + 1)) {
                    event.setCancelled(true);
                    living.removePotionEffect(PotionEffectType.POISON);
                    plugin.debug("[Immunity] Prevented POISON damage for " + living.getName(), "item_effects");
                }
            }
            case FALL -> {
                if (hasPersistentFallImmunity(living)) {
                    event.setCancelled(true);
                    plugin.debug("[Immunity] Prevented FALL damage for " + living.getName(), "item_effects");
                }
            }
            default -> { /* no-op */ }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConsume(PlayerItemConsumeEvent e) {
        applyForConsumedItem(e.getPlayer(), e.getItem());
    }

    // When a bow/crossbow (and similar) is used to shoot
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onShootBow(EntityShootBowEvent e) {
        LivingEntity shooter = e.getEntity();
        ItemStack weapon = e.getBow();
        if (weapon == null) return;

        // Fire SHOOT_PROJECTILE for the bow/crossbow used
        var list = plugin.getItemEffectManager().getItemEffects(weapon, TriggerType.SHOOT_PROJECTILE);
        if (!list.isEmpty()) {
            for (ItemEffect eff : list) {
                plugin.getItemEffectProcessor().processEffect(eff, shooter, null, shooter.getLocation());
            }
        }

        // Tag projectile with the item's custom id for PROJECTILE_HIT resolution
        String id = plugin.getItemEffectManager().getCustomItemId(weapon);
        if (id != null) {
            e.getProjectile().getPersistentDataContainer().set(projectileItemKey, PersistentDataType.STRING, id);
        }
    }

    // Fallback for other launched projectiles (e.g., trident)
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        Projectile proj = e.getEntity();
        ProjectileSource src = proj.getShooter();
        if (!(src instanceof LivingEntity shooter)) return;

        // Handle trident throw or other hand-thrown items by checking hands
        ItemStack main = shooter.getEquipment() != null ? shooter.getEquipment().getItemInMainHand() : null;
        ItemStack off = shooter.getEquipment() != null ? shooter.getEquipment().getItemInOffHand() : null;

        // Prefer main hand; if nothing matches, try offhand
        if (!processShootForItem(shooter, proj, main)) {
            processShootForItem(shooter, proj, off);
        }
    }

    private boolean processShootForItem(LivingEntity shooter, Projectile proj, ItemStack item) {
        if (item == null) return false;
        var list = plugin.getItemEffectManager().getItemEffects(item, TriggerType.SHOOT_PROJECTILE);
        if (list.isEmpty()) return false;

        for (ItemEffect eff : list) {
            plugin.getItemEffectProcessor().processEffect(eff, shooter, null, shooter.getLocation());
        }
        String id = plugin.getItemEffectManager().getCustomItemId(item);
        if (id != null) {
            proj.getPersistentDataContainer().set(projectileItemKey, PersistentDataType.STRING, id);
        }
        return true;
    }

    // Apply effects when the tagged projectile hits something
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        ProjectileSource src = proj.getShooter();
        if (!(src instanceof LivingEntity shooter)) return;

        String id = proj.getPersistentDataContainer().get(projectileItemKey, PersistentDataType.STRING);
        if (id == null) return;

        var effects = plugin.getItemEffectManager().getItemEffects(id);
        if (effects == null || effects.isEmpty()) return;

        LivingEntity hitEntity = (e.getHitEntity() instanceof LivingEntity le) ? le : null;
        Location hitLoc =
                (hitEntity != null) ? hitEntity.getLocation() :
                        (e.getHitBlock() != null) ? e.getHitBlock().getLocation() :
                                proj.getLocation();

        for (ItemEffect eff : effects) {
            if (eff.getTrigger() != TriggerType.PROJECTILE_HIT) continue;
            plugin.getItemEffectProcessor().processEffect(eff, shooter, hitEntity, hitLoc);
        }
    }

    private int getActiveAmplifier(LivingEntity entity, PotionEffectType type) {
        for (PotionEffect pe : entity.getActivePotionEffects()) {
            if (pe.getType().equals(type)) {
                return pe.getAmplifier();
            }
        }
        return -1;
    }

    private void applyForItems(LivingEntity holder, TriggerType trigger, LivingEntity triggerEntity, LivingEntity target) {
        for (ItemStack stack : getHeldItems(holder)) {
            var list = plugin.getItemEffectManager().getItemEffects(stack, trigger);
            if (list.isEmpty()) continue;
            for (var eff : list) {
                plugin.getItemEffectProcessor().processEffect(eff, triggerEntity, target, holder.getLocation());
            }
        }
    }

    private void applyForHotbar(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null) continue;
            var list = plugin.getItemEffectManager().getItemEffects(stack, TriggerType.HOTBAR);
            if (list.isEmpty()) continue;
            for (var eff : list) {
                plugin.getItemEffectProcessor().processEffect(eff, player, null, player.getLocation());
            }
        }
    }

    private void applyForInventory(Player player) {
        var contents = player.getInventory().getStorageContents();
        if (contents == null) return;
        for (ItemStack stack : contents) {
            if (stack == null) continue;
            var list = plugin.getItemEffectManager().getItemEffects(stack, TriggerType.INVENTORY);
            if (list.isEmpty()) continue;
            for (var eff : list) {
                plugin.getItemEffectProcessor().processEffect(eff, player, null, player.getLocation());
            }
        }
    }

    private void applyForArmor(LivingEntity holder, TriggerType trigger, LivingEntity triggerEntity, LivingEntity targetEntity) {
        for (ItemStack stack : getArmorItems(holder)) {
            var list = plugin.getItemEffectManager().getItemEffects(stack, trigger);
            for (var eff : list) {
                // Important: pass targetEntity so ATTACKER/VICTIM resolution works for ON_HIT/ON_HIT_TAKEN
                plugin.getItemEffectProcessor().processEffect(eff, triggerEntity, targetEntity, holder.getLocation());
            }
        }
    }

    private List<ItemStack> getArmorItems(LivingEntity le) {
        var out = new ArrayList<ItemStack>(4);
        if (le.getEquipment() == null) return out;
        if (le.getEquipment().getHelmet() != null) out.add(le.getEquipment().getHelmet());
        if (le.getEquipment().getChestplate() != null) out.add(le.getEquipment().getChestplate());
        if (le.getEquipment().getLeggings() != null) out.add(le.getEquipment().getLeggings());
        if (le.getEquipment().getBoots() != null) out.add(le.getEquipment().getBoots());
        return out;
    }

    private List<ItemStack> getHeldItems(LivingEntity le) {
        List<ItemStack> out = new ArrayList<>();
        if (le.getEquipment() == null) return out;
        ItemStack main = le.getEquipment().getItemInMainHand();
        ItemStack off = le.getEquipment().getItemInOffHand();
        if (main != null) out.add(main);
        if (off != null) out.add(off);
        return out;
    }

    private LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity le) return le;
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof LivingEntity le) return le;
        }
        return null;
    }

    private boolean hasPersistentPotionImmunity(LivingEntity target, PotionEffectType type, int levelOneBased) {
        if (plugin.getItemEffectManager() == null || target == null || type == null) return false;

        for (ItemEffect immunity : collectPersistentImmunities(target)) {
            int maxImmune = immunity.getImmuneMaxLevelFor(type);
            if (maxImmune >= 1 && levelOneBased <= maxImmune) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPersistentFireImmunity(LivingEntity target) {
        if (plugin.getItemEffectManager() == null || target == null) return false;

        for (ItemEffect immunity : collectPersistentImmunities(target)) {
            if (immunity.isClearFire()) return true;
        }
        return false;
    }

    private List<ItemEffect> collectPersistentImmunities(LivingEntity entity) {
        List<ItemEffect> out = new ArrayList<>();
        var eq = entity.getEquipment();
        if (eq != null) {
            // Hands -> HOLDING
            addItemImmunities(out, eq.getItemInMainHand(), TriggerType.HOLDING);
            addItemImmunities(out, eq.getItemInOffHand(), TriggerType.HOLDING);

            // Armor -> EQUIPPED
            addItemImmunities(out, eq.getHelmet(), TriggerType.EQUIPPED);
            addItemImmunities(out, eq.getChestplate(), TriggerType.EQUIPPED);
            addItemImmunities(out, eq.getLeggings(), TriggerType.EQUIPPED);
            addItemImmunities(out, eq.getBoots(), TriggerType.EQUIPPED);
        }

        // NEW: Inventory-based persistent immunities
        if (entity instanceof HumanEntity human) {
            var inv = human.getInventory();

            // HOTBAR immunities
            for (int i = 0; i < 9; i++) {
                addItemImmunities(out, inv.getItem(i), TriggerType.HOTBAR);
            }

            // INVENTORY immunities (storage contents: main inventory + hotbar)
            var storage = inv.getStorageContents();
            if (storage != null) {
                for (ItemStack stack : storage) {
                    addItemImmunities(out, stack, TriggerType.INVENTORY);
                }
            }
        }

        return out;
    }

    private void addItemImmunities(List<ItemEffect> out, ItemStack item, TriggerType expectedTrigger) {
        if (item == null || plugin.getItemEffectManager() == null) return;

        for (ItemEffect e : plugin.getItemEffectManager().getItemEffects(item)) {
            if (e.getEffectType() != EffectType.IMMUNITY) continue;
            if (e.getCooldown() != 0) continue;                 // persistent only
            if (e.getTargetType() != TargetType.SELF) continue; // applies to holder only
            if (e.getTrigger() != expectedTrigger) continue;    // holding/equipped scope
            out.add(e);
        }
    }

    private boolean hasPersistentFallImmunity(LivingEntity target) {
        if (plugin.getItemEffectManager() == null || target == null) return false;
        for (ItemEffect immunity : collectPersistentImmunities(target)) {
            if (immunity.isNegateFallDamage()) return true;
        }
        return false;
    }

    private void applyForConsumedItem(Player player, ItemStack item) {
        if (item == null) return;
        var list = plugin.getItemEffectManager().getItemEffects(item, TriggerType.CONSUME);
        if (list.isEmpty()) return;
        for (ItemEffect eff : list) {
            // triggerEntity = player, no explicit target entity for CONSUME
            plugin.getItemEffectProcessor().processEffect(eff, player, null, player.getLocation());
        }
    }
}