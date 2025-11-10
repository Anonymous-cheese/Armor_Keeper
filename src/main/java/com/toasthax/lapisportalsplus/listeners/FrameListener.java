package com.toasthax.lapisportalsplus.listeners;

import com.toasthax.lapisportalsplus.LapisPortalsPlus;
import com.toasthax.lapisportalsplus.PortalManager;
import com.toasthax.lapisportalsplus.detect.FrameDetector;
import com.toasthax.lapisportalsplus.model.Cuboid;
import com.toasthax.lapisportalsplus.model.Portal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashSet;
import java.util.Set;

public class FrameListener implements Listener {
    private final LapisPortalsPlus plugin;
    private final PortalManager portalManager;
    private final Set<Material> frameBlocks = new HashSet<>();
    private final int minW, minH, maxW, maxH;

    public FrameListener(LapisPortalsPlus plugin, PortalManager portalManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        plugin.getConfig().getStringList("frame_blocks").forEach(s -> {
            Material m = Material.matchMaterial(s.toUpperCase());
            if (m != null) frameBlocks.add(m);
        });
        var inner = plugin.getConfig().getConfigurationSection("inner_size");
        this.minW = inner.getInt("min_width", 2);
        this.minH = inner.getInt("min_height", 3);
        this.maxW = inner.getInt("max_width", 21);
        this.maxH = inner.getInt("max_height", 21);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlockPlaced();
        if (!p.hasPermission("lapisportalsplus.create")) return;
        if (!portalManager.worldAllowed(b.getWorld())) {
            p.sendMessage(plugin.getConfig().getString("messages.world_blocked"));
            return;
        }
        if (!frameBlocks.contains(b.getType())) return;

        Cuboid interior = FrameDetector.detect(b, frameBlocks, minW, minH, maxW, maxH);
        if (interior == null) return;

        Location chestLoc = findAdjacentChest(interior);
        if (chestLoc == null) {
            p.sendMessage(plugin.getConfig().getString("messages.chest_required"));
            return;
        }

        Portal portal = portalManager.createPortal(interior, chestLoc);
        var res = portalManager.relinkFromChest(portal, p);
        if (res == PortalManager.RelinkResult.KEY_IN_USE) {
            p.sendMessage(plugin.getConfig().getString("messages.key_in_use"));
        } else if (res == PortalManager.RelinkResult.WAITING) {
            p.sendMessage(plugin.getConfig().getString("messages.created"));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!portalManager.worldAllowed(e.getBlock().getWorld())) return;
        portalManager.removePortalAt(e.getBlock());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().forEach(portalManager::removePortalAt);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().forEach(portalManager::removePortalAt);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Chest chest) {
            var loc = chest.getLocation();
            portalManager.all().forEach(portal -> {
                if (portal.chestLoc() != null &&
                        portal.chestLoc().getWorld().equals(loc.getWorld()) &&
                        portal.chestLoc().getBlockX() == loc.getBlockX() &&
                        portal.chestLoc().getBlockY() == loc.getBlockY() &&
                        portal.chestLoc().getBlockZ() == loc.getBlockZ()) {
                    Bukkit.getScheduler().runTaskLater(LapisPortalsPlus.get(), () -> portalManager.relinkFromChest(portal, null), 1L);
                }
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Chest chest) {
            var loc = chest.getLocation();
            portalManager.all().forEach(portal -> {
                if (portal.chestLoc() != null &&
                        portal.chestLoc().getWorld().equals(loc.getWorld()) &&
                        portal.chestLoc().getBlockX() == loc.getBlockX() &&
                        portal.chestLoc().getBlockY() == loc.getBlockY() &&
                        portal.chestLoc().getBlockZ() == loc.getBlockZ()) {
                    Bukkit.getScheduler().runTaskLater(LapisPortalsPlus.get(), () -> portalManager.relinkFromChest(portal, null), 1L);
                }
            });
        }
    }

    private org.bukkit.Location findAdjacentChest(Cuboid interior) {
        var w = interior.getWorld();
        for (int x = interior.getMinX()-1; x <= interior.getMaxX()+1; x++) {
            for (int y = interior.getMinY()-1; y <= interior.getMaxY()+1; y++) {
                for (int z : new int[]{interior.getMinZ()-1, interior.getMaxZ()+1}) {
                    if (w.getBlockAt(x, y, z).getType() == Material.CHEST) return w.getBlockAt(x, y, z).getLocation();
                }
            }
        }
        for (int z = interior.getMinZ()-1; z <= interior.getMaxZ()+1; z++) {
            for (int y = interior.getMinY()-1; y <= interior.getMaxY()+1; y++) {
                for (int x : new int[]{interior.getMinX()-1, interior.getMaxX()+1}) {
                    if (w.getBlockAt(x, y, z).getType() == Material.CHEST) return w.getBlockAt(x, y, z).getLocation();
                }
            }
        }
        return null;
    }
}
