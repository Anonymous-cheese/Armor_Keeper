package com.toasthax.lapisportalsplus.model;

import org.bukkit.Location;
import org.bukkit.Material;

public class Portal {
    private final String id;
    private final Cuboid interior;
    private final Location chestLoc;
    private Material keyMaterial;
    private String linkedPortalId;

    public Portal(String id, Cuboid interior, Location chestLoc) {
        this.id = id;
        this.interior = interior;
        this.chestLoc = chestLoc;
    }

    public String id() { return id; }
    public Cuboid interior() { return interior; }
    public Location chestLoc() { return chestLoc; }
    public Material keyMaterial() { return keyMaterial; }
    public void setKeyMaterial(Material m) { this.keyMaterial = m; }
    public String linkedPortalId() { return linkedPortalId; }
    public void setLinkedPortalId(String id) { this.linkedPortalId = id; }
}
