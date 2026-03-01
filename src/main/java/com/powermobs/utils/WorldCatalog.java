package com.powermobs.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class WorldCatalog {

    private WorldCatalog() {
    }

    /**
     * @return map of known world names -> active (loaded) state
     * Known worlds include:
     * - Bukkit loaded worlds (active=true)
     * - Multiverse worlds (active based on Bukkit.getWorld(name) != null)
     * - Vanilla folder guesses for nether/end if present (active based on Bukkit.getWorld(name) != null)
     */
    public static Map<String, Boolean> getKnownWorldActiveMap() {
        LinkedHashMap<String, Boolean> out = new LinkedHashMap<>();

        for (World w : Bukkit.getWorlds()) {
            out.put(w.getName(), true);
        }

        addMultiverseWorlds(out);

        addVanillaDimensionFolders(out);

        return out;
    }

    /**
     * Resolve world names that match the given environments, using:
     * - Bukkit loaded worlds
     * - Multiverse worlds (even inactive/unloaded)
     * - Vanilla folder guesses (nether/end) if present
     */
    public static Set<String> resolveWorldNamesForEnvironments(EnumSet<World.Environment> envs) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (envs == null || envs.isEmpty()) return out;

        for (World w : Bukkit.getWorlds()) {
            if (envs.contains(w.getEnvironment())) {
                out.add(w.getName());
            }
        }

        addMultiverseWorldsByEnvironment(out, envs);

        addVanillaDimensionFoldersByEnvironment(out, envs);

        return out;
    }

    private static void addMultiverseWorlds(Map<String, Boolean> out) {
        Plugin mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (mv == null || !mv.isEnabled()) return;

        try {
            Object mvWorldManager = mv.getClass().getMethod("getMVWorldManager").invoke(mv);
            Object mvWorldsObj = mvWorldManager.getClass().getMethod("getMVWorlds").invoke(mvWorldManager);

            if (!(mvWorldsObj instanceof Iterable<?> mvWorlds)) return;

            for (Object mvWorld : mvWorlds) {
                String name = String.valueOf(mvWorld.getClass().getMethod("getName").invoke(mvWorld));
                boolean active = Bukkit.getWorld(name) != null;
                out.putIfAbsent(name, active);
            }
        } catch (ReflectiveOperationException ignored) {
            // ignore (API mismatch); Bukkit worlds still work
        }
    }

    private static void addMultiverseWorldsByEnvironment(Set<String> out, EnumSet<World.Environment> envs) {
        Plugin mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (mv == null || !mv.isEnabled()) return;

        try {
            Object mvWorldManager = mv.getClass().getMethod("getMVWorldManager").invoke(mv);
            Object mvWorldsObj = mvWorldManager.getClass().getMethod("getMVWorlds").invoke(mvWorldManager);

            if (!(mvWorldsObj instanceof Iterable<?> mvWorlds)) return;

            for (Object mvWorld : mvWorlds) {
                Object envObj = mvWorld.getClass().getMethod("getEnvironment").invoke(mvWorld);
                if (!(envObj instanceof World.Environment env)) continue;
                if (!envs.contains(env)) continue;

                String name = String.valueOf(mvWorld.getClass().getMethod("getName").invoke(mvWorld));
                out.add(name);
            }
        } catch (ReflectiveOperationException ignored) {
            // ignore (API mismatch); Bukkit worlds still work
        }
    }

    private static void addVanillaDimensionFolders(Map<String, Boolean> out) {
        String base = getPrimaryOverworldName();
        if (base == null) return;

        File container = Bukkit.getWorldContainer();
        if (container == null) return;

        File nether = new File(container, base + "_nether");
        if (nether.exists() && nether.isDirectory()) {
            String name = base + "_nether";
            out.putIfAbsent(name, Bukkit.getWorld(name) != null);
        }

        File end = new File(container, base + "_the_end");
        if (end.exists() && end.isDirectory()) {
            String name = base + "_the_end";
            out.putIfAbsent(name, Bukkit.getWorld(name) != null);
        }
    }

    private static void addVanillaDimensionFoldersByEnvironment(Set<String> out, EnumSet<World.Environment> envs) {
        String base = getPrimaryOverworldName();
        if (base == null) return;

        File container = Bukkit.getWorldContainer();
        if (container == null) return;

        if (envs.contains(World.Environment.NETHER)) {
            File nether = new File(container, base + "_nether");
            if (nether.exists() && nether.isDirectory()) {
                out.add(base + "_nether");
            }
        }

        if (envs.contains(World.Environment.THE_END)) {
            File end = new File(container, base + "_the_end");
            if (end.exists() && end.isDirectory()) {
                out.add(base + "_the_end");
            }
        }
    }

    private static String getPrimaryOverworldName() {
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NORMAL) {
                return w.getName();
            }
        }
        // fallback: first loaded world
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getName();
    }
}
