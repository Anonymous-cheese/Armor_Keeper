package com.toasthax.lapisportalsplus;

import com.toasthax.lapisportalsplus.commands.LppCommand;
import com.toasthax.lapisportalsplus.listeners.FrameListener;
import com.toasthax.lapisportalsplus.listeners.PortalListener;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class LapisPortalsPlus extends JavaPlugin {
    private static LapisPortalsPlus instance;
    private PortalManager portalManager;
    private boolean debug = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.portalManager = new PortalManager(this);
        portalManager.loadFromConfig();

        Bukkit.getPluginManager().registerEvents(new FrameListener(this, portalManager), this);
        Bukkit.getPluginManager().registerEvents(new PortalListener(this, portalManager), this);
        getCommand("lpp").setExecutor(new LppCommand(this));

        startOutlineTask();

        getLogger().info("LapisPortalsPlus 1.3.0 enabled.");
    }

    @Override
    public void onDisable() {
        portalManager.saveToConfig();
    }

    private void startOutlineTask() {
        var sec = getConfig().getConfigurationSection("linked_outline");
        boolean enabled = sec != null && sec.getBoolean("enabled", true);
        if (!enabled) return;
        int interval = sec.getInt("interval_ticks", 15);
        double density = sec.getDouble("density", 0.5);

        new BukkitRunnable(){
            @Override public void run() {
                portalManager.all().forEach(p -> {
                    if (p.linkedPortalId() != null) {
                        ParticleUtil.drawInteriorOutline(p.interior(),
                                org.bukkit.Particle.valueOf(getConfig().getString("edge_particle_type", "PORTAL").toUpperCase()),
                                density);
                        var interiorSec = getConfig().getConfigurationSection("interior_particles");
                        if (interiorSec != null && interiorSec.getBoolean("enabled", false)) {
                            Particle interiorType = Particle.valueOf(getConfig().getString("interior_particles.type", "ENCHANTMENT_TABLE").toUpperCase());
                            int count = getConfig().getInt("interior_particles.count", 8);
                            ParticleUtil.spawnInterior(p.interior(), interiorType, count);
                        }
                    }
                });
            }
        }.runTaskTimer(this, interval, interval);
    }

    public static LapisPortalsPlus get() { return instance; }
    public PortalManager portalManager() { return portalManager; }
    public boolean isDebug() { return debug; }
    public void setDebug(boolean v) { debug = v; }
}
