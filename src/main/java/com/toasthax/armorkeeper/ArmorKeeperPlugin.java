package com.toasthax.armorkeeper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class ArmorKeeperPlugin extends JavaPlugin implements Listener {

    // Store per-player saved gear between death and respawn
    private final Map<UUID, SavedGear> saved = new HashMap<>();

    private static final String PERM_KEEP = "armorkeeper.keep";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Armor_Keeper enabled.");
    }

    @Override
    public void onDisable() {
        saved.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!player.hasPermission(PERM_KEEP)) {
            return; // not eligible
        }

        PlayerInventory inv = player.getInventory();

        // Capture current armor + offhand (shield or other)
        ItemStack[] armor = inv.getArmorContents();
        ItemStack offhand = inv.getItemInOffHand();

        // Keep only non-empty pieces
        SavedGear gear = new SavedGear(clean(armor), isAir(offhand) ? null : offhand.clone());
        if (gear.isEmpty()) {
            return; // nothing to keep
        }

        // Remove one instance of each kept item from the death drops
        // so they don't duplicate on the ground.
        List<ItemStack> drops = event.getDrops();
        for (ItemStack piece : gear.armor) {
            if (piece == null) continue;
            removeSingleMatching(drops, piece);
        }
        if (gear.offhand != null) {
            removeSingleMatching(drops, gear.offhand);
        }

        // Remember for respawn
        saved.put(player.getUniqueId(), gear);

        // Optional: ensure these slots appear empty to the death screen;
        // the engine clears inventory on death anyway, so no extra action required.
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        SavedGear gear = saved.remove(player.getUniqueId());
        if (gear == null || gear.isEmpty()) return;

        // Give back armor and offhand after the player fully spawns
        Bukkit.getScheduler().runTask(this, () -> {
            PlayerInventory inv = player.getInventory();

            // Restore armor in correct order: boots, leggings, chestplate, helmet (Bukkit’s order)
            ItemStack[] armor = inv.getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] == null || isAir(armor[i])) {
                    armor[i] = gear.armor[i];
                } else if (gear.armor[i] != null && !isAir(gear.armor[i])) {
                    // If slot occupied (plugins), just add to contents safely
                    inv.addItem(gear.armor[i]);
                }
            }
            inv.setArmorContents(armor);

            if (gear.offhand != null && (isAir(inv.getItemInOffHand()))) {
                inv.setItemInOffHand(gear.offhand);
            } else if (gear.offhand != null) {
                inv.addItem(gear.offhand);
            }
        });
    }

    // --- helpers ---

    private static boolean isAir(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private static ItemStack[] clean(ItemStack[] src) {
        ItemStack[] copy = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            copy[i] = isAir(src[i]) ? null : src[i].clone();
        }
        return copy;
    }

    private static void removeSingleMatching(List<ItemStack> drops, ItemStack target) {
        ListIterator<ItemStack> it = drops.listIterator();
        while (it.hasNext()) {
            ItemStack next = it.next();
            if (next != null && next.isSimilar(target) && next.getAmount() >= target.getAmount()) {
                // Remove exactly one stack equivalent to target
                it.remove();
                return;
            }
        }
    }

    private static final class SavedGear {
        final ItemStack[] armor; // boots, leggings, chestplate, helmet (Bukkit order)
        final ItemStack offhand;

        SavedGear(ItemStack[] armor, ItemStack offhand) {
            // Ensure fixed length 4
            if (armor == null || armor.length != 4) armor = new ItemStack[4];
            this.armor = armor;
            this.offhand = offhand;
        }

        boolean isEmpty() {
            boolean armorEmpty = true;
            for (ItemStack a : armor) {
                if (a != null && a.getType() != Material.AIR) {
                    armorEmpty = false; break;
                }
            }
            return armorEmpty && offhand == null;
        }
    }
}
