package com.toasthax.lapisportalsplus;

import com.toasthax.lapisportalsplus.model.Cuboid;
import com.toasthax.lapisportalsplus.model.Portal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class PortalManager {
    private final LapisPortalsPlus plugin;
    private final Map<String, Portal> portals = new HashMap<>();
    private final Map<Material, List<String>> linksByKey = new HashMap<>();

    public enum RelinkResult { LINKED, WAITING, KEY_IN_USE, NO_KEYS, CROSS_WORLD_BLOCKED }

    public PortalManager(LapisPortalsPlus plugin) { this.plugin = plugin; }

    public Collection<Portal> all() { return portals.values(); }
    public Optional<Portal> byId(String id) { return Optional.ofNullable(portals.get(id)); }

    public boolean worldAllowed(World world) {
        var allow = plugin.getConfig().getStringList("worlds.allow");
        var deny = plugin.getConfig().getStringList("worlds.deny");
        if (!allow.isEmpty() && !allow.contains(world.getName())) return false;
        return !deny.contains(world.getName());
    }

    public void saveToConfig() {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("portals", null);
        for (Portal p : portals.values()) {
            String base = "portals." + p.id() + ".";
            cfg.set(base + "world", p.interior().getWorld().getName());
            cfg.set(base + "interior", List.of(p.interior().getMinX(), p.interior().getMinY(), p.interior().getMinZ(),
                                               p.interior().getMaxX(), p.interior().getMaxY(), p.interior().getMaxZ()));
            if (p.chestLoc() != null) {
                cfg.set(base + "chest", List.of(p.chestLoc().getBlockX(), p.chestLoc().getBlockY(), p.chestLoc().getBlockZ()));
            }
            cfg.set(base + "key", p.keyMaterial() == null ? null : p.keyMaterial().name());
            cfg.set(base + "link", p.linkedPortalId());
        }
        plugin.saveConfig();
    }

    public void loadFromConfig() {
        portals.clear();
        linksByKey.clear();
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("portals");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            String base = "portals." + id + ".";
            String worldName = cfg.getString(base + "world");
            World w = Bukkit.getWorld(worldName);
            if (w == null) continue;
            List<Integer> interior = cfg.getIntegerList(base + "interior");
            if (interior.size() != 6) continue;
            Cuboid c = new Cuboid(new Location(w, interior.get(0), interior.get(1), interior.get(2)),
                                  new Location(w, interior.get(3), interior.get(4), interior.get(5)));
            List<Integer> chest = cfg.getIntegerList(base + "chest");
            Location chestLoc = null;
            if (chest.size() == 3) chestLoc = new Location(w, chest.get(0), chest.get(1), chest.get(2));
            Portal p = new Portal(id, c, chestLoc);
            String key = cfg.getString(base + "key");
            if (key != null) p.setKeyMaterial(Material.matchMaterial(key));
            String link = cfg.getString(base + "link");
            if (link != null) p.setLinkedPortalId(link);
            portals.put(id, p);
            if (p.keyMaterial() != null) linksByKey.computeIfAbsent(p.keyMaterial(), k -> new ArrayList<>()).add(p.id());
        }
    }

    public Portal createPortal(Cuboid interior, Location chestLoc) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Portal p = new Portal(id, interior, chestLoc);
        portals.put(id, p);
        saveToConfig();
        return p;
    }

    public void removePortalAt(Block block) {
        List<String> toRemove = new ArrayList<>();
        for (Portal p : portals.values()) {
            if (p.interior().getWorld().equals(block.getWorld())) {
                if (block.getX() >= p.interior().getMinX()-1 && block.getX() <= p.interior().getMaxX()+1 &&
                    block.getY() >= p.interior().getMinY()-1 && block.getY() <= p.interior().getMaxY()+1 &&
                    block.getZ() >= p.interior().getMinZ()-1 && block.getZ() <= p.interior().getMaxZ()+1) {
                    toRemove.add(p.id());
                }
            }
        }
        if (toRemove.isEmpty()) return;
        for (String id : toRemove) {
            Portal p = portals.remove(id);
            if (p != null && p.keyMaterial() != null) {
                var list = linksByKey.getOrDefault(p.keyMaterial(), new ArrayList<>());
                list.remove(p.id());
                if (p.linkedPortalId() != null) {
                    byId(p.linkedPortalId()).ifPresent(other -> other.setLinkedPortalId(null));
                }
            }
        }
        saveToConfig();
    }

    public RelinkResult relinkFromChest(Portal p, Player notify) {
        if (p.chestLoc() == null) return RelinkResult.NO_KEYS;
        var state = p.chestLoc().getBlock().getState();
        if (!(state instanceof Chest chest)) return RelinkResult.NO_KEYS;

        Set<Material> keys = new HashSet<>();
        Arrays.stream(chest.getBlockInventory().getContents())
                .filter(item -> item != null && item.getType() != Material.AIR)
                .forEach(item -> keys.add(item.getType()));

        if (keys.isEmpty()) {
            if (notify != null && LapisPortalsPlus.get().isDebug()) notify.sendMessage("§7[LPP] No items in chest -> no keys.");
            unlink(p);
            saveToConfig();
            return RelinkResult.NO_KEYS;
        }

        // unlink former
        if (p.keyMaterial() != null) {
            var list = linksByKey.getOrDefault(p.keyMaterial(), new ArrayList<>());
            list.remove(p.id());
            if (p.linkedPortalId() != null) {
                byId(p.linkedPortalId()).ifPresent(other -> other.setLinkedPortalId(null));
                p.setLinkedPortalId(null);
            }
            p.setKeyMaterial(null);
        }

        RelinkResult result = RelinkResult.KEY_IN_USE;
        for (Material key : keys) {
            var list = linksByKey.computeIfAbsent(key, k -> new ArrayList<>());
            if (list.size() == 0) {
                list.add(p.id());
                p.setKeyMaterial(key);
                if (notify != null && LapisPortalsPlus.get().isDebug()) notify.sendMessage("§7[LPP] Key " + key + " waiting for a partner.");
                result = RelinkResult.WAITING;
                break;
            } else if (list.size() == 1) {
                String otherId = list.get(0);
                var other = byId(otherId).orElse(null);
                if (other != null && other.interior().getWorld().equals(p.interior().getWorld())) {
                    list.add(p.id());
                    p.setKeyMaterial(key);
                    p.setLinkedPortalId(otherId);
                    other.setLinkedPortalId(p.id());
                    if (notify != null) notify.sendMessage(LapisPortalsPlus.get().getConfig().getString("messages.linked")
                            .replace("{target}", otherId).replace("{key}", key.name()));
                    saveToConfig();
                    return RelinkResult.LINKED;
                } else {
                    if (notify != null && LapisPortalsPlus.get().isDebug()) notify.sendMessage("§7[LPP] Cross-world blocked for key " + key);
                    // try another key if any
                }
            } else {
                // >=2 already using this key
                if (notify != null) notify.sendMessage(LapisPortalsPlus.get().getConfig().getString("messages.key_in_use"));
                result = RelinkResult.KEY_IN_USE;
                // but keep trying other keys
            }
        }
        saveToConfig();
        return result;
    }

    private void unlink(Portal p) {
        if (p.keyMaterial() != null) {
            var list = linksByKey.getOrDefault(p.keyMaterial(), new ArrayList<>());
            list.remove(p.id());
        }
        if (p.linkedPortalId() != null) {
            byId(p.linkedPortalId()).ifPresent(other -> other.setLinkedPortalId(null));
        }
        p.setLinkedPortalId(null);
        p.setKeyMaterial(null);
    }
}
