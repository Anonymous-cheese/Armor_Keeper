package com.toasthax.lapisportalsplus.listeners;

import com.toasthax.lapisportalsplus.LapisPortalsPlus;
import com.toasthax.lapisportalsplus.model.Portal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalListener implements Listener {
    private final LapisPortalsPlus plugin;
    private final com.toasthax.lapisportalsplus.PortalManager portalManager;
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Map<UUID, Long> lastNoTargetMsg = new HashMap<>();

    public PortalListener(LapisPortalsPlus plugin, com.toasthax.lapisportalsplus.PortalManager manager) {
        this.plugin = plugin;
        this.portalManager = manager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (!player.hasPermission("lapisportalsplus.use")) return;

        for (Portal p : portalManager.all()) {
            boolean fromInside = p.interior().contains(e.getFrom());
            if (!fromInside && p.interior().contains(e.getTo())) {
                if (p.linkedPortalId() == null) {
                    long last = lastNoTargetMsg.getOrDefault(player.getUniqueId(), 0L);
                    if (System.currentTimeMillis() - last > 5000) {
                        player.sendMessage(plugin.getConfig().getString("messages.no_target"));
                        lastNoTargetMsg.put(player.getUniqueId(), System.currentTimeMillis());
                    }
                    continue;
                }

                long lastTp = cooldown.getOrDefault(player.getUniqueId(), 0L);
                if (System.currentTimeMillis() - lastTp < plugin.getConfig().getInt("teleport_cooldown_seconds", 2) * 1000L)
                    continue;

                var target = portalManager.byId(p.linkedPortalId()).orElse(null);
                if (target == null) continue;

                cooldown.put(player.getUniqueId(), System.currentTimeMillis());
                player.teleport(target.interior().centerFloor());
            }
        }
    }
}
