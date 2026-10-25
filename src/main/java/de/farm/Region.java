package de.farm;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

/** Bereich einer natürlichen Spawn-Plattform: nur die Ziel-Mob-Art darf dort spawnen, Drops gehen in die Kiste. */
record Region(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, EntityType type,
              int cx, int cy, int cz, boolean hasOther, int ox, int oy, int oz) {

    boolean contains(Location l) {
        if (l.getWorld() == null || !l.getWorld().getName().equals(world)) return false;
        int x = l.getBlockX(), y = l.getBlockY(), z = l.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    String serialize() {
        return String.join(";", world, "" + minX, "" + minY, "" + minZ, "" + maxX, "" + maxY, "" + maxZ,
            type.name(), "" + cx, "" + cy, "" + cz, "" + hasOther, "" + ox, "" + oy, "" + oz);
    }

    static Region parse(String s) {
        String[] p = s.split(";");
        return new Region(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]),
            Integer.parseInt(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]), EntityType.valueOf(p[7]),
            Integer.parseInt(p[8]), Integer.parseInt(p[9]), Integer.parseInt(p[10]), Boolean.parseBoolean(p[11]),
            Integer.parseInt(p[12]), Integer.parseInt(p[13]), Integer.parseInt(p[14]));
    }
}
