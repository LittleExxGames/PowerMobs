package com.powermobs.mobs.equipment.items;

public enum TriggerType {
    HOLDING,
    HOTBAR,
    INVENTORY,
    EQUIPPED,
    CONSUME,
    ON_HIT,        // EntityDamageByEntityEvent (attacker has the item)
    ON_HIT_TAKEN,  // victim has the worn item
    ON_KILL,       // EntityDeathEvent with killer
    RIGHT_CLICK,   // PlayerInteractEvent (use item)
    SHOOT_PROJECTILE,
    PROJECTILE_HIT,
    BLOCK_BREAK
}
