package com.toasthax.lapisportalsplus;

import com.toasthax.lapisportalsplus.model.Cuboid;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Random;

public class ParticleUtil {
    private static final Random RNG = new Random();

    public static void drawInteriorOutline(Cuboid c, Particle particle, double density) {
        World w = c.getWorld();
        boolean xConst = c.getMinX() == c.getMaxX();
        if (xConst) {
            int x = c.getMinX();
            for (int z = c.getMinZ(); z <= c.getMaxZ(); z++) {
                for (int y = c.getMinY(); y <= c.getMaxY(); y++) {
                    if (onBorderZ(y, z, c) && RNG.nextDouble() <= density) {
                        w.spawnParticle(particle, x + 0.5, y + 0.5, z + 0.5, 1, 0,0,0,0);
                    }
                }
            }
        } else {
            int z = c.getMinZ();
            for (int x = c.getMinX(); x <= c.getMaxX(); x++) {
                for (int y = c.getMinY(); y <= c.getMaxY(); y++) {
                    if (onBorderX(y, x, c) && RNG.nextDouble() <= density) {
                        w.spawnParticle(particle, x + 0.5, y + 0.5, z + 0.5, 1, 0,0,0,0);
                    }
                }
            }
        }
    }

    private static boolean onBorderZ(int y, int z, Cuboid c) {
        return y == c.getMinY() || y == c.getMaxY() || z == c.getMinZ() || z == c.getMaxZ();
    }
    private static boolean onBorderX(int y, int x, Cuboid c) {
        return y == c.getMinY() || y == c.getMaxY() || x == c.getMinX() || x == c.getMaxX();
    }

    public static void spawnInterior(Cuboid c, Particle particle, int count) {
        World w = c.getWorld();
        for (int i = 0; i < count; i++) {
            double x = c.getMinX() + 0.5 + RNG.nextDouble() * (c.getMaxX() - c.getMinX());
            double y = c.getMinY() + 0.5 + RNG.nextDouble() * (c.getMaxY() - c.getMinY());
            double z = c.getMinZ() + 0.5 + RNG.nextDouble() * (c.getMaxZ() - c.getMinZ());
            w.spawnParticle(particle, x, y, z, 1, 0,0,0,0.01);
        }
    }
}
