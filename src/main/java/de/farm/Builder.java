package de.farm;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;

/** Hilfsklasse zum Platzieren von Bloecken relativ zur Mitte der Farm (y=0 ist die Boden-Ebene). */
final class Builder {
    final World w;
    final int ox, oy, oz;
    final int n;
    final int h;

    Builder(Location origin, int n) {
        this.w = origin.getWorld();
        this.ox = origin.getBlockX();
        this.oy = origin.getBlockY();
        this.oz = origin.getBlockZ();
        this.n = n;
        this.h = n / 2;
    }

    Block at(int x, int y, int z) {
        return w.getBlockAt(ox + x, oy + y, oz + z);
    }

    void set(int x, int y, int z, Material m) {
        at(x, y, z).setType(m, false);
    }

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, m);
    }

    void floor(Material m) {
        fill(-h, 0, -h, h, 0, h, m);
    }

    /** Pflanze setzen und voll ausgewachsen. */
    void crop(int x, int y, int z, Material m) {
        Block b = at(x, y, z);
        b.setType(m, false);
        BlockData d = b.getBlockData();
        if (d instanceof Ageable a) {
            a.setAge(a.getMaximumAge());
            b.setBlockData(a, false);
        }
    }

    /** Waende am Rand von y1 bis y2. */
    void ring(int y1, int y2, Material m) {
        for (int x = -h; x <= h; x++) {
            for (int z = -h; z <= h; z++) {
                if (Math.abs(x) == h || Math.abs(z) == h) {
                    for (int y = y1; y <= y2; y++) set(x, y, z, m);
                }
            }
        }
    }

    void spawn(EntityType t, int x, int z) {
        w.spawnEntity(new Location(w, ox + x + 0.5, oy + 1, oz + z + 0.5), t);
    }
}
