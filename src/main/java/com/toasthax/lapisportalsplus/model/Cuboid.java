package com.toasthax.lapisportalsplus.model;

import org.bukkit.Location;
import org.bukkit.World;

public class Cuboid {
    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public Cuboid(Location a, Location b) {
        if (!a.getWorld().equals(b.getWorld())) throw new IllegalArgumentException("Different worlds");
        this.world = a.getWorld();
        this.minX = Math.min(a.getBlockX(), b.getBlockX());
        this.minY = Math.min(a.getBlockY(), b.getBlockY());
        this.minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        this.maxX = Math.max(a.getBlockX(), b.getBlockX());
        this.maxY = Math.max(a.getBlockY(), b.getBlockY());
        this.maxZ = Math.max(a.getBlockZ(), b.getBlockZ());
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().equals(world)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public World getWorld() { return world; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public Location centerFloor() {
        double cx = (minX + maxX) / 2.0 + 0.5;
        double cy = minY + 1.0;
        double cz = (minZ + maxZ) / 2.0 + 0.5;
        return new Location(world, cx, cy, cz);
    }
}
