package com.toasthax.lapisportalsplus.detect;

import com.toasthax.lapisportalsplus.model.Cuboid;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Set;

public class FrameDetector {

    public static Cuboid detect(Block origin, Set<Material> frameBlocks,
                                int minW, int minH, int maxW, int maxH) {
        Cuboid c = detectInPlane(origin, frameBlocks, minW, minH, maxW, maxH, true);
        if (c != null) return c;
        return detectInPlane(origin, frameBlocks, minW, minH, maxW, maxH, false);
    }

    private static Cuboid detectInPlane(Block origin, Set<Material> frames,
                                        int minW, int minH, int maxW, int maxH, boolean zOriented) {
        final int ox = origin.getX();
        final int oy = origin.getY();
        final int oz = origin.getZ();
        final World w = origin.getWorld();

        for (int iw = minW; iw <= maxW; iw++) {
            for (int ih = minH; ih <= maxH; ih++) {
                final int ow = iw + 2;
                final int oh = ih + 2;

                for (int dx = -ow + 1; dx <= 0; dx++) {
                    for (int dy = -oh + 1; dy <= 0; dy++) {

                        int minX, minY, minZ, maxX, maxY, maxZ;
                        if (zOriented) {
                            minX = ox + dx; maxX = minX + ow - 1;
                            minY = oy + dy; maxY = minY + oh - 1;
                            minZ = oz;      maxZ = oz;

                            if (!onBorder(ox, oy, oz, minX, minY, minZ, maxX, maxY, maxZ, zOriented)) continue;

                            boolean ok = true;
                            for (int x = minX; x <= maxX && ok; x++) {
                                for (int y = minY; y <= maxY && ok; y++) {
                                    boolean border = (x == minX || x == maxX || y == minY || y == maxY);
                                    Material type = w.getBlockAt(x, y, minZ).getType();
                                    if (border) { if (!frames.contains(type)) ok = false; }
                                    else { if (!isAirish(type)) ok = false; }
                                }
                            }
                            if (!ok) continue;
                            return new Cuboid(
                                    new org.bukkit.Location(w, minX + 1, minY + 1, minZ),
                                    new org.bukkit.Location(w, maxX - 1, maxY - 1, maxZ)
                            );
                        } else {
                            minZ = oz + dx; maxZ = minZ + ow - 1;
                            minY = oy + dy; maxY = minY + oh - 1;
                            minX = ox;      maxX = ox;

                            if (!onBorder(ox, oy, oz, minX, minY, minZ, maxX, maxY, maxZ, zOriented)) continue;

                            boolean ok = true;
                            for (int z = minZ; z <= maxZ && ok; z++) {
                                for (int y = minY; y <= maxY && ok; y++) {
                                    boolean border = (z == minZ || z == maxZ || y == minY || y == maxY);
                                    Material type = w.getBlockAt(minX, y, z).getType();
                                    if (border) { if (!frames.contains(type)) ok = false; }
                                    else { if (!isAirish(type)) ok = false; }
                                }
                            }
                            if (!ok) continue;
                            return new Cuboid(
                                    new org.bukkit.Location(w, minX, minY + 1, minZ + 1),
                                    new org.bukkit.Location(w, maxX, maxY - 1, maxZ - 1)
                            );
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isAirish(Material m) {
        return m.isAir() || m == Material.CAVE_AIR || m == Material.VOID_AIR || m == Material.FIRE || m == Material.SOUL_FIRE;
    }

    private static boolean onBorder(int x, int y, int z,
                                    int minX, int minY, int minZ,
                                    int maxX, int maxY, int maxZ,
                                    boolean zOriented) {
        if (zOriented) {
            if (z != minZ) return false;
            boolean onX = (x == minX || x == maxX) && (y >= minY && y <= maxY);
            boolean onY = (y == minY || y == maxY) && (x >= minX && x <= maxX);
            return onX || onY;
        } else {
            if (x != minX) return false;
            boolean onZ = (z == minZ || z == maxZ) && (y >= minY && y <= maxY);
            boolean onY = (y == minY || y == maxY) && (z >= minZ && z <= maxZ);
            return onZ || onY;
        }
    }
}
